package com.clerk.api.sdk

import android.content.Context
import com.clerk.api.Clerk
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.environment.DisplayConfig
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.model.environment.UserSettings
import com.clerk.api.session.Session
import com.clerk.api.signin.SignIn
import com.clerk.api.signup.SignUp
import com.clerk.api.user.User
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ClerkTest {
  private val testDispatcher = StandardTestDispatcher()
  private lateinit var mockContext: Context
  private lateinit var mockClient: Client
  private lateinit var mockEnvironment: Environment
  private lateinit var mockDisplayConfig: DisplayConfig
  private lateinit var mockUserSettings: UserSettings
  private lateinit var mockSession: Session
  private lateinit var mockUser: User
  private lateinit var mockSignIn: SignIn
  private lateinit var mockSignUp: SignUp

  @OptIn(ExperimentalCoroutinesApi::class)
  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)

    // Reset Clerk object state before each test
    resetClerkState()

    // Create mocks
    mockContext = mockk(relaxed = true)
    mockClient = mockk()
    mockEnvironment = mockk()
    mockDisplayConfig = mockk()
    mockUserSettings = mockk()
    mockSession = mockk(relaxed = true)
    mockUser = mockk(relaxed = true)
    mockSignIn = mockk(relaxed = true)
    mockSignUp = mockk(relaxed = true)

    // Set up basic client mock properties
    every { mockClient.sessions } returns emptyList()
    every { mockClient.lastActiveSessionId } returns null
    every { mockClient.signIn } returns null
    every { mockClient.signUp } returns null
    every { mockClient.id } returns null
    every { mockClient.updatedAt } returns null

    // Set up environment mock with display config and user settings
    every { mockEnvironment.displayConfig } returns mockDisplayConfig
    every { mockEnvironment.userSettings } returns mockUserSettings
    every { mockDisplayConfig.logoImageUrl } returns "https://example.com/logo.png"
    every { mockDisplayConfig.applicationName } returns "Test App"
    every { mockUserSettings.social } returns emptyMap()
    every { mockEnvironment.passkeyIsEnabled } returns false
    every { mockEnvironment.mfaIsEnabled } returns false
    every { mockEnvironment.mfaAuthenticatorAppIsEnabled } returns false
    every { mockEnvironment.passwordIsEnabled } returns true
    every { mockEnvironment.usernameIsEnabled } returns true
    every { mockEnvironment.firstNameIsEnabled } returns true
    every { mockEnvironment.lastNameIsEnabled } returns true
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
    resetClerkState()
  }

  private fun resetClerkState() {
    try {
      // Reset lateinit properties using reflection
      val clerkClass = Clerk::class.java

      // Reset client if it's initialized
      try {
        // We can't unset a lateinit property, but we can set it to a mock that behaves as
        // uninitialized
        val uninitializedClient = mockk<Client>()
        every { uninitializedClient.sessions } returns emptyList()
        every { uninitializedClient.lastActiveSessionId } returns null
        every { uninitializedClient.signIn } returns null
        every { uninitializedClient.signUp } returns null
        every { uninitializedClient.id } returns null
        every { uninitializedClient.updatedAt } returns null
        Clerk.updateClient(uninitializedClient)
      } catch (_: Exception) {
        // Client not initialized, that's fine
      }

      // Reset environment if it's initialized
      try {
        val environmentField = clerkClass.getDeclaredField("environment")
        environmentField.isAccessible = true
        // We can't unset a lateinit property, so we set it to a mock that behaves as uninitialized
        val uninitializedEnvironment = mockk<Environment>()
        val uninitializedDisplayConfig = mockk<DisplayConfig>()
        val uninitializedUserSettings = mockk<UserSettings>()
        every { uninitializedEnvironment.displayConfig } returns uninitializedDisplayConfig
        every { uninitializedEnvironment.userSettings } returns uninitializedUserSettings
        every { uninitializedDisplayConfig.logoImageUrl } returns ""
        every { uninitializedDisplayConfig.applicationName } returns ""
        every { uninitializedUserSettings.social } returns emptyMap()
        every { uninitializedEnvironment.passkeyIsEnabled } returns false
        every { uninitializedEnvironment.mfaIsEnabled } returns false
        every { uninitializedEnvironment.mfaAuthenticatorAppIsEnabled } returns false
        every { uninitializedEnvironment.passwordIsEnabled } returns false
        every { uninitializedEnvironment.usernameIsEnabled } returns false
        every { uninitializedEnvironment.firstNameIsEnabled } returns false
        every { uninitializedEnvironment.lastNameIsEnabled } returns false
        environmentField.set(Clerk, uninitializedEnvironment)
      } catch (_: Exception) {
        // Environment not initialized, that's fine
      }

      // Reset StateFlow values to ensure clean state between tests
      try {
        // Force update session and user state to null by triggering state update with empty client
        Clerk.updateSessionAndUserState()
      } catch (_: Exception) {
        // Ignore if method is not accessible or other issues
      }
    } catch (_: Exception) {
      // Ignore reflection errors - they mean the fields weren't accessible
    }
  }

  private fun initializeClerkWithClient(client: Client) {
    Clerk.updateClient(client)
    Clerk.environment = mockEnvironment
  }

  private fun initializeClerkWithEnvironment() {
    Clerk.environment = mockEnvironment
  }

  private fun simulateUninitializedClient() {
    val uninitializedClient = mockk<Client>()
    every { uninitializedClient.sessions } returns emptyList()
    every { uninitializedClient.lastActiveSessionId } returns null
    every { uninitializedClient.signIn } returns null
    every { uninitializedClient.signUp } returns null
    every { uninitializedClient.id } returns null
    every { uninitializedClient.updatedAt } returns null
    Clerk.updateClient(uninitializedClient)
  }

  private fun simulateUninitializedEnvironment() {
    val uninitializedEnvironment = mockk<Environment>(relaxed = true)
    val uninitializedDisplayConfig = mockk<DisplayConfig>(relaxed = true)
    val uninitializedUserSettings = mockk<UserSettings>(relaxed = true)
    every { uninitializedEnvironment.displayConfig } returns uninitializedDisplayConfig
    every { uninitializedEnvironment.userSettings } returns uninitializedUserSettings
    every { uninitializedDisplayConfig.logoImageUrl } returns ""
    every { uninitializedDisplayConfig.applicationName } returns ""
    every { uninitializedUserSettings.social } returns emptyMap()
    every { uninitializedEnvironment.passkeyIsEnabled } returns false
    Clerk.environment = uninitializedEnvironment
  }

  @Test
  fun `session returns null when client is not initialized`() = runTest {
    // Given - simulate uninitialized client
    simulateUninitializedClient()

    // When
    val session = Clerk.session

    // Then
    assertNull(session)
  }

  @Test
  fun `session returns null when client has no last active session ID`() = runTest {
    // Given
    every { mockClient.lastActiveSessionId } returns null
    every { mockClient.sessions } returns listOf(mockSession)
    initializeClerkWithClient(mockClient)

    // When
    val session = Clerk.session

    // Then
    assertNull(session)
  }

  @Test
  fun `session returns null when no sessions match active session ID`() = runTest {
    // Given
    val activeSessionId = "active_session_id"
    val differentSessionId = "different_session_id"

    every { mockClient.lastActiveSessionId } returns activeSessionId
    every { mockClient.sessions } returns listOf(mockSession)
    every { mockSession.id } returns differentSessionId
    initializeClerkWithClient(mockClient)

    // When
    val session = Clerk.session

    // Then
    assertNull(session)
  }

  @Test
  fun `session returns matching session when client has session`() = runTest {
    // Given
    val activeSessionId = "active_session_id"

    every { mockClient.lastActiveSessionId } returns activeSessionId
    every { mockClient.sessions } returns listOf(mockSession)
    every { mockSession.id } returns activeSessionId
    every { mockSession.status } returns Session.SessionStatus.ACTIVE
    initializeClerkWithClient(mockClient)

    // When
    val session = Clerk.session

    // Then
    assertEquals(mockSession, session)
  }

  @Test
  fun `user returns null when no active session exists`() = runTest {
    // Given - simulate uninitialized client
    simulateUninitializedClient()

    // When
    val user = Clerk.userFlow.value

    // Then
    assertNull(user)
  }

  @Test
  fun `user returns user when session exists`() = runTest {
    // Given
    val activeSessionId = "active_session_id"

    every { mockClient.lastActiveSessionId } returns activeSessionId
    every { mockClient.sessions } returns listOf(mockSession)
    every { mockSession.id } returns activeSessionId
    every { mockSession.user } returns mockUser
    every { mockSession.status } returns Session.SessionStatus.ACTIVE
    initializeClerkWithClient(mockClient)

    // When
    val user = Clerk.userFlow.value

    // Then
    assertEquals(mockUser, user)
  }

  @Test
  fun `isSignedIn returns false when no session exists`() = runTest {
    // Given - simulate uninitialized client
    simulateUninitializedClient()

    // When
    val isSignedIn = Clerk.isSignedIn

    // Then
    assertFalse(isSignedIn)
  }

  @Test
  fun `isSignedIn returns true when session exists`() = runTest {
    // Given
    val activeSessionId = "active_session_id"

    every { mockClient.lastActiveSessionId } returns activeSessionId
    every { mockClient.sessions } returns listOf(mockSession)
    every { mockSession.id } returns activeSessionId
    every { mockSession.status } returns Session.SessionStatus.ACTIVE
    initializeClerkWithClient(mockClient)

    // When
    val isSignedIn = Clerk.isSignedIn

    // Then
    assertTrue(isSignedIn)
  }

  @Test
  fun `currentSignIn returns null when client is not initialized`() = runTest {
    // Given - simulate uninitialized client
    simulateUninitializedClient()

    // When
    val signIn = Clerk.auth.currentSignIn

    // Then
    assertNull(signIn)
  }

  @Test
  fun `currentSignIn returns sign in when client is initialized`() = runTest {
    // Given
    every { mockClient.signIn } returns mockSignIn
    initializeClerkWithClient(mockClient)

    // When
    val signIn = Clerk.auth.currentSignIn

    // Then
    assertEquals(mockSignIn, signIn)
  }

  @Test
  fun `currentSignUp returns null when client is not initialized`() = runTest {
    // Given - simulate uninitialized client
    simulateUninitializedClient()

    // When
    val signUp = Clerk.auth.currentSignUp

    // Then
    assertNull(signUp)
  }

  @Test
  fun `currentSignUp returns sign up when client is initialized`() = runTest {
    // Given
    every { mockClient.signUp } returns mockSignUp
    initializeClerkWithClient(mockClient)

    // When
    val signUp = Clerk.auth.currentSignUp

    // Then
    assertEquals(mockSignUp, signUp)
  }

  @Test
  fun `logoUrl returns null when environment is not initialized`() = runTest {
    // Given - simulate uninitialized environment
    simulateUninitializedEnvironment()

    // When
    val logoUrl = Clerk.organizationLogoUrl

    // Then
    assertEquals("", logoUrl)
  }

  @Test
  fun `logoUrl returns logo URL when environment is initialized`() = runTest {
    // Given
    val expectedLogoUrl = "https://example.com/logo.png"
    every { mockDisplayConfig.logoImageUrl } returns expectedLogoUrl
    initializeClerkWithEnvironment()

    // When
    val logoUrl = Clerk.organizationLogoUrl

    // Then
    assertEquals(expectedLogoUrl, logoUrl)
  }

  @Test
  fun `applicationName returns null when environment is not initialized`() = runTest {
    // Given - simulate uninitialized environment
    simulateUninitializedEnvironment()

    // When
    val applicationName = Clerk.applicationName

    // Then
    assertEquals("", applicationName)
  }

  @Test
  fun `applicationName returns name when environment is initialized`() = runTest {
    // Given
    val expectedAppName = "Test App"
    every { mockDisplayConfig.applicationName } returns expectedAppName
    initializeClerkWithEnvironment()

    // When
    val applicationName = Clerk.applicationName

    // Then
    assertEquals(expectedAppName, applicationName)
  }

  @Test
  fun `socialProviders returns empty map when environment is not initialized`() = runTest {
    // Given - simulate uninitialized environment
    simulateUninitializedEnvironment()

    // When
    val providers = Clerk.socialProviders

    // Then
    assertTrue(providers.isEmpty())
  }

  @Test
  fun `socialProviders returns providers when environment is initialized`() = runTest {
    // Given
    val expectedProviders = mapOf("oauth_google" to mockk<UserSettings.SocialConfig>())
    every { mockUserSettings.social } returns expectedProviders
    initializeClerkWithEnvironment()

    // When
    val providers = Clerk.socialProviders

    // Then
    assertEquals(expectedProviders, providers)
  }

  @Test
  fun `debugMode is false by default`() = runTest {
    // When
    val debugMode = Clerk.debugMode

    // Then
    assertFalse(debugMode)
  }

  @Test
  fun `clearSessionAndUserState nulls both sessionFlow and userFlow`() = runTest {
    // Given - setup initial state with session and user
    val activeSessionId = "active_session_id"

    every { mockClient.lastActiveSessionId } returns activeSessionId
    every { mockClient.sessions } returns listOf(mockSession)
    every { mockSession.id } returns activeSessionId
    every { mockSession.user } returns mockUser
    every { mockSession.status } returns Session.SessionStatus.ACTIVE
    initializeClerkWithClient(mockClient)

    // Verify initial state has session and user
    assertEquals(mockSession, Clerk.sessionFlow.value)
    assertEquals(mockUser, Clerk.userFlow.value)
    assertEquals(mockSession, Clerk.session)
    assertEquals(mockUser, Clerk.user)
    assertTrue(Clerk.isSignedIn)

    // When - clear session and user state (simulating sign out)
    Clerk.clearSessionAndUserState()

    // Then - verify both flows are null
    assertNull(Clerk.sessionFlow.value)
    assertNull(Clerk.userFlow.value)
    assertNull(Clerk.session)
    assertNull(Clerk.user)
    assertFalse(Clerk.isSignedIn)
  }

  @Test
  fun `session property returns sessionFlow value`() = runTest {
    // Given - setup initial state
    val activeSessionId = "active_session_id"

    every { mockClient.lastActiveSessionId } returns activeSessionId
    every { mockClient.sessions } returns listOf(mockSession)
    every { mockSession.id } returns activeSessionId
    every { mockSession.status } returns Session.SessionStatus.ACTIVE
    initializeClerkWithClient(mockClient)

    // When
    val sessionFromProperty = Clerk.session
    val sessionFromFlow = Clerk.sessionFlow.value

    // Then - both should return the same value
    assertEquals(sessionFromFlow, sessionFromProperty)
    assertEquals(mockSession, sessionFromProperty)
  }

  @Test
  fun `user property returns userFlow value`() = runTest {
    // Given - setup initial state
    val activeSessionId = "active_session_id"

    every { mockClient.lastActiveSessionId } returns activeSessionId
    every { mockClient.sessions } returns listOf(mockSession)
    every { mockSession.id } returns activeSessionId
    every { mockSession.user } returns mockUser
    every { mockSession.status } returns Session.SessionStatus.ACTIVE
    initializeClerkWithClient(mockClient)

    // When
    val userFromProperty = Clerk.user
    val userFromFlow = Clerk.userFlow.value

    // Then - both should return the same value
    assertEquals(userFromFlow, userFromProperty)
    assertEquals(mockUser, userFromProperty)
  }

  @Test
  fun `updateSessionAndUserState updates both flows correctly`() = runTest {
    // Given - setup initial state with no session
    simulateUninitializedClient()
    assertNull(Clerk.sessionFlow.value)
    assertNull(Clerk.userFlow.value)

    // When - update client to have a session
    val activeSessionId = "active_session_id"
    every { mockClient.lastActiveSessionId } returns activeSessionId
    every { mockClient.sessions } returns listOf(mockSession)
    every { mockSession.id } returns activeSessionId
    every { mockSession.user } returns mockUser
    every { mockSession.status } returns Session.SessionStatus.ACTIVE
    Clerk.updateClient(mockClient)

    // Then - both flows should be updated
    assertEquals(mockSession, Clerk.sessionFlow.value)
    assertEquals(mockUser, Clerk.userFlow.value)
    assertEquals(mockSession, Clerk.session)
    assertEquals(mockUser, Clerk.user)
  }

  // region Pending Session Tests

  @Test
  fun `session returns matching session even when status is pending`() = runTest {
    // Given - session with pending status matching lastActiveSessionId
    val sessionId = "pending_session_id"
    val pendingSession = mockk<Session>(relaxed = true)

    every { pendingSession.id } returns sessionId
    every { pendingSession.status } returns Session.SessionStatus.PENDING
    every { pendingSession.user } returns mockUser
    every { mockClient.lastActiveSessionId } returns sessionId
    every { mockClient.sessions } returns listOf(pendingSession)
    initializeClerkWithClient(mockClient)

    // When
    val session = Clerk.session

    // Then - session should be returned even though it's pending
    assertEquals(pendingSession, session)
    assertEquals(Session.SessionStatus.PENDING, session?.status)
  }

  @Test
  fun `user returns user even when session is pending`() = runTest {
    // Given - session with pending status
    val sessionId = "pending_session_id"
    val pendingSession = mockk<Session>(relaxed = true)

    every { pendingSession.id } returns sessionId
    every { pendingSession.status } returns Session.SessionStatus.PENDING
    every { pendingSession.user } returns mockUser
    every { mockClient.lastActiveSessionId } returns sessionId
    every { mockClient.sessions } returns listOf(pendingSession)
    initializeClerkWithClient(mockClient)

    // When
    val user = Clerk.user

    // Then - user should be returned even though session is pending
    assertEquals(mockUser, user)
  }

  @Test
  fun `isSignedIn returns true for pending session`() = runTest {
    // Given - a session with PENDING status
    val sessionId = "pending_session_id"

    every { mockClient.lastActiveSessionId } returns sessionId
    every { mockClient.sessions } returns listOf(mockSession)
    every { mockSession.id } returns sessionId
    every { mockSession.status } returns Session.SessionStatus.PENDING
    initializeClerkWithClient(mockClient)

    // When
    val isSignedIn = Clerk.isSignedIn

    // Then - user with pending session should be considered signed in
    assertTrue(isSignedIn)
  }

  @Test
  fun `activeSession returns null when session is pending`() = runTest {
    // Given - session with pending status
    val sessionId = "pending_session_id"
    val pendingSession = mockk<Session>(relaxed = true)

    every { pendingSession.id } returns sessionId
    every { pendingSession.status } returns Session.SessionStatus.PENDING
    every { pendingSession.user } returns mockUser
    every { mockClient.lastActiveSessionId } returns sessionId
    every { mockClient.sessions } returns listOf(pendingSession)
    initializeClerkWithClient(mockClient)

    // When
    val activeSession = Clerk.activeSession

    // Then - activeSession should be null because session is pending
    assertNull(activeSession)
  }

  @Test
  fun `activeSession returns session when status is active`() = runTest {
    // Given - session with active status
    val sessionId = "active_session_id"
    val activeSession = mockk<Session>(relaxed = true)

    every { activeSession.id } returns sessionId
    every { activeSession.status } returns Session.SessionStatus.ACTIVE
    every { activeSession.user } returns mockUser
    every { mockClient.lastActiveSessionId } returns sessionId
    every { mockClient.sessions } returns listOf(activeSession)
    initializeClerkWithClient(mockClient)

    // When
    val result = Clerk.activeSession

    // Then - activeSession should be returned
    assertEquals(activeSession, result)
  }

  @Test
  fun `activeUser returns null when session is pending`() = runTest {
    // Given - session with pending status
    val sessionId = "pending_session_id"
    val pendingSession = mockk<Session>(relaxed = true)

    every { pendingSession.id } returns sessionId
    every { pendingSession.status } returns Session.SessionStatus.PENDING
    every { pendingSession.user } returns mockUser
    every { mockClient.lastActiveSessionId } returns sessionId
    every { mockClient.sessions } returns listOf(pendingSession)
    initializeClerkWithClient(mockClient)

    // When
    val activeUser = Clerk.activeUser

    // Then - activeUser should be null because session is pending
    assertNull(activeUser)
  }

  @Test
  fun `activeUser returns user when session is active`() = runTest {
    // Given - session with active status
    val sessionId = "active_session_id"
    val activeSession = mockk<Session>(relaxed = true)

    every { activeSession.id } returns sessionId
    every { activeSession.status } returns Session.SessionStatus.ACTIVE
    every { activeSession.user } returns mockUser
    every { mockClient.lastActiveSessionId } returns sessionId
    every { mockClient.sessions } returns listOf(activeSession)
    initializeClerkWithClient(mockClient)

    // When
    val activeUser = Clerk.activeUser

    // Then - activeUser should be returned
    assertEquals(mockUser, activeUser)
  }

  // endregion
}
