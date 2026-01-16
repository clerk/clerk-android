package com.clerk.api.sso

import android.content.Context
import android.net.Uri
import com.clerk.api.Clerk
import com.clerk.api.externalaccount.ExternalAccount
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.SignInApi
import com.clerk.api.network.api.UserApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.Session
import com.clerk.api.signin.SignIn
import com.clerk.api.signup.SignUp
import com.clerk.api.user.User
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for [com.clerk.sso.sso.SSOService].
 *
 * These tests focus on OAuth authentication flows and redirect handling. Tests for external account
 * connection functionality can be found in [ExternalAccountServiceTest].
 */
@Ignore("TODO: Fix these tests")
@RunWith(RobolectricTestRunner::class)
class SSOServiceTest {
  private val testDispatcher = StandardTestDispatcher()

  private lateinit var mockContext: Context
  private lateinit var mockSignInApi: SignInApi
  private lateinit var mockUserApi: UserApi
  private lateinit var mockSignIn: SignIn
  private lateinit var mockSignUp: SignUp
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
    mockSignInApi = mockk(relaxed = true)
    mockUserApi = mockk(relaxed = true)
    mockSignIn = mockk(relaxed = true)
    mockSignUp = mockk(relaxed = true)
    mockClient = mockk(relaxed = true)
    mockSession = mockk(relaxed = true)
    mockUser = mockk(relaxed = true)
    mockExternalAccount = mockk(relaxed = true)
    mockVerification = mockk(relaxed = true)

    // Mock static objects
    mockkObject(ClerkApi)
    mockkObject(Clerk)
    mockkStatic(Client::class)
    mockkStatic(SignUp::class)

    // Setup basic mocks
    every { ClerkApi.signIn } returns mockSignInApi
    every { ClerkApi.user } returns mockUserApi
    every { Clerk.applicationContext } returns WeakReference(mockContext)
    every { Clerk.signIn } returns mockSignIn

    // Setup verification mock
    every { mockVerification.status } returns Verification.Status.VERIFIED
    every { mockVerification.externalVerificationRedirectUrl } returns
      "https://oauth.example.com/auth"
    every { mockExternalAccount.verification } returns mockVerification
    every { mockExternalAccount.id } returns "ext_account_123"

    // Setup sign-in mock
    every { mockSignIn.firstFactorVerification } returns mockVerification

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
  fun `hasPendingAuthentication returns false initially`() {
    assertFalse(SSOService.hasPendingAuthentication())
  }

  @Test
  fun `hasPendingExternalAccountConnection returns false initially`() {
    assertFalse(SSOService.hasPendingExternalAccountConnection())
  }

  @Test
  fun `cancelPendingAuthentication clears state`() {
    // Cancel any pending auth
    SSOService.cancelPendingAuthentication()

    // Should have no pending authentication
    assertFalse(SSOService.hasPendingAuthentication())
  }

  @Test
  fun `authenticateWithRedirect returns failure when API call fails`() = runTest {
    val errorResponse = mockk<ClerkErrorResponse>(relaxed = true)
    val strategy = "oauth_google"
    val redirectUrl = "https://example.com/callback"

    coEvery { mockSignInApi.authenticateWithRedirect(strategy, redirectUrl) } returns
      ClerkResult.Failure(errorResponse)

    val result = SSOService.authenticateWithRedirect(strategy, redirectUrl)

    assertTrue(result is ClerkResult.Failure)
    assertEquals(errorResponse, (result as ClerkResult.Failure).error)
  }

  @Test
  fun `completeAuthenticateWithRedirect handles no pending authentication gracefully`() = runTest {
    val mockUri = mockk<Uri>(relaxed = true)
    every { mockUri.getQueryParameter("rotating_token_nonce") } returns "test_nonce"

    // Should not throw when no pending authentication
    SSOService.completeAuthenticateWithRedirect(mockUri)

    // Should still have no pending authentication
    assertFalse(SSOService.hasPendingAuthentication())
  }

  @Test
  fun `completeExternalConnection handles no pending connection gracefully`() = runTest {
    // Should not throw when no pending connection
    SSOService.completeExternalConnection()

    // Should still have no pending connection
    assertFalse(SSOService.hasPendingExternalAccountConnection())
  }

  @Test
  fun `completeExternalConnection handles missing external account`() = runTest {
    // Mock client with no matching external account
    every { mockUser.externalAccounts } returns emptyList()
    coEvery { Client.get() } returns ClerkResult.Success(mockClient, emptyMap())

    // Complete the external connection (should handle gracefully with no pending connection)
    SSOService.completeExternalConnection()

    // Should have no pending connection
    assertFalse(SSOService.hasPendingExternalAccountConnection())
  }
}
