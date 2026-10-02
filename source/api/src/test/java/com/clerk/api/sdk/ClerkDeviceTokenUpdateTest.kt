package com.clerk.api.sdk

import android.content.Context
import com.clerk.api.Clerk
import com.clerk.api.FrameworkIntegrationApi
import com.clerk.api.configuration.ConfigurationManager
import com.clerk.api.configuration.connectivity.NetworkConnectivityMonitor
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.environment.AuthConfig
import com.clerk.api.network.model.environment.DisplayConfig
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.model.environment.UserSettings
import com.clerk.api.network.model.token.TokenResource
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.Session
import com.clerk.api.session.SessionTokensCache
import com.clerk.api.storage.StorageHelper
import com.clerk.api.storage.StorageKey
import com.clerk.api.storage.failCommitsForTesting
import com.clerk.api.user.User
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import java.lang.ref.WeakReference
import java.util.Base64
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(FrameworkIntegrationApi::class)
@RunWith(RobolectricTestRunner::class)
class ClerkDeviceTokenUpdateTest {
  private lateinit var context: Context

  @Before
  fun setup() {
    context = RuntimeEnvironment.getApplication()
    StorageHelper.reset(context)
    resetClerkState()
    mockkObject(Client.Companion)
    mockkObject(Environment.Companion)
  }

  @After
  fun tearDown() {
    unmockkAll()
    StorageHelper.reset(context)
    NetworkConnectivityMonitor.resetForTesting()
    resetClerkState()
  }

  @Test
  fun `updateDeviceToken fails when Clerk has not been initialized`() = runTest {
    val result = Clerk.updateDeviceToken("device_token_123")

    assertTrue(result is ClerkResult.Failure)
    result as ClerkResult.Failure
    assertEquals(
      "Clerk must be initialized before updating the device token",
      result.throwable?.message,
    )
  }

  @Test
  fun `updateDeviceToken rejects blank tokens`() = runTest {
    configureClerkForDeviceTokenUpdate()

    val result = Clerk.updateDeviceToken("   ")

    assertTrue(result is ClerkResult.Failure)
    result as ClerkResult.Failure
    assertEquals("Device token must not be blank", result.throwable?.message)
  }

