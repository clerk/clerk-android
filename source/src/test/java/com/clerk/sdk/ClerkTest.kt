package com.clerk.sdk

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.clerk.Clerk
import com.clerk.configuration.ClerkConfigurationState
import com.clerk.configuration.ConfigurationManager
import com.clerk.model.client.Client
import com.clerk.model.environment.Environment
import com.clerk.model.session.Session
import com.clerk.model.user.User
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
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
  private lateinit var mockLifecycleOwner: LifecycleOwner
  private lateinit var spyConfigManager: ConfigurationManager

  private val configCallbackSlot = slot<(ClerkConfigurationState) -> Unit>()

  @OptIn(ExperimentalCoroutinesApi::class)
  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    // Create mocks using MockK
    mockContext = mockk(relaxed = true)
    mockClient = mockk(relaxed = true)
    mockEnvironment = mockk(relaxed = true)
    mockSession = mockk(relaxed = true)
    mockUser = mockk(relaxed = true)
    mockLifecycleOwner = mockk(relaxed = true)

    // Mock the Clerk object but keep real behavior
    mockkObject(Clerk)

    // Instead of replacing the configurationManager, we'll mock the object and spy on its methods
    spyConfigManager = spyk(Clerk.configurationManager)

    // Configure mock responses for the config manager
    every { spyConfigManager.configure(any(), any(), capture(configCallbackSlot)) } answers
      {
        // Do nothing, we'll manually invoke the callback later
      }

    // Mock the Clerk's configurationManager getter to return our spy
    every { Clerk.configurationManager } returns spyConfigManager
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll() // Reset all mocks
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
  fun `user returns null when no active session exists`() = runTest {
    // Given
    every { Clerk.client } returns mockClient
    every { mockClient.lastActiveSessionId } returns "session_id"
    every { mockClient.sessions } returns emptyList()

    // When
    val user = Clerk.user

    // Then
    assertNull(user)
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
  fun `isInitialized returns false before initialization`() = runTest {
    // Given - environment field is not initialized by default

    // When
    val initialized = Clerk.isInitialized.value

    // Then
    assertTrue(!initialized)
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
}
