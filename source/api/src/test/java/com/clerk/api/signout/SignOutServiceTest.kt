package com.clerk.api.signout

import android.content.Context
import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.SessionApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.environment.DisplayConfig
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.model.environment.UserSettings
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.Session
import com.clerk.api.storage.StorageHelper
import com.clerk.api.storage.StorageKey
import com.clerk.api.user.User
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SignOutServiceTest {
  private val testDispatcher = StandardTestDispatcher()
  private lateinit var context: Context
  private lateinit var mockClient: Client
  private lateinit var mockEnvironment: Environment
  private lateinit var mockSession: Session
  private lateinit var mockUser: User
  private lateinit var mockSessionApi: SessionApi

  @OptIn(ExperimentalCoroutinesApi::class)
  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    context = RuntimeEnvironment.getApplication()

    // Initialize StorageHelper with real context
    StorageHelper.initialize(context)
    StorageHelper.reset(context)

    // Create mocks
    mockClient = mockk()
    mockEnvironment = mockk()
    mockSession = mockk(relaxed = true)
    mockUser = mockk(relaxed = true)
    mockSessionApi = mockk()

    // Mock ClerkApi.session
    mockkObject(ClerkApi)
    every { ClerkApi.session } returns mockSessionApi

    // Setup environment mock
    val mockDisplayConfig = mockk<DisplayConfig>()
    val mockUserSettings = mockk<UserSettings>()
    every { mockEnvironment.displayConfig } returns mockDisplayConfig
    every { mockEnvironment.userSettings } returns mockUserSettings
    every { mockDisplayConfig.logoImageUrl } returns ""
    every { mockDisplayConfig.applicationName } returns ""
    every { mockUserSettings.social } returns emptyMap()
    every { mockEnvironment.passkeyIsEnabled } returns false
    every { mockEnvironment.mfaIsEnabled } returns false
    every { mockEnvironment.mfaAuthenticatorAppIsEnabled } returns false
    every { mockEnvironment.passwordIsEnabled } returns false
    every { mockEnvironment.usernameIsEnabled } returns false
    every { mockEnvironment.firstNameIsEnabled } returns false
    every { mockEnvironment.lastNameIsEnabled } returns false
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
    StorageHelper.reset(context)
  }

  private fun setupActiveSession() {
    val activeSessionId = "test_session_id"
    every { mockClient.lastActiveSessionId } returns activeSessionId
    every { mockClient.sessions } returns listOf(mockSession)
    every { mockClient.activeSessions() } returns listOf(mockSession)
    every { mockClient.signIn } returns null
    every { mockClient.signUp } returns null
    every { mockClient.id } returns "test_client_id"
    every { mockClient.updatedAt } returns null
    every { mockSession.id } returns activeSessionId
    every { mockSession.user } returns mockUser

    Clerk.updateClient(mockClient)
    Clerk.environment = mockEnvironment
  }

  @Test
  fun `signOut clears device token on successful server sign-out`() = runTest {
    // Given - active session with device token stored
    setupActiveSession()
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "test_device_token")

    // Mock successful server sign-out
    coEvery { mockSessionApi.removeSession(any()) } returns ClerkResult.success(mockSession)

    // Verify device token exists before sign-out
    assertTrue(StorageHelper.loadValue(StorageKey.DEVICE_TOKEN) != null)

    // When
    val result = SignOutService.signOut()

    // Then
    assertTrue("Sign-out should succeed", result is ClerkResult.Success)
    assertNull("Device token should be deleted", StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
    assertNull("Session should be cleared", Clerk.session)
    assertNull("User should be cleared", Clerk.user)
    assertFalse("isSignedIn should be false", Clerk.isSignedIn)
  }

  @Test
  fun `signOut clears device token even when server sign-out fails`() = runTest {
    // Given - active session with device token stored
    setupActiveSession()
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "test_device_token")

    // Mock server sign-out failure (network error)
    coEvery { mockSessionApi.removeSession(any()) } throws Exception("Network error")

    // Verify device token exists before sign-out
    assertTrue(StorageHelper.loadValue(StorageKey.DEVICE_TOKEN) != null)

    // When
    val result = SignOutService.signOut()

    // Then
    assertTrue("Sign-out should return failure", result is ClerkResult.Failure)
    assertNull(
      "Device token should still be deleted on failure",
      StorageHelper.loadValue(StorageKey.DEVICE_TOKEN),
    )
    assertNull("Session should be cleared on failure", Clerk.session)
    assertNull("User should be cleared on failure", Clerk.user)
    assertFalse("isSignedIn should be false on failure", Clerk.isSignedIn)
  }

  @Test
  fun `signOut clears session and user state on successful sign-out`() = runTest {
    // Given - active session
    setupActiveSession()

    // Mock successful server sign-out
    coEvery { mockSessionApi.removeSession(any()) } returns ClerkResult.success(mockSession)

    // Verify session exists before sign-out
    assertTrue("Session should exist before sign-out", Clerk.session != null)
    assertTrue("User should exist before sign-out", Clerk.user != null)
    assertTrue("isSignedIn should be true before sign-out", Clerk.isSignedIn)

    // When
    val result = SignOutService.signOut()

    // Then
    assertTrue("Sign-out should succeed", result is ClerkResult.Success)
    assertNull("Session should be null after sign-out", Clerk.sessionFlow.value)
    assertNull("User should be null after sign-out", Clerk.userFlow.value)
  }

  @Test
  fun `signOut clears session and user state even when server sign-out fails`() = runTest {
    // Given - active session
    setupActiveSession()

    // Mock server sign-out failure
    coEvery { mockSessionApi.removeSession(any()) } throws Exception("Server error")

    // Verify session exists before sign-out
    assertTrue("Session should exist before sign-out", Clerk.session != null)
    assertTrue("User should exist before sign-out", Clerk.user != null)

    // When
    val result = SignOutService.signOut()

    // Then
    assertTrue("Sign-out should return failure", result is ClerkResult.Failure)
    assertNull("Session should be null even on failure", Clerk.sessionFlow.value)
    assertNull("User should be null even on failure", Clerk.userFlow.value)
  }

  @Test
  fun `signOut succeeds when no session exists`() = runTest {
    // Given - no active session
    val emptyClient = mockk<Client>()
    every { emptyClient.lastActiveSessionId } returns null
    every { emptyClient.sessions } returns emptyList()
    every { emptyClient.activeSessions() } returns emptyList()
    every { emptyClient.signIn } returns null
    every { emptyClient.signUp } returns null
    every { emptyClient.id } returns null
    every { emptyClient.updatedAt } returns null
    Clerk.updateClient(emptyClient)
    Clerk.environment = mockEnvironment

    // When
    val result = SignOutService.signOut()

    // Then - should succeed (nothing to sign out)
    assertTrue("Sign-out should succeed with no session", result is ClerkResult.Success)
  }

  @Test
  fun `signOut preserves device ID while clearing device token`() = runTest {
    // Given - active session with both device ID and device token stored
    setupActiveSession()
    StorageHelper.saveValue(StorageKey.DEVICE_ID, "test_device_id")
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "test_device_token")

    // Mock successful server sign-out
    coEvery { mockSessionApi.removeSession(any()) } returns ClerkResult.success(mockSession)

    // When
    SignOutService.signOut()

    // Then - device token should be deleted, but device ID should be preserved
    assertNull("Device token should be deleted", StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
    assertTrue(
      "Device ID should be preserved",
      StorageHelper.loadValue(StorageKey.DEVICE_ID) == "test_device_id",
    )
  }
}
