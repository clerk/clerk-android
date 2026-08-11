package com.clerk.api.sdk

import android.content.Context
import com.clerk.api.Clerk
import com.clerk.api.ClerkConfigurationOptions
import com.clerk.api.configuration.CachedClerkState
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
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Covers persistence and restoration of complete SDK state for offline cold starts. */
@RunWith(RobolectricTestRunner::class)
class ClerkOfflineCacheTest {
  private lateinit var context: Context

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    StorageHelper.initialize(context)
    StorageHelper.reset(context)
    Clerk.reset()
    mockkObject(Client.Companion)
    mockkObject(Environment.Companion)
    every { Client.serializer() } answers { callOriginal() }
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
  fun `client and environment updates persist a complete snapshot`() {
    val client = Client(id = "client_persisted")
    val environment = testEnvironment("Persisted App")
    Clerk.publishableKey = PUBLISHABLE_KEY
    Clerk.baseUrl = PROXY_URL

    Clerk.updateClient(client = client, serverFetchAtMillis = SERVER_FETCH_AT_MILLIS)
    Clerk.updateEnvironment(environment)

    val cachedState = loadCachedState()
    assertEquals(PUBLISHABLE_KEY, cachedState?.publishableKey)
    assertEquals(PROXY_URL, cachedState?.baseUrl)
    assertEquals(client, cachedState?.client)
    assertEquals(environment, cachedState?.environment)
    assertEquals(SERVER_FETCH_AT_MILLIS, cachedState?.clientServerFetchAtMillis)
  }

  @Test
  fun `environment without a client does not persist an incomplete snapshot`() {
    Clerk.publishableKey = PUBLISHABLE_KEY
    Clerk.baseUrl = PROXY_URL

    Clerk.updateEnvironment(testEnvironment("Environment Only"))

    assertNull(StorageHelper.loadValue(StorageKey.CACHED_CLERK_STATE))
  }

  @Test
  fun `reset clears cached state`() {
    saveCachedState()

    Clerk.reset()

    assertNull(StorageHelper.loadValue(StorageKey.CACHED_CLERK_STATE))
  }

  @Test
  fun `initialize restores complete state and readiness before network refresh`() = runBlocking {
    saveCachedState(
      client = Client(id = "client_cached"),
      environment = testEnvironment("Cached App"),
    )
    stubNeverCompletingRefresh()

    initialize()

    assertTrue(Clerk.isInitialized.value)
    assertEquals("client_cached", Clerk.client.id)
    assertEquals("Cached App", Clerk.applicationName)
  }

  @Test
  fun `failed offline refresh keeps restored state ready`() = runBlocking {
    saveCachedState(
      client = Client(id = "client_cached"),
      environment = testEnvironment("Cached App"),
    )
    coEvery { Client.get() } returns ClerkResult.unknownFailure(IllegalStateException("offline"))
    coEvery { Client.getSkippingClientId() } returns
      ClerkResult.unknownFailure(IllegalStateException("offline"))
    coEvery { Environment.get() } returns
      ClerkResult.unknownFailure(IllegalStateException("offline"))

    initialize()
    coVerify(timeout = 5_000) { Environment.get() }
    delay(50)

    assertTrue(Clerk.isInitialized.value)
    assertNull(Clerk.initializationError.value)
    assertEquals("client_cached", Clerk.client.id)
    assertEquals("Cached App", Clerk.applicationName)
  }

  @Test
  fun `cache for another configuration is ignored`() = runBlocking {
    saveCachedState(publishableKey = "pk_test_other", baseUrl = "https://other.example.com")
    stubNeverCompletingRefresh()

    initialize()

    assertFalse(Clerk.isInitialized.value)
    assertNull(Clerk.clientFlow.value)
    assertNull(Clerk.environment)
  }

  @Test
  fun `corrupted cached state is ignored`() = runBlocking {
    StorageHelper.saveValue(StorageKey.CACHED_CLERK_STATE, "not-valid-cache-json")
    stubNeverCompletingRefresh()

    initialize()

    assertFalse(Clerk.isInitialized.value)
    assertNull(Clerk.clientFlow.value)
    assertNull(Clerk.environment)
  }

  @Test
  fun `fresh refresh replaces restored state and persisted snapshot`() = runBlocking {
    saveCachedState(
      client = Client(id = "client_cached"),
      environment = testEnvironment("Cached App"),
    )
    val clientDeferred = CompletableDeferred<ClerkResult<Client, Nothing>>()
    val environmentDeferred = CompletableDeferred<ClerkResult<Environment, Nothing>>()
    coEvery { Client.get() } coAnswers { clientDeferred.await() }
    coEvery { Client.getSkippingClientId() } coAnswers { clientDeferred.await() }
    coEvery { Environment.get() } coAnswers { environmentDeferred.await() }

    initialize()
    clientDeferred.complete(ClerkResult.success(Client(id = "client_fresh")))
    environmentDeferred.complete(ClerkResult.success(testEnvironment("Fresh App")))
    waitUntil { Clerk.applicationName == "Fresh App" }

    assertEquals("client_fresh", Clerk.client.id)
    assertEquals("Fresh App", Clerk.applicationName)
    assertEquals("client_fresh", loadCachedState()?.client?.id)
    assertEquals("Fresh App", loadCachedState()?.environment?.displayConfig?.applicationName)
  }

  private fun initialize() {
    Clerk.initialize(
      context = context,
      publishableKey = PUBLISHABLE_KEY,
      options = ClerkConfigurationOptions(proxyUrl = PROXY_URL),
    )
  }

  private fun stubNeverCompletingRefresh() {
    val clientDeferred = CompletableDeferred<ClerkResult<Client, Nothing>>()
    val environmentDeferred = CompletableDeferred<ClerkResult<Environment, Nothing>>()
    coEvery { Client.get() } coAnswers { clientDeferred.await() }
    coEvery { Client.getSkippingClientId() } coAnswers { clientDeferred.await() }
    coEvery { Environment.get() } coAnswers { environmentDeferred.await() }
  }

  private fun saveCachedState(
    client: Client = Client(id = "client_cached"),
    environment: Environment = testEnvironment("Cached App"),
    publishableKey: String = PUBLISHABLE_KEY,
    baseUrl: String = PROXY_URL,
  ) {
    val state =
      CachedClerkState(
        publishableKey = publishableKey,
        baseUrl = baseUrl,
        client = client,
        environment = environment,
        clientServerFetchAtMillis = SERVER_FETCH_AT_MILLIS,
      )
    StorageHelper.saveValue(
      StorageKey.CACHED_CLERK_STATE,
      ClerkApi.json.encodeToString(CachedClerkState.serializer(), state),
    )
  }

  private fun loadCachedState(): CachedClerkState? {
    val cachedJson = StorageHelper.loadValue(StorageKey.CACHED_CLERK_STATE) ?: return null
    return ClerkApi.json.decodeFromString(CachedClerkState.serializer(), cachedJson)
  }

  private suspend fun waitUntil(condition: () -> Boolean) {
    withTimeout(5_000) {
      while (!condition()) {
        delay(10)
      }
    }
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
    const val PUBLISHABLE_KEY = "pk_test_cache_state"
    const val PROXY_URL = "https://proxy.example.com/__clerk"
    const val SERVER_FETCH_AT_MILLIS = 1_786_464_000_000L
  }
}
