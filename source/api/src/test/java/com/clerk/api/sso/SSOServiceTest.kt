package com.clerk.api.sso

import android.content.Context
import com.clerk.api.Clerk
import com.clerk.api.auth.Auth
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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
  private lateinit var mockAuth: Auth

  @OptIn(ExperimentalCoroutinesApi::class)
  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)

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
    mockAuth = mockk(relaxed = true)

    mockkObject(ClerkApi)
    mockkObject(Clerk)
    mockkStatic(Client::class)
    mockkStatic(SignUp::class)

    every { ClerkApi.signIn } returns mockSignInApi
    every { ClerkApi.user } returns mockUserApi
    every { Clerk.applicationContext } returns WeakReference(mockContext)
    every { Clerk.auth } returns mockAuth
    every { Clerk.debugMode } returns false
    every { mockAuth.currentSignIn } returns mockSignIn

    every { mockVerification.status } returns Verification.Status.VERIFIED
    every { mockVerification.externalVerificationRedirectUrl } returns
      "https://oauth.example.com/auth"
    every { mockExternalAccount.verification } returns mockVerification
    every { mockExternalAccount.id } returns "ext_account_123"

    every { mockSignIn.firstFactorVerification } returns mockVerification

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
}