  @Test
  fun `clearDeviceToken fails when Clerk has not been initialized`() = runTest {
    val result = Clerk.clearDeviceToken()

    assertTrue(result is ClerkResult.Failure)
    result as ClerkResult.Failure
    assertEquals(
      "Clerk must be initialized before clearing the device token",
      result.throwable?.message,
    )
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `updateDeviceToken persists token and refreshes client state without default client lookup`() =
    runTest {
      configureClerkForDeviceTokenUpdate()

      val staleClient = Client(id = "client_anon")
      val staleEnvironment = testEnvironment(applicationName = "Before Refresh")
      val refreshedUser = mockk<User>(relaxed = true)
      val refreshedSession = mockk<Session>(relaxed = true)
      every { refreshedSession.id } returns "sess_123"
      every { refreshedSession.status } returns Session.SessionStatus.PENDING
      every { refreshedSession.user } returns refreshedUser

      val refreshedClient =
        Client(
          id = "client_real",
          sessions = listOf(refreshedSession),
          lastActiveSessionId = "sess_123",
        )
      val refreshedEnvironment = testEnvironment(applicationName = "After Refresh")

      Clerk.updateClient(staleClient)
      Clerk.updateEnvironment(staleEnvironment)

      coEvery { Client.getSkippingClientId() } returns ClerkResult.success(refreshedClient)
      coEvery { Client.get() } returns
        ClerkResult.unknownFailure(IllegalStateException("Client.get() should not be called"))
      coEvery { Environment.get() } returns ClerkResult.success(refreshedEnvironment)

      val result = Clerk.updateDeviceToken("device_token_123")

      assertTrue(result is ClerkResult.Success)
      assertEquals("device_token_123", StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
      assertEquals("client_real", Clerk.client.id)
      assertEquals(refreshedSession, Clerk.session)
      assertEquals(refreshedUser, Clerk.user)
      assertEquals("After Refresh", Clerk.applicationName)
      coVerify(exactly = 1) { Client.getSkippingClientId() }
      coVerify(exactly = 0) { Client.get() }
    }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `clearDeviceToken removes token and refreshes client state without default client lookup`() =
    runTest {
      configureClerkForDeviceTokenUpdate()

      val staleClient = Client(id = "client_anon")
      val staleEnvironment = testEnvironment(applicationName = "Before Refresh")
      val refreshedClient = Client(id = "client_refreshed")
      val refreshedEnvironment = testEnvironment(applicationName = "After Refresh")

      StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "device_token_123")
      SessionTokensCache.setToken("sess_123", TokenResource(jwt = "jwt_123"))
      Clerk.updateClient(staleClient)
      Clerk.updateEnvironment(staleEnvironment)

      coEvery { Client.getSkippingClientId() } coAnswers
        {
          StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "device_token_refreshed")
          ClerkResult.success(refreshedClient)
        }
      coEvery { Client.get() } returns
        ClerkResult.unknownFailure(IllegalStateException("Client.get() should not be called"))
      coEvery { Environment.get() } returns ClerkResult.success(refreshedEnvironment)

      val result = Clerk.clearDeviceToken()

      assertTrue(result is ClerkResult.Success)
      assertNull(StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
      assertEquals(0, SessionTokensCache.size)
      assertEquals("client_refreshed", Clerk.client.id)
      assertNull(Clerk.session)
      assertNull(Clerk.user)
      assertEquals("After Refresh", Clerk.applicationName)
      coVerify(exactly = 1) { Client.getSkippingClientId() }
      coVerify(exactly = 0) { Client.get() }
    }

  @Test
  fun `setDeviceToken stores token when expected matches stored token`() {
    configureClerkForDeviceTokenUpdate()
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "device_token_old")

    val result = Clerk.setDeviceToken(token = "device_token_new", expected = "device_token_old")

    assertTrue(result)
    assertEquals("device_token_new", StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
  }

  @Test
  fun `setDeviceToken leaves stored token unchanged when expected does not match`() {
    configureClerkForDeviceTokenUpdate()
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "device_token_newer")

    val result = Clerk.setDeviceToken(token = "device_token_stale", expected = "device_token_old")

    assertFalse(result)
    assertEquals("device_token_newer", StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
  }

  @Test
  fun `setDeviceToken with null expected stores token only when none is stored`() {
    configureClerkForDeviceTokenUpdate()

    assertTrue(Clerk.setDeviceToken(token = "device_token_first", expected = null))
    assertEquals("device_token_first", StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))

    assertFalse(Clerk.setDeviceToken(token = "device_token_second", expected = null))
    assertEquals("device_token_first", StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
  }

  @Test
  fun `setDeviceToken with null token clears the stored token`() {
    configureClerkForDeviceTokenUpdate()
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "device_token_123")

    val result = Clerk.setDeviceToken(token = null, expected = "device_token_123")

    assertTrue(result)
    assertNull(StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
  }

  @Test
  fun `setDeviceToken rotation for the same client keeps state and queues no refresh`() {
    configureClerkForDeviceTokenUpdate()
    stubRefreshFailure()
    val session = testSession("sess_123")
    Clerk.updateClient(
      Client(id = "client_current", sessions = listOf(session), lastActiveSessionId = session.id)
    )
    val client = Clerk.client
    SessionTokensCache.setToken("sess_123", TokenResource(jwt = "jwt_123"))
    val oldToken = deviceTokenJwt(clientId = "client_current", rotatingToken = "r1")
    val newToken = deviceTokenJwt(clientId = "client_current", rotatingToken = "r2")
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, oldToken)
    val pendingWork = configurationScopeChildren()

    assertTrue(Clerk.setDeviceToken(token = newToken, expected = oldToken))

    assertEquals(pendingWork, configurationScopeChildren())
    awaitConfigurationScopeIdle()
    assertEquals(newToken, StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
    assertEquals(client, Clerk.client)
    assertEquals(session, Clerk.session)
    assertEquals(1, SessionTokensCache.size)
    verifyNoRefresh()
  }

  @Test
  fun `setDeviceToken swap to an opaque token keeps state and queues no refresh`() {
    configureClerkForDeviceTokenUpdate()
    stubRefreshFailure()
    val session = testSession("sess_123")
    Clerk.updateClient(
      Client(id = "client_current", sessions = listOf(session), lastActiveSessionId = session.id)
    )
    val client = Clerk.client
    SessionTokensCache.setToken("sess_123", TokenResource(jwt = "jwt_123"))
    val oldToken = deviceTokenJwt(clientId = "client_current", rotatingToken = "r1")
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, oldToken)
    val pendingWork = configurationScopeChildren()

    assertTrue(Clerk.setDeviceToken(token = "device_token_opaque", expected = oldToken))

    assertEquals(pendingWork, configurationScopeChildren())
    awaitConfigurationScopeIdle()
    assertEquals("device_token_opaque", StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
    assertEquals(client, Clerk.client)
    assertEquals(session, Clerk.session)
    assertEquals(1, SessionTokensCache.size)
    verifyNoRefresh()
  }

  @Test
  fun `setDeviceToken for a different client resets local state and refreshes the client`() {
    configureClerkForDeviceTokenUpdate()
    val session = testSession("sess_123")
    Clerk.updateClient(
      Client(id = "client_a", sessions = listOf(session), lastActiveSessionId = session.id)
    )
    SessionTokensCache.setToken("sess_123", TokenResource(jwt = "jwt_123"))
    val oldToken = deviceTokenJwt(clientId = "client_a", rotatingToken = "r1")
    val newToken = deviceTokenJwt(clientId = "client_b", rotatingToken = "r1")
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, oldToken)
    val releaseRefresh = stubGatedRefresh(Client(id = "client_b"))
    assertEquals(session, Clerk.session)

    assertTrue(Clerk.setDeviceToken(token = newToken, expected = oldToken))

    assertNull(Clerk.clientFlow.value?.id)
    assertNull(Clerk.session)
    assertEquals(0, SessionTokensCache.size)
    releaseRefresh.complete(Unit)
    awaitConfigurationScopeIdle()
    assertEquals("client_b", Clerk.client.id)
    assertEquals(newToken, StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
    coVerify(exactly = 1) { Client.getSkippingClientId() }
    coVerify(exactly = 0) { Client.get() }
  }

  @Test
  fun `setDeviceToken first token for a different client than the in-memory one resets state`() {
    configureClerkForDeviceTokenUpdate()
    val session = testSession("sess_123")
    Clerk.updateClient(
      Client(id = "client_a", sessions = listOf(session), lastActiveSessionId = session.id)
    )
    SessionTokensCache.setToken("sess_123", TokenResource(jwt = "jwt_123"))
    val newToken = deviceTokenJwt(clientId = "client_b", rotatingToken = "r1")
    val releaseRefresh = stubGatedRefresh(Client(id = "client_b"))

    assertTrue(Clerk.setDeviceToken(token = newToken, expected = null))

    assertNull(Clerk.clientFlow.value?.id)
    assertNull(Clerk.session)
    assertEquals(0, SessionTokensCache.size)
    releaseRefresh.complete(Unit)
    awaitConfigurationScopeIdle()
    assertEquals("client_b", Clerk.client.id)
    coVerify(exactly = 1) { Client.getSkippingClientId() }
  }

  @Test
  fun `setDeviceToken first token for the in-memory client keeps state and queues no refresh`() {
    configureClerkForDeviceTokenUpdate()
    stubRefreshFailure()
    val session = testSession("sess_123")
    Clerk.updateClient(
      Client(id = "client_a", sessions = listOf(session), lastActiveSessionId = session.id)
    )
    val client = Clerk.client
    SessionTokensCache.setToken("sess_123", TokenResource(jwt = "jwt_123"))
    val newToken = deviceTokenJwt(clientId = "client_a", rotatingToken = "r1")
    val pendingWork = configurationScopeChildren()

    assertTrue(Clerk.setDeviceToken(token = newToken, expected = null))

    assertEquals(pendingWork, configurationScopeChildren())
    awaitConfigurationScopeIdle()
    assertEquals(newToken, StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
    assertEquals(client, Clerk.client)
    assertEquals(session, Clerk.session)
    assertEquals(1, SessionTokensCache.size)
    verifyNoRefresh()
  }

  @Test
  fun `setDeviceToken no-op clear keeps state and queues no refresh`() {
    configureClerkForDeviceTokenUpdate()
    stubRefreshFailure()
    val session = testSession("sess_123")
    Clerk.updateClient(
      Client(id = "client_a", sessions = listOf(session), lastActiveSessionId = session.id)
    )
    val client = Clerk.client
    SessionTokensCache.setToken("sess_123", TokenResource(jwt = "jwt_123"))
    val initialFence = deviceTokenFenceGeneration()
    val pendingWork = configurationScopeChildren()

    assertTrue(Clerk.setDeviceToken(token = null, expected = null))

    assertEquals(pendingWork, configurationScopeChildren())
    awaitConfigurationScopeIdle()
    assertEquals(initialFence, deviceTokenFenceGeneration())
    assertEquals(client, Clerk.client)
    assertEquals(session, Clerk.session)
    assertEquals(1, SessionTokensCache.size)
    verifyNoRefresh()
  }

  @Test
  fun `setDeviceToken leaves token, fence and state unchanged when the write fails to commit`() {
    configureClerkForDeviceTokenUpdate()
    stubRefreshFailure()
    val session = testSession("sess_123")
    Clerk.updateClient(
      Client(id = "client_a", sessions = listOf(session), lastActiveSessionId = session.id)
    )
    val client = Clerk.client
    val oldToken = deviceTokenJwt(clientId = "client_a", rotatingToken = "r1")
    val newToken = deviceTokenJwt(clientId = "client_b", rotatingToken = "r1")
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, oldToken)
    val initialFence = deviceTokenFenceGeneration()
    val pendingWork = configurationScopeChildren()
    val restoreCommits = StorageHelper.failCommitsForTesting()

    val result =
      try {
        Clerk.setDeviceToken(token = newToken, expected = oldToken)
      } finally {
        restoreCommits()
      }

    assertFalse(result)
    assertEquals(pendingWork, configurationScopeChildren())
    assertEquals(oldToken, StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
    assertEquals(initialFence, deviceTokenFenceGeneration())
    assertEquals(client, Clerk.client)
    assertEquals(session, Clerk.session)
    verifyNoRefresh()
  }

  @Test
  fun `setDeviceToken with null token resets local state and refreshes the client`() {
    configureClerkForDeviceTokenUpdate()
    Clerk.updateClient(Client(id = "client_current"))
    SessionTokensCache.setToken("sess_123", TokenResource(jwt = "jwt_123"))
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "device_token_123")
    val releaseRefresh = stubGatedRefresh(Client(id = "client_new"))

    assertTrue(Clerk.setDeviceToken(token = null, expected = "device_token_123"))

    assertNull(Clerk.clientFlow.value?.id)
    assertEquals(0, SessionTokensCache.size)
    releaseRefresh.complete(Unit)
    awaitConfigurationScopeIdle()
    assertEquals("client_new", Clerk.client.id)
    coVerify(exactly = 1) { Client.getSkippingClientId() }
  }

  @Test
  fun `setDeviceToken treats a blank expected token as none stored`() {
    configureClerkForDeviceTokenUpdate()

    assertTrue(Clerk.setDeviceToken(token = "device_token_first", expected = "  "))
    assertEquals("device_token_first", StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))

    assertFalse(Clerk.setDeviceToken(token = "device_token_second", expected = ""))
    assertEquals("device_token_first", StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
  }

  @Test
  fun `setDeviceToken fences in-flight client responses only when the token changes`() {
    configureClerkForDeviceTokenUpdate()
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "device_token_old")
    val initialFence = deviceTokenFenceGeneration()

    Clerk.setDeviceToken(token = "device_token_stale", expected = "device_token_other")
    Clerk.setDeviceToken(token = "device_token_old", expected = "device_token_old")
    assertEquals(initialFence, deviceTokenFenceGeneration())

    Clerk.setDeviceToken(token = "device_token_new", expected = "device_token_old")
    assertEquals(initialFence + 1, deviceTokenFenceGeneration())
  }

