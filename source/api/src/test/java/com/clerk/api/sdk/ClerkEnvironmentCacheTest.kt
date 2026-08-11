package com.clerk.api.sdk

import android.content.Context
import com.clerk.api.Clerk
import com.clerk.api.ClerkConfigurationOptions
import com.clerk.api.configuration.connectivity.NetworkConnectivityMonitor
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.environment.AuthConfig
import com.clerk.api.network.model.environment.DisplayConfig
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.model.environment.UserSettings
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.storage.StorageHelper
import com.clerk.api.storage.StorageKey
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Covers the offline-environment-cache fix: [Clerk.updateEnvironment] persists the environment to
 * encrypted storage, and [com.clerk.api.configuration.ConfigurationManager] rehydrates it on cold
 * start before the network refresh lands, so [AuthView]-style UI has a representation to render
 * while offline instead of an empty form.
 */
@RunWith(RobolectricTestRunner::class)
class ClerkEnvironmentCacheTest {
  private lateinit var context: Context

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    StorageHelper.initialize(context)
    StorageHelper.reset(context)
    Clerk.reset()
    mockkObject(Client.Companion)
    mockkObject(Environment.Companion)
    // mockkObject() intercepts every member of the companion, including the
    // compiler-synthesized Environment.serializer() used by Clerk.updateEnvironment()'s
    // caching and by this test's own encode/decode setup - without this, those calls throw
    // "no answer found for ...serializer()" since only get() is stubbed per-test below.
    every { Environment.serializer() } answers { callOriginal() }
  }

  @After
  fun tearDown() {
    Clerk.reset()
    unmockkAll()
    StorageHelper.reset(context)
    NetworkConnectivityMonitor.resetForTesting()
  }

  @Test
  fun `updateEnvironment persists the environment to encrypted storage`() {
    val environment = testEnvironment("Persisted App")

    Clerk.updateEnvironment(environment)

    val cachedJson = StorageHelper.loadValue(StorageKey.CACHED_ENVIRONMENT)
    val decoded = cachedJson?.let { ClerkApi.json.decodeFromString(Environment.serializer(), it) }
    assertEquals(environment, decoded)
  }

  @Test
  fun `reset clears the cached environment`() {
    Clerk.updateEnvironment(testEnvironment("To Clear"))

    Clerk.reset()

    assertNull(StorageHelper.loadValue(StorageKey.CACHED_ENVIRONMENT))
  }

  @Test
  fun `initialize hydrates environment from cache before the network refresh completes`() =
    runBlocking {
      val cachedEnvironment = testEnvironment("Cached App")
      StorageHelper.saveValue(
        StorageKey.CACHED_ENVIRONMENT,
        ClerkApi.json.encodeToString(Environment.serializer(), cachedEnvironment),
      )
      val neverCompletingClient = CompletableDeferred<ClerkResult<Client, Nothing>>()
      val neverCompletingEnvironment = CompletableDeferred<ClerkResult<Environment, Nothing>>()
      coEvery { Client.get() } coAnswers { neverCompletingClient.await() }
      coEvery { Client.getSkippingClientId() } coAnswers { neverCompletingClient.await() }
      coEvery { Environment.get() } coAnswers { neverCompletingEnvironment.await() }

      Clerk.initialize(
        context = context,
        publishableKey = PUBLISHABLE_KEY,
        options = ClerkConfigurationOptions(proxyUrl = PROXY_URL),
      )

      // Hydration from cache happens synchronously as part of initialize(), independently of the
      // network refresh above (which is stalled forever), so the cached values should already be
      // visible even though isInitialized never flips to true.
      assertEquals("Cached App", Clerk.applicationName)
    }

  @Test
  fun `corrupted cached environment is ignored and does not crash initialization`() = runBlocking {
    StorageHelper.saveValue(StorageKey.CACHED_ENVIRONMENT, "not-valid-environment-json")
    // The client/environment fetches are held open with a CompletableDeferred (rather than
    // resolving immediately via `returns`) so the background refresh coroutine - which runs on
    // Dispatchers.IO independently of this runBlocking scope - cannot race ahead and populate
    // Clerk.environment before the assertNull below observes the post-hydration state.
    val clientDeferred = CompletableDeferred<ClerkResult<Client, Nothing>>()
    val environmentDeferred = CompletableDeferred<ClerkResult<Environment, Nothing>>()
    coEvery { Client.get() } coAnswers { clientDeferred.await() }
    coEvery { Client.getSkippingClientId() } coAnswers { clientDeferred.await() }
    coEvery { Environment.get() } coAnswers { environmentDeferred.await() }

    Clerk.initialize(
      context = context,
      publishableKey = PUBLISHABLE_KEY,
      options = ClerkConfigurationOptions(proxyUrl = PROXY_URL),
    )

    // The corrupted cache entry should be treated as a cache miss, not crash initialization.
    assertNull(Clerk.environment)

    clientDeferred.complete(ClerkResult.success(Client()))
    environmentDeferred.complete(ClerkResult.success(testEnvironment("Fresh App")))
    withTimeout(5_000) { Clerk.isInitialized.first { it } }

    assertEquals("Fresh App", Clerk.applicationName)
  }

  @Test
  fun `cached environment does not clobber an environment a fresh fetch already produced`() =
    runBlocking {
      StorageHelper.saveValue(
        StorageKey.CACHED_ENVIRONMENT,
        ClerkApi.json.encodeToString(Environment.serializer(), testEnvironment("Stale Cached App")),
      )
      // Held open for the same reason as above: this also lets us assert on the hydrated stale
      // value before releasing the fresh fetch, actually exercising the "does not clobber"
      // ordering instead of only checking the final state once both writes have happened.
      val clientDeferred = CompletableDeferred<ClerkResult<Client, Nothing>>()
      val environmentDeferred = CompletableDeferred<ClerkResult<Environment, Nothing>>()
      coEvery { Client.get() } coAnswers { clientDeferred.await() }
      coEvery { Client.getSkippingClientId() } coAnswers { clientDeferred.await() }
      coEvery { Environment.get() } coAnswers { environmentDeferred.await() }

      Clerk.initialize(
        context = context,
        publishableKey = PUBLISHABLE_KEY,
        options = ClerkConfigurationOptions(proxyUrl = PROXY_URL),
      )
      assertEquals("Stale Cached App", Clerk.applicationName)

      clientDeferred.complete(ClerkResult.success(Client()))
      environmentDeferred.complete(ClerkResult.success(testEnvironment("Fresh App")))
      withTimeout(5_000) { Clerk.isInitialized.first { it } }

      assertEquals("Fresh App", Clerk.applicationName)
    }

  private fun testEnvironment(applicationName: String): Environment {
    return Environment(
      authConfig = AuthConfig(singleSessionMode = false),
      displayConfig =
        DisplayConfig(
          applicationName = applicationName,
          branded = true,
          logoImageUrl = "https://example.com/logo.png",
          homeUrl = "/",
          privacyPolicyUrl = null,
          termsUrl = null,
          googleOneTapClientId = null,
        ),
      userSettings =
        UserSettings(
          attributes = emptyMap(),
          signUp =
            UserSettings.SignUpUserSettings(
              customActionRequired = false,
              progressive = false,
              mode = "public",
              legalConsentEnabled = false,
            ),
          social = emptyMap(),
          actions = UserSettings.Actions(),
          passkeySettings = null,
        ),
    )
  }

  private companion object {
    const val PUBLISHABLE_KEY = "pk_test_cachedenv"
    // A proxyUrl is supplied to every Clerk.initialize() call below so that
    // ConfigurationManager.configureSdkState() short-circuits before
    // PublishableKeyHelper.extractApiUrl(), which otherwise base64-decodes the
    // key's suffix via android.util.Base64.decode() and throws
    // IllegalArgumentException for a non-base64, non-padded value like
    // "cachedenv".
    const val PROXY_URL = "https://proxy.example.com/__clerk"
  }
}
