package com.clerk.api.redirect

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.clerk.api.Clerk
import com.clerk.api.auth.AuthEvent
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.ClientApi
import com.clerk.api.network.api.SignInApi
import com.clerk.api.network.api.SignUpApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.network.model.hostedauth.HostedAuthResource
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signin.SignIn
import com.clerk.api.sso.OAuthProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RedirectFlowErrorReportingTest {
  private val signInApi = mockk<SignInApi>()
  private val signUpApi = mockk<SignUpApi>()
  private val clientApi = mockk<ClientApi>()
  private val errors = CopyOnWriteArrayList<AuthEvent.Error>()

  @Before
  fun setUp() {
    mockkObject(Clerk)
    every { Clerk.applicationContext } returns
      WeakReference(ApplicationProvider.getApplicationContext<Application>())
    mockkObject(ClerkApi)
    every { ClerkApi.signIn } returns signInApi
    every { ClerkApi.signUp } returns signUpApi
    every { ClerkApi.client } returns clientApi
  }

  @After
  fun tearDown() {
    RedirectCoordinator.resetForTests()
    unmockkAll()
  }

  @Test
  fun `a failed OAuth flow reports exactly one error`() = runBlocking {
    val prepareParams = slot<Map<String, String>>()
    coEvery { signInApi.createSignIn(any()) } returns
      ClerkResult.success(
        SignIn(
          id = SIGN_IN_ID,
          status = SignIn.Status.NEEDS_FIRST_FACTOR,
          supportedFirstFactors = listOf(Factor(strategy = "oauth_google")),
        )
      )
    coEvery { signInApi.prepareSignInFirstFactor(SIGN_IN_ID, capture(prepareParams)) } returns
      ClerkResult.success(
        SignIn(
          id = SIGN_IN_ID,
          firstFactorVerification =
            Verification(externalVerificationRedirectUrl = EXTERNAL_VERIFICATION_URL),
        )
      )
    coEvery { signUpApi.createSignUp(any()) } returns
      ClerkResult.apiFailure(ClerkErrorResponse(errors = listOf(Error(code = "transfer_failed"))))
    val collector = collectErrors()

    val result =
      async(Dispatchers.Default) {
        Clerk.auth.signInWithOAuth(OAuthProvider.GOOGLE, redirectUrl = REDIRECT_URL)
      }
    waitUntil { RedirectCoordinator.current() is PendingRedirect.Sso }
    val state =
      Uri.parse(prepareParams.captured.getValue("redirect_url"))
        .getQueryParameter(RedirectState.QUERY_PARAMETER)
    RedirectCoordinator.dispatch(Uri.parse("$TRANSFER_CALLBACK&clerk_redirect_state=$state"))

    assertTrue(withTimeout(TIMEOUT_MS) { result.await() } is ClerkResult.Failure)
    assertExactlyOneError()
    collector.cancel()
  }

  @Test
  fun `a failed hosted auth flow reports exactly one error`() = runBlocking {
    val capturedState = CompletableDeferred<String>()
    coEvery {
      clientApi.createHostedAuth(any(), any(), any(), any(), any(), any(), any())
    } answers
      {
        capturedState.complete(thirdArg())
        ClerkResult.success(
          HostedAuthResource(objectType = "hosted_auth", url = "https://portal.dev/start")
        )
      }
    coEvery { clientApi.redeemHostedAuth(any(), any(), any(), any(), any()) } returns
      ClerkResult.apiFailure(ClerkErrorResponse(errors = listOf(Error(code = "redeem_failed"))))
    val collector = collectErrors()

    val result =
      async(Dispatchers.Default) { Clerk.auth.startHostedAuth(redirectUrl = REDIRECT_URL) }
    val state = withTimeout(TIMEOUT_MS) { capturedState.await() }
    waitUntil { RedirectCoordinator.current() is PendingRedirect.HostedAuth }
    RedirectCoordinator.dispatch(
      Uri.parse("$REDIRECT_URL?state=$state&rotating_token_nonce=n&created_session_id=sess_1")
    )

    assertTrue(withTimeout(TIMEOUT_MS) { result.await() } is ClerkResult.Failure)
    assertExactlyOneError()
    collector.cancel()
  }

  private fun kotlinx.coroutines.CoroutineScope.collectErrors() =
    launch(Dispatchers.Default, start = CoroutineStart.UNDISPATCHED) {
      Clerk.auth.events.collect { if (it is AuthEvent.Error) errors += it }
    }

  private suspend fun assertExactlyOneError() {
    waitUntil { errors.isNotEmpty() }
    delay(SETTLE_MS)
    assertEquals(1, errors.size)
  }

  private suspend fun waitUntil(condition: () -> Boolean) {
    withTimeout(TIMEOUT_MS) {
      while (!condition()) delay(POLL_MS)
    }
  }

  private companion object {
    const val SIGN_IN_ID = "sia_123"
    const val REDIRECT_URL = "clerk://com.example.app.callback"
    const val EXTERNAL_VERIFICATION_URL = "https://accounts.example.com/oauth/authorize"
    const val TRANSFER_CALLBACK =
      "$REDIRECT_URL?__clerk_status=failed&__clerk_error_code=external_account_not_found"
    const val TIMEOUT_MS = 5_000L
    const val POLL_MS = 5L
    const val SETTLE_MS = 200L
  }
}