  @Test
  fun `setDeviceToken rejects blank tokens`() {
    configureClerkForDeviceTokenUpdate()
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "device_token_123")

    assertThrows(IllegalArgumentException::class.java) {
      Clerk.setDeviceToken(token = "   ", expected = "device_token_123")
    }
    assertEquals("device_token_123", StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
  }

  @Test
  fun `setDeviceToken returns false and leaves storage untouched when Clerk is not initialized`() {
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "device_token_sentinel")
    val initialFence = deviceTokenFenceGeneration()

    assertFalse(
      Clerk.setDeviceToken(token = "device_token_new", expected = "device_token_sentinel")
    )

    assertEquals("device_token_sentinel", StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
    assertEquals(initialFence, deviceTokenFenceGeneration())
  }

  @Test
  fun `reinitialize remains a no-op when Clerk is already initialized`() {
    configureClerkForDeviceTokenUpdate(isInitialized = true)

    assertFalse(Clerk.reinitialize())
  }

  private fun configureClerkForDeviceTokenUpdate(isInitialized: Boolean = true) {
    val configurationManager = configurationManager()
    setField(configurationManager, "context", WeakReference(context))
    setField(configurationManager, "hasConfigured", true)
    setField(configurationManager, "storedOptions", null)
    setField(configurationManager, "storageInitialized", false)
    mutableStateFlow<Boolean>(configurationManager, "_isInitialized").value = isInitialized
    mutableStateFlow<Throwable?>(configurationManager, "_initializationError").value = null
  }

