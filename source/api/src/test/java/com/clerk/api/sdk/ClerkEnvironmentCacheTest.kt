package com.clerk.api.sdk

import android.content.Context
import com.clerk.api.Clerk
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

      Clerk.initialize(context = context, publishableKey = PUBLISHABLE_KEY)

      // Hydration from cache happens synchronously as part of initialize(), independently of the
      // network refresh above (which is stalled forever), so the cached values should already be
      // visible even though isInitialized never flips to true.
      assertEquals("Cached App", Clerk.applicationName)
    }

  @Test
  fun `corrupted cached environment is ignored and does not crash initialization`() = runBlocking {
    StorageHelper.saveValue(StorageKey.CACHED_ENVIRONMENT, "not-valid-environment-json")
    coEvery { Client.get() } returns ClerkResult.success(Client())
    coEvery { Client.getSkippingClientId() } returns ClerkResult.success(Client())
    coEvery { Environment.get() } returns ClerkResult.success(testEnvironment("Fresh App"))

    Clerk.initialize(context = context, publishableKey = PUBLISHABLE_KEY)

    // The corrupted cache entry should be treated as a cache miss, not crash initialization.
    assertNull(Clerk.environment)

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
      coEvery { Client.get() } returns ClerkResult.success(Client())
      coEvery { Client.getSkippingClientId() } returns ClerkResult.success(Client())
      coEvery { Environment.get() } returns ClerkResult.success(testEnvironment("Fresh App"))

      Clerk.initialize(context = context, publishableKey = PUBLISHABLE_KEY)
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
  }
}
