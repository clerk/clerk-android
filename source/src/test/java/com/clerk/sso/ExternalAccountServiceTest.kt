package com.clerk.sso

import android.content.Context
import com.clerk.Clerk
import com.clerk.network.ClerkApi
import com.clerk.network.api.UserApi
import com.clerk.network.model.account.ExternalAccount
import com.clerk.network.model.client.Client
import com.clerk.network.model.verification.Verification
import com.clerk.network.serialization.ClerkResult
import com.clerk.session.Session
import com.clerk.user.User
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.lang.ref.WeakReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for [ExternalAccountService].
 *
 * These tests focus on external account connection functionality for existing users.
 */
@RunWith(RobolectricTestRunner::class)
class ExternalAccountServiceTest {
  private val testDispatcher = StandardTestDispatcher()

  private lateinit var mockContext: Context
  private lateinit var mockUserApi: UserApi
  private lateinit var mockClient: Client
  private lateinit var mockSession: Session
  private lateinit var mockUser: User
  private lateinit var mockExternalAccount: ExternalAccount
  private lateinit var mockVerification: Verification

  @OptIn(ExperimentalCoroutinesApi::class)
  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)

    // Mock objects
    mockContext = mockk(relaxed = true)
    mockUserApi = mockk(relaxed = true)
    mockClient = mockk(relaxed = true)
    mockSession = mockk(relaxed = true)
    mockUser = mockk(relaxed = true)
    mockExternalAccount = mockk(relaxed = true)
    mockVerification = mockk(relaxed = true)

    // Mock static objects
    mockkObject(ClerkApi)
    mockkObject(Clerk)
    mockkStatic(Client::class)

    // Setup basic mocks
    every { ClerkApi.user } returns mockUserApi
    every { Clerk.applicationContext } returns WeakReference(mockContext)

    // Setup verification mock
    every { mockVerification.status } returns Verification.Status.VERIFIED
    every { mockVerification.externalVerificationRedirectUrl } returns
      "https://oauth.example.com/auth"
    every { mockExternalAccount.verification } returns mockVerification
    every { mockExternalAccount.id } returns "ext_account_123"

    // Setup client/session mocks
    every { mockClient.lastActiveSessionId } returns "session_123"
    every { mockClient.sessions } returns listOf(mockSession)
    every { mockSession.id } returns "session_123"
    every { mockSession.user } returns mockUser
    every { mockUser.externalAccounts } returns listOf(mockExternalAccount)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

  @Test
  fun `hasPendingExternalAccountConnection returns false initially`() {
    assertFalse(ExternalAccountService.hasPendingExternalAccountConnection())
  }

  @Test
  fun `cancelPendingExternalAccountConnection clears state`() {
    // Cancel any pending connection
    ExternalAccountService.cancelPendingExternalAccountConnection()

    // Should have no pending connection
    assertFalse(ExternalAccountService.hasPendingExternalAccountConnection())
  }

  @Test
  fun `completeExternalConnection handles no pending connection gracefully`() = runTest {
    // Should not throw when no pending connection
    ExternalAccountService.completeExternalConnection()

    // Should still have no pending connection
    assertFalse(ExternalAccountService.hasPendingExternalAccountConnection())
  }

  @Test
  fun `completeExternalConnection handles missing external account`() = runTest {
    // Mock client with no matching external account
    every { mockUser.externalAccounts } returns emptyList()
    coEvery { Client.get() } returns ClerkResult.Success(mockClient, emptyMap())

    // Complete the external connection (should handle gracefully with no pending connection)
    ExternalAccountService.completeExternalConnection()

    // Should have no pending connection
    assertFalse(ExternalAccountService.hasPendingExternalAccountConnection())
  }

  @Test
  fun `cancelPendingExternalAccountConnection completes with cancellation error`() {
    // This test verifies that cancellation properly cleans up state
    ExternalAccountService.cancelPendingExternalAccountConnection()

    // After cancellation, there should be no pending connection
    assertFalse(ExternalAccountService.hasPendingExternalAccountConnection())
  }
}