  private fun deviceTokenJwt(clientId: String, rotatingToken: String): String {
    val encoder = Base64.getUrlEncoder().withoutPadding()
    fun encode(json: String) = encoder.encodeToString(json.toByteArray())
    val header = encode("""{"alg":"RS256","typ":"JWT"}""")
    val payload = encode("""{"id":"$clientId","rotating_token":"$rotatingToken"}""")
    return "$header.$payload.signature"
  }

  private fun testSession(id: String): Session =
    Session(
      id = id,
      status = Session.SessionStatus.ACTIVE,
      expireAt = 10_000,
      lastActiveAt = 1_000,
      createdAt = 1_000,
      updatedAt = 1_000,
    )

  /** Stubs a refresh that returns [client] once the returned signal is completed. */
  private fun stubGatedRefresh(client: Client): CompletableDeferred<Unit> {
    val release = CompletableDeferred<Unit>()
    coEvery { Client.getSkippingClientId() } coAnswers
      {
        release.await()
        ClerkResult.success(client)
      }
    coEvery { Client.get() } returns
      ClerkResult.unknownFailure(IllegalStateException("Client.get() should not be called"))
    coEvery { Environment.get() } returns ClerkResult.success(testEnvironment())
    return release
  }

  private fun stubRefreshFailure() {
    coEvery { Client.getSkippingClientId() } returns
      ClerkResult.unknownFailure(IllegalStateException("No refresh expected"))
    coEvery { Client.get() } returns
      ClerkResult.unknownFailure(IllegalStateException("No refresh expected"))
    coEvery { Environment.get() } returns
      ClerkResult.unknownFailure(IllegalStateException("No refresh expected"))
  }

