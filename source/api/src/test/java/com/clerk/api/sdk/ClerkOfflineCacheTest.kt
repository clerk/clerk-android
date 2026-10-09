package com.clerk.api.sdk

import android.content.Context
import com.clerk.api.Clerk
import com.clerk.api.ClerkConfigurationOptions
import com.clerk.api.Constants.Storage.CLERK_PREFERENCES_FILE_NAME
import com.clerk.api.SharedSessionSyncConfig
import com.clerk.api.configuration.CachedClerkState
import com.clerk.api.configuration.connectivity.NetworkConnectivityMonitor
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.environment.AuthConfig
import com.clerk.api.network.model.environment.DisplayConfig
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.model.environment.UserSettings
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.Session
import com.clerk.api.state.ClientStateStore
import com.clerk.api.storage.StorageCipher
import com.clerk.api.storage.StorageHelper
import com.clerk.api.storage.StorageKey
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

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
    StorageHelper.storageCipherFactoryOverride = null
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
    Clerk.stateStore.flushPersistence()

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
    Clerk.stateStore.flushPersistence()

    assertNull(StorageHelper.loadValue(StorageKey.CACHED_CLERK_STATE))
  }

  @Test
  fun `sign-out persists the signed-out snapshot before returning`() {
    Clerk.publishableKey = PUBLISHABLE_KEY
    Clerk.baseUrl = PROXY_URL
    val session =
      Session(
        id = "sess_123",
        status = Session.SessionStatus.ACTIVE,
        expireAt = 10_000,
        lastActiveAt = 1_000,
        createdAt = 1_000,
        updatedAt = 1_000,
      )
    Clerk.updateEnvironment(testEnvironment("Persisted App"))
    Clerk.updateClient(
      client =
        Client(id = "client_123", sessions = listOf(session), lastActiveSessionId = session.id),
      serverFetchAtMillis = SERVER_FETCH_AT_MILLIS,
    )
    Clerk.stateStore.flushPersistence()

    Clerk.updateClient(Client(id = "client_123"))

    assertEquals(emptyList<Session>(), loadCachedState()?.client?.sessions)
  }

  @Test
  fun `reset discards a pending coalesced write`() = runBlocking {
    Clerk.publishableKey = PUBLISHABLE_KEY
    Clerk.baseUrl = PROXY_URL
    Clerk.updateEnvironment(testEnvironment("Persisted App"))
    Clerk.updateClient(client = Client(id = "client_123"), serverFetchAtMillis = 1)

    Clerk.reset()
    delay(ClientStateStore.DEFAULT_PERSIST_DEBOUNCE_MILLIS * 3)

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
    withTimeout(5_000) { Clerk.isInitialized.first { it } }

    assertEquals("client_cached", Clerk.client.id)
    assertEquals("Cached App", Clerk.applicationName)
  }

  @Test
  fun `initialize defers keystore setup and cache decryption off the calling thread`() =
    runBlocking {
      val callingThread = Thread.currentThread()
      val keystoreThreads = ConcurrentLinkedQueue<Thread>()
      val decryptThreads = ConcurrentLinkedQueue<Thread>()
      val releaseKeystore = CountDownLatch(1)
      StorageHelper.resetToUninitializedForTesting()
      StorageHelper.storageCipherFactoryOverride = {
        keystoreThreads.add(Thread.currentThread())
        releaseKeystore.await(2, TimeUnit.SECONDS)
        PassThroughCipher(decryptThreads)
      }
      writeCachedStateWithoutInitializingStorage(cachedStateJson())
      stubNeverCompletingRefresh()

      initialize()

      assertFalse(Clerk.isInitialized.value)
      releaseKeystore.countDown()
      withTimeout(5_000) { Clerk.isInitialized.first { it } }

      assertEquals("client_cached", Clerk.client.id)
      assertTrue(keystoreThreads.isNotEmpty())
      assertTrue(decryptThreads.isNotEmpty())
      assertTrue(keystoreThreads.none { it === callingThread })
      assertTrue(decryptThreads.none { it === callingThread })
    }

  @Test
  fun `a client written while the cache is decrypting is not overwritten by it`() = runBlocking {
    val decryptStarted = CountDownLatch(1)
    val releaseDecrypt = CountDownLatch(1)
    StorageHelper.resetToUninitializedForTesting()
    StorageHelper.storageCipherFactoryOverride = {
      object : StorageCipher {
        override fun encrypt(plaintext: String): String = plaintext

        override fun decrypt(ciphertext: String): String {
          if (ciphertext.contains("client_cached")) {
            decryptStarted.countDown()
            releaseDecrypt.await(5, TimeUnit.SECONDS)
          }
          return ciphertext
        }
      }
    }
    writeCachedStateWithoutInitializingStorage(cachedStateJson())
    stubNeverCompletingRefresh()

    initialize()
    assertTrue(decryptStarted.await(5, TimeUnit.SECONDS))
    Clerk.updateClient(client = Client(id = "client_live"), serverFetchAtMillis = LIVE_FETCH_AT)
    releaseDecrypt.countDown()
    withTimeout(5_000) { Clerk.isInitialized.first { it } }

    assertEquals("client_live", Clerk.client.id)
    assertEquals(LIVE_FETCH_AT, Clerk.lastClientServerFetchAtMillis)
    assertEquals("Cached App", Clerk.applicationName)
  }

  @Test
  fun `a client written before restore still lets the cached environment make it ready`() =
    runBlocking {
      val releaseKeystore = CountDownLatch(1)
      StorageHelper.resetToUninitializedForTesting()
      StorageHelper.storageCipherFactoryOverride = {
        releaseKeystore.await(5, TimeUnit.SECONDS)
        PassThroughCipher(ConcurrentLinkedQueue())
      }
      writeCachedStateWithoutInitializingStorage(cachedStateJson())
      coEvery { Client.get() } returns ClerkResult.unknownFailure(IllegalStateException("offline"))
      coEvery { Client.getSkippingClientId() } returns
        ClerkResult.unknownFailure(IllegalStateException("offline"))
      coEvery { Environment.get() } returns
        ClerkResult.unknownFailure(IllegalStateException("offline"))

      val clientResetBySetDeviceToken = Client()

      initialize()
      Clerk.updateClient(client = clientResetBySetDeviceToken, serverFetchAtMillis = LIVE_FETCH_AT)
      releaseKeystore.countDown()
      withTimeout(5_000) { Clerk.isInitialized.first { it } }

      assertEquals(clientResetBySetDeviceToken, Clerk.client)
      assertEquals("Cached App", Clerk.applicationName)
    }

  @Test
  fun `initialize registers shared session sync before the cache restore finishes`() =
    runBlocking<Unit> {
      val releaseKeystore = CountDownLatch(1)
      StorageHelper.resetToUninitializedForTesting()
      StorageHelper.storageCipherFactoryOverride = {
        releaseKeystore.await(5, TimeUnit.SECONDS)
        PassThroughCipher(ConcurrentLinkedQueue())
      }
      writeCachedStateWithoutInitializingStorage(cachedStateJson())
      stubNeverCompletingRefresh()

      initialize(sharedSessionSync = SharedSessionSyncConfig.enabled)

      try {
        assertFalse(Clerk.isInitialized.value)
        assertNotNull(StorageHelper.valueChangeListener)
      } finally {
        releaseKeystore.countDown()
      }
      withTimeout(5_000) { Clerk.isInitialized.first { it } }
    }

  @Test
  fun `restoring the cache does not publish the cached state to sibling apps`() = runBlocking {
    saveCachedState()
    stubNeverCompletingRefresh()

    initialize(sharedSessionSync = SharedSessionSyncConfig.enabled)
    withTimeout(5_000) { Clerk.isInitialized.first { it } }

    assertEquals("client_cached", Clerk.client.id)
    assertNull(StorageHelper.loadValue(StorageKey.SHARED_SESSION_SYNC_SNAPSHOT))
  }

  @Test
  fun `a refresh started before the cache restore still replaces the restored client`() {
    val updateCountAtRefreshStart = Clerk.clientUpdateCount
    Clerk.restoreCachedClient(Client(id = "client_cached"), SERVER_FETCH_AT_MILLIS)

    val applied =
      Clerk.updateClientIfUnchangedSince(updateCountAtRefreshStart, Client(id = "client_fresh"))

    assertTrue(applied)
    assertEquals("client_fresh", Clerk.client.id)
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
    awaitInitializationRetryScheduled()

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
    coVerify(timeout = 5_000) { Environment.get() }

    assertFalse(Clerk.isInitialized.value)
    assertNull(Clerk.clientFlow.value)
    assertNull(Clerk.environment)
  }

  @Test
  fun `corrupted cached state is ignored`() = runBlocking {
    StorageHelper.saveValue(StorageKey.CACHED_CLERK_STATE, "not-valid-cache-json")
    stubNeverCompletingRefresh()

    initialize()
    coVerify(timeout = 5_000) { Environment.get() }

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
    waitUntil { loadCachedState()?.environment?.displayConfig?.applicationName == "Fresh App" }

    assertEquals("client_fresh", Clerk.client.id)
    assertEquals("Fresh App", Clerk.applicationName)
    assertEquals("client_fresh", loadCachedState()?.client?.id)
    assertEquals("Fresh App", loadCachedState()?.environment?.displayConfig?.applicationName)
  }

  private fun initialize(sharedSessionSync: SharedSessionSyncConfig? = null) {
    Clerk.initialize(
      context = context,
      publishableKey = PUBLISHABLE_KEY,
      options =
        ClerkConfigurationOptions(proxyUrl = PROXY_URL, sharedSessionSync = sharedSessionSync),
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
    StorageHelper.saveValue(
      StorageKey.CACHED_CLERK_STATE,
      cachedStateJson(client, environment, publishableKey, baseUrl),
    )
  }

  private fun cachedStateJson(
    client: Client = Client(id = "client_cached"),
    environment: Environment = testEnvironment("Cached App"),
    publishableKey: String = PUBLISHABLE_KEY,
    baseUrl: String = PROXY_URL,
  ): String {
    val state =
      CachedClerkState(
        publishableKey = publishableKey,
        baseUrl = baseUrl,
        client = client,
        environment = environment,
        clientServerFetchAtMillis = SERVER_FETCH_AT_MILLIS,
      )
    return ClerkApi.json.encodeToString(CachedClerkState.serializer(), state)
  }

  private fun writeCachedStateWithoutInitializingStorage(json: String) {
    context
      .getSharedPreferences(CLERK_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)
      .edit()
      .putString(StorageKey.CACHED_CLERK_STATE.name, "clerk:v1:$json")
      .commit()
  }

  private class PassThroughCipher(private val decryptThreads: MutableCollection<Thread>) :
    StorageCipher {
    override fun encrypt(plaintext: String): String = plaintext

    override fun decrypt(ciphertext: String): String {
      decryptThreads.add(Thread.currentThread())
      return ciphertext
    }
  }

  private fun loadCachedState(): CachedClerkState? {
    val cachedJson = StorageHelper.loadValue(StorageKey.CACHED_CLERK_STATE) ?: return null
    return ClerkApi.json.decodeFromString(CachedClerkState.serializer(), cachedJson)
  }

  private suspend fun awaitInitializationRetryScheduled() = waitUntil {
    initializationRetryJob() != null
  }

  private fun initializationRetryJob(): Any? {
    val configurationManager =
      Clerk::class.java.getDeclaredField("configurationManager").let {
        it.isAccessible = true
        it.get(Clerk)
      }
    return configurationManager.javaClass.getDeclaredField("initializationRetryJob").let {
      it.isAccessible = true
      it.get(configurationManager)
    }
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
    const val LIVE_FETCH_AT = 1_786_464_999_000L
  }
}
