package com.clerk.api.sdk

import android.content.Context
import com.clerk.api.Clerk
import com.clerk.api.configuration.ConfigurationManager
import com.clerk.api.configuration.connectivity.NetworkConnectivityMonitor
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.environment.AuthConfig
import com.clerk.api.network.model.environment.DisplayConfig
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.model.environment.UserSettings
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.Session
import com.clerk.api.storage.StorageHelper
import com.clerk.api.storage.StorageKey
import com.clerk.api.user.User
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import java.lang.ref.WeakReference
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

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
}