  private fun verifyNoRefresh() {
    coVerify(exactly = 0) { Client.get() }
    coVerify(exactly = 0) { Client.getSkippingClientId() }
    coVerify(exactly = 0) { Environment.get() }
  }

  /** Active jobs launched by the configuration manager; a queued refresh shows up here. */
  private fun configurationScopeChildren(): Set<Job> {
    val field = ConfigurationManager::class.java.getDeclaredField("scope")
    field.isAccessible = true
    val scope = field.get(configurationManager()) as CoroutineScope
    return scope.coroutineContext.job.children.filter { it.isActive }.toSet()
  }

  /** Joins every job the configuration manager launched, including ones launched while joining. */
  private fun awaitConfigurationScopeIdle() = runBlocking {
    withTimeout(ASYNC_TIMEOUT_MS) {
      var active = configurationScopeChildren()
      while (active.isNotEmpty()) {
        active.joinAll()
        active = configurationScopeChildren()
      }
    }
  }

  private fun deviceTokenFenceGeneration(): Int {
    val field =
      ConfigurationManager::class.java.getDeclaredField("sharedDeviceTokenFenceGeneration")
    field.isAccessible = true
    return (field.get(configurationManager()) as AtomicInteger).get()
  }

  private fun configurationManager(): ConfigurationManager {
    val field = Clerk::class.java.getDeclaredField("configurationManager")
    field.isAccessible = true
    return field.get(Clerk) as ConfigurationManager
  }

  private fun resetClerkState() {
    val configurationManager = configurationManager()
    cancelJobField(configurationManager, "refreshJob")
    cancelJobField(configurationManager, "initializationJob")
    setField(configurationManager, "context", null)
    setField(configurationManager, "hasConfigured", false)
    setField(configurationManager, "storedOptions", null)
    setField(configurationManager, "storageInitialized", false)
    mutableStateFlow<Boolean>(configurationManager, "_isInitialized").value = false
    mutableStateFlow<Throwable?>(configurationManager, "_initializationError").value = null

    Clerk.clearSessionAndUserState()
    Clerk.updateClient(Client())
    Clerk.updateEnvironment(testEnvironment())
  }

  @Suppress("UNCHECKED_CAST")
  private fun <T> mutableStateFlow(
    target: Any,
    name: String,
  ): kotlinx.coroutines.flow.MutableStateFlow<T> {
    val field = target.javaClass.getDeclaredField(name)
    field.isAccessible = true
    return field.get(target) as kotlinx.coroutines.flow.MutableStateFlow<T>
  }

  private fun setField(target: Any, name: String, value: Any?) {
    val field = target.javaClass.getDeclaredField(name)
    field.isAccessible = true
    field.set(target, value)
  }

  private fun cancelJobField(target: Any, name: String) {
    val field = target.javaClass.getDeclaredField(name)
    field.isAccessible = true
    (field.get(target) as? Job)?.cancel()
    field.set(target, null)
  }

  private fun testEnvironment(applicationName: String = "Test App"): Environment {
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
    const val ASYNC_TIMEOUT_MS = 5_000L
  }
}
