package com.clerk.api.auth

import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signin.SignIn
import com.clerk.api.signup.SignUp
import com.clerk.api.sso.OAuthResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthTest {

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `signUpWithGoogleOneTap delegates to transferable Google One Tap flow`() = runTest {
    mockkObject(SignIn.Companion)
    val signUp = mockk<SignUp>(relaxed = true)
    val oauthResult = OAuthResult(signUp = signUp)
    coEvery { SignIn.authenticateWithGoogleOneTap(true) } returns ClerkResult.success(oauthResult)

    val result = Auth().signUpWithGoogleOneTap()

    assertTrue(result is ClerkResult.Success)
    assertSame(oauthResult, (result as ClerkResult.Success).value)
    coVerify(exactly = 1) { SignIn.authenticateWithGoogleOneTap(true) }
  }

  @Test
  fun `signUpWithGoogleOneTap emits auth error event on failure`() = runTest {
    mockkObject(SignIn.Companion)
    val error =
      ClerkErrorResponse(
        errors =
          listOf(
            Error(
              code = "external_account_exists",
              message = "Account already exists",
              longMessage = "Account already exists. Use sign in instead.",
            )
          ),
        clerkTraceId = "trace_123",
      )
    coEvery { SignIn.authenticateWithGoogleOneTap(true) } returns ClerkResult.apiFailure(error)

    val auth = Auth()
    val events = mutableListOf<AuthEvent>()
    val eventJob =
      launch(start = CoroutineStart.UNDISPATCHED) { auth.events.take(1).toList(events) }

    val result = auth.signUpWithGoogleOneTap()

    withTimeout(1_000) { eventJob.join() }

    assertTrue(result is ClerkResult.Failure)
    assertEquals(error, (result as ClerkResult.Failure).error)
    assertTrue(events.single() is AuthEvent.Error)
    assertEquals(
      "Account already exists. Use sign in instead.",
      (events.single() as AuthEvent.Error).message,
    )
    coVerify(exactly = 1) { SignIn.authenticateWithGoogleOneTap(true) }
  }
}
