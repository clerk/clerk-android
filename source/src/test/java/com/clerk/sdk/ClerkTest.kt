package com.clerk.sdk

import android.content.Context
import com.clerk.Clerk
import com.clerk.model.client.Client
import com.clerk.model.environment.Environment
import com.clerk.model.session.Session
import com.clerk.model.user.User
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
  private lateinit var mockSession: Session
  private lateinit var mockUser: User

  @OptIn(ExperimentalCoroutinesApi::class)
  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)

    // Create mocks
    mockContext = mockk(relaxed = true)
    mockClient = mockk(relaxed = true)
    mockEnvironment = mockk(relaxed = true)
    mockSession = mockk(relaxed = true)
    mockUser = mockk(relaxed = true)

    // Mock the Clerk object
    mockkObject(Clerk)

    // Set default mock behavior for environment
    every { mockEnvironment.displayConfig.logoImageUrl } returns "https://example.com/logo.png"
    every { mockEnvironment.userSettings.social } returns emptyMap()
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

  @Test
  fun `session returns null when no active session exists`() = runTest {
    // Given
    every { Clerk.client } returns mockClient
    every { mockClient.lastActiveSessionId } returns "session_id"
    every { mockClient.sessions } returns emptyList()

    // When
    val session = Clerk.session

    // Then
    assertNull(session)
  }

  @Test
  fun `session returns active session when it exists`() = runTest {
    // Given
    val activeSessionId = "active_session_id"
    every { Clerk.client } returns mockClient
    every { mockClient.lastActiveSessionId } returns activeSessionId
    every { mockClient.sessions } returns listOf(mockSession)
    every { mockSession.id } returns activeSessionId

    // When
    val session = Clerk.session

    // Then
    assertEquals(mockSession, session)
  }

  @Test
  fun `user returns null when no active session exists`() = runTest {
    // Given - Mock session to return null
    every { Clerk.session } returns null

    // When
    val user = Clerk.user

    // Then
    assertNull(user)
  }

  @Test
  fun `user returns user when active session exists`() = runTest {
    // Given - Mock session to return mockSession with user
    every { Clerk.session } returns mockSession
    every { mockSession.user } returns mockUser

    // When
    val user = Clerk.user

    // Then
    assertEquals(mockUser, user)
  }

  @Test
  fun `session returns null when client has no last active session ID`() = runTest {
    // Given
    every { Clerk.client } returns mockClient
    every { mockClient.lastActiveSessionId } returns null
    every { mockClient.sessions } returns listOf(mockSession)

    // When
    val session = Clerk.session

    // Then
    assertNull(session)
  }

  @Test
  fun `session returns null when active session ID does not match any session`() = runTest {
    // Given
    val activeSessionId = "active_session_id"
    val differentSessionId = "different_session_id"

    every { Clerk.client } returns mockClient
    every { mockClient.lastActiveSessionId } returns activeSessionId
    every { mockClient.sessions } returns listOf(mockSession)
    every { mockSession.id } returns differentSessionId

    // When
    val session = Clerk.session

    // Then
    assertNull(session)
  }

  @Test
  fun `isSignedIn returns false when no session exists`() = runTest {
    // Given - Mock session property directly to bypass client initialization issues
    every { Clerk.session } returns null

    // When
    val isSignedIn = Clerk.isSignedIn

    // Then
    assertFalse(isSignedIn)
  }

  @Test
  fun `isSignedIn returns true when session exists`() = runTest {
    // Given - Mock session property directly to bypass client initialization issues
    every { Clerk.session } returns mockSession

    // When
    val isSignedIn = Clerk.isSignedIn

    // Then
    assertTrue(isSignedIn)
  }

  @Test
  fun `isSignedIn returns false when client is not initialized`() = runTest {
    // Given - Remove mocks to test actual uninitialized behavior
    unmockkAll()

    // When
    val isSignedIn = Clerk.isSignedIn

    // Then
    assertFalse(isSignedIn)
  }

  @Test
  fun `signIn returns null when client is not initialized`() = runTest {
    // Given - don't set up client mock, let it use the actual uninitialized state
    unmockkAll() // Remove mocks to test actual behavior

    // When
    val signIn = Clerk.signIn

    // Then
    assertNull(signIn)
  }

  @Test
  fun `logoUrl returns empty string when environment is not initialized`() = runTest {
    // Given - don't set up environment mock, let it use the actual uninitialized state
    unmockkAll() // Remove mocks to test actual behavior

    // When
    val logoUrl = Clerk.logoUrl

    // Then
    assertEquals("", logoUrl)
  }

  @Test
  fun `socialProviders returns empty map when environment is not initialized`() = runTest {
    // Given - don't set up environment mock, let it use the actual uninitialized state
    unmockkAll() // Remove mocks to test actual behavior

    // When
    val providers = Clerk.socialProviders

    // Then
    assertTrue(providers.isEmpty())
  }

  @Test
  fun `debugMode is false by default`() = runTest {
    // Given - fresh state
    unmockkAll() // Remove mocks to test actual behavior

    // When
    val debugMode = Clerk.debugMode

    // Then
    assertFalse(debugMode)
  }
}
