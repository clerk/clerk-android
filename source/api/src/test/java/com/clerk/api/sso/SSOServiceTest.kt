package com.clerk.api.sso

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.clerk.api.Clerk
import com.clerk.api.auth.Auth
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.SignInApi
import com.clerk.api.network.api.SignUpApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signin.SignIn
import com.clerk.api.signup.SignUp
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SSOServiceTest {
  private val signInApi = mockk<SignInApi>()
  private val signUpApi = mockk<SignUpApi>()
  private val auth = mockk<Auth>()
  private lateinit var context: Context

  @Before
  fun setup() {
    SSOService.cancelPendingAuthentication()
    context = mockk(relaxed = true)
    mockkObject(Clerk)
    mockkObject(ClerkApi)
    every { Clerk.applicationContext } returns WeakReference(context)
    every { Clerk.auth } returns auth
    justRun { auth.emitAuthError(any()) }
    every { ClerkApi.signIn } returns signInApi
    every { ClerkApi.signUp } returns signUpApi
  }

  @After
  fun tearDown() {
    SSOService.cancelPendingAuthentication()
    unmockkAll()
  }

  @Test
  fun `authenticateWithRedirect fails without creating a sign in when strategy is null`() =
    runTest {
      val result = SSOService.authenticateWithRedirect(strategy = null, redirectUrl = REDIRECT_URL)

      val failure = result as ClerkResult.Failure
      assertEquals(ClerkResult.Failure.ErrorType.UNKNOWN, failure.errorType)
      assertEquals(
        "Strategy cannot be null for redirect authentication",
        failure.throwable?.message,
      )
      coVerify(exactly = 0) { signInApi.createSignIn(any()) }
      verify(exactly = 0) { context.startActivity(any()) }
      assertFalse(SSOService.hasPendingAuthentication())
    }

  @Test
  fun `authenticateWithRedirect returns sign in creation failure without launching the browser`() =
    runTest {
      val errorResponse = apiError("form_param_invalid")
      coEvery { signInApi.createSignIn(any()) } returns ClerkResult.apiFailure(errorResponse)

      val result =
        SSOService.authenticateWithRedirect(strategy = "oauth_google", redirectUrl = REDIRECT_URL)

      val failure = result as ClerkResult.Failure
      assertEquals(ClerkResult.Failure.ErrorType.API, failure.errorType)
      assertSame(errorResponse, failure.error)
      verify(exactly = 1) { auth.emitAuthError(match { it.error === errorResponse }) }
      coVerify(exactly = 0) { signInApi.prepareSignInFirstFactor(any(), any()) }
      verify(exactly = 0) { context.startActivity(any()) }
      assertFalse(SSOService.hasPendingAuthentication())
    }

  @Test
  fun `authenticateWithRedirect returns prepare failure without launching the browser`() = runTest {
    val errorResponse = apiError("strategy_for_user_invalid")
    coEvery { signInApi.createSignIn(any()) } returns ClerkResult.success(createdSignIn())
    coEvery { signInApi.prepareSignInFirstFactor(SIGN_IN_ID, any()) } returns
      ClerkResult.apiFailure(errorResponse)

    val result =
      SSOService.authenticateWithRedirect(strategy = "oauth_google", redirectUrl = REDIRECT_URL)

    val failure = result as ClerkResult.Failure
    assertEquals(ClerkResult.Failure.ErrorType.API, failure.errorType)
    assertSame(errorResponse, failure.error)
    verify(exactly = 1) { auth.emitAuthError(match { it.error === errorResponse }) }
    verify(exactly = 0) { context.startActivity(any()) }
    assertFalse(SSOService.hasPendingAuthentication())
  }

  @Test
  fun `nonce callback completes pending sign in with the refreshed sign in`() = runTest {
    val currentSignIn = createdSignIn()
    val completedSignIn = SignIn(id = SIGN_IN_ID, status = SignIn.Status.COMPLETE)
    every { auth.currentSignIn } returns currentSignIn
    coEvery { signInApi.fetchSignIn(SIGN_IN_ID, NONCE) } returns
      ClerkResult.success(completedSignIn)
    val startedIntent = slot<Intent>()

    val pendingResult =
      async(start = CoroutineStart.UNDISPATCHED) {
        SSOService.authenticateWithPreparedRedirect(AUTHORIZATION_URL)
      }
    verify(exactly = 1) { context.startActivity(capture(startedIntent)) }
    assertEquals(
      AUTHORIZATION_URL,
      startedIntent.captured.getStringExtra(SSOManagerActivity.URI_KEY),
    )
    assertTrue(SSOService.hasPendingAuthentication())

    SSOService.completeAuthenticateWithRedirect(
      Uri.parse("$CALLBACK_URL?rotating_token_nonce=$NONCE")
    )

    val result = (pendingResult.await() as ClerkResult.Success).value
    assertSame(completedSignIn, result.signIn)
    assertNull(result.signUp)
    assertFalse(SSOService.hasPendingAuthentication())
    coVerify(exactly = 1) { signInApi.fetchSignIn(SIGN_IN_ID, NONCE) }
    coVerify(exactly = 0) { signUpApi.createSignUp(any()) }
  }

  @Test
  fun `callback without pending authentication makes no requests`() = runTest {
    every { auth.currentSignIn } returns createdSignIn()

    SSOService.completeAuthenticateWithRedirect(
      Uri.parse("$CALLBACK_URL?rotating_token_nonce=$NONCE")
    )

    assertFalse(SSOService.hasPendingAuthentication())
    verify(exactly = 0) { auth.currentSignIn }
    coVerify(exactly = 0) { signInApi.fetchSignIn(any(), any()) }
  }

  @Test
  fun `sign up redirect launches external verification url and completes with nonce`() = runTest {
    val createParams = slot<Map<String, String>>()
    val createdSignUp =
      testSignUp(
        verifications =
          mapOf(
            "external_account" to Verification(externalVerificationRedirectUrl = AUTHORIZATION_URL)
          )
      )
    val completedSignUp = testSignUp(status = SignUp.Status.COMPLETE)
    coEvery { signUpApi.createSignUp(capture(createParams)) } returns
      ClerkResult.success(createdSignUp)
    coEvery { signUpApi.fetchSignUp(SIGN_UP_ID, NONCE) } returns
      ClerkResult.success(completedSignUp)
    val startedIntent = slot<Intent>()

    val pendingResult =
      async(start = CoroutineStart.UNDISPATCHED) {
        SSOService.authenticateSignUpWithRedirect(
          strategy = "oauth_google",
          redirectUrl = REDIRECT_URL,
        )
      }
    verify(exactly = 1) { context.startActivity(capture(startedIntent)) }
    assertEquals(
      AUTHORIZATION_URL,
      startedIntent.captured.getStringExtra(SSOManagerActivity.URI_KEY),
    )
    assertEquals("oauth_google", createParams.captured["strategy"])
    assertEquals(REDIRECT_URL, createParams.captured["redirect_url"])

    SSOService.completeAuthenticateWithRedirect(
      Uri.parse("$CALLBACK_URL?rotating_token_nonce=$NONCE")
    )

    val result = (pendingResult.await() as ClerkResult.Success).value
    assertSame(completedSignUp, result.signUp)
    assertNull(result.signIn)
    assertFalse(SSOService.hasPendingAuthentication())
    coVerify(exactly = 0) { signInApi.fetchSignIn(any(), any()) }
  }

  @Test
  fun `sign up redirect transfers to sign in when external account already exists`() = runTest {
    val createdSignUp =
      testSignUp(
        verifications =
          mapOf(
            "external_account" to Verification(externalVerificationRedirectUrl = AUTHORIZATION_URL)
          )
      )
    val transferableSignUp =
      testSignUp(
        verifications =
          mapOf("external_account" to Verification(status = Verification.Status.TRANSFERABLE))
      )
    val transferredSignIn = SignIn(id = SIGN_IN_ID, status = SignIn.Status.COMPLETE)
    val transferParams = slot<Map<String, String>>()
    coEvery { signUpApi.createSignUp(any()) } returns ClerkResult.success(createdSignUp)
    coEvery { signUpApi.fetchSignUp(SIGN_UP_ID, null) } returns
      ClerkResult.success(transferableSignUp)
    coEvery { signInApi.createSignIn(capture(transferParams)) } returns
      ClerkResult.success(transferredSignIn)

    val pendingResult =
      async(start = CoroutineStart.UNDISPATCHED) {
        SSOService.authenticateSignUpWithRedirect(
          strategy = "oauth_google",
          redirectUrl = REDIRECT_URL,
        )
      }

    SSOService.completeAuthenticateWithRedirect(
      Uri.parse("$CALLBACK_URL?__clerk_status=failed&__clerk_error_code=external_account_exists")
    )

    val result = (pendingResult.await() as ClerkResult.Success).value
    assertSame(transferredSignIn, result.signIn)
    assertNull(result.signUp)
    assertEquals("true", transferParams.captured["transfer"])
    assertFalse(SSOService.hasPendingAuthentication())
  }

  private fun createdSignIn() =
    SignIn(
      id = SIGN_IN_ID,
      status = SignIn.Status.NEEDS_FIRST_FACTOR,
      firstFactorVerification = Verification(externalVerificationRedirectUrl = AUTHORIZATION_URL),
    )

  private fun testSignUp(
    status: SignUp.Status = SignUp.Status.MISSING_REQUIREMENTS,
    verifications: Map<String, Verification?> = emptyMap(),
  ) =
    SignUp(
      id = SIGN_UP_ID,
      status = status,
      requiredFields = emptyList(),
      optionalFields = emptyList(),
      missingFields = emptyList(),
      unverifiedFields = emptyList(),
      verifications = verifications,
      passwordEnabled = false,
    )

  private fun apiError(code: String) =
    ClerkErrorResponse(errors = listOf(Error(code = code, longMessage = "$code message")))

  private companion object {
    const val SIGN_IN_ID = "sign_in_123"
    const val SIGN_UP_ID = "sign_up_123"
    const val NONCE = "nonce_123"
    const val REDIRECT_URL = "clerk://com.example.app.callback"
    const val CALLBACK_URL = REDIRECT_URL
    const val AUTHORIZATION_URL = "https://accounts.example.com/oauth/authorize"
  }
}
