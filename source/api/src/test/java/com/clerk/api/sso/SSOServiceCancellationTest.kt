package com.clerk.api.sso

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signup.SignUp
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SSOServiceCancellationTest {

  @Before
  fun setUp() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    mockkObject(Clerk)
    every { Clerk.applicationContext } returns WeakReference(application)
  }

  @After
  fun tearDown() {
    SSOService.cancelPendingAuthentication()
    unmockkAll()
  }

  @Test
  fun `cancelPendingAuthentication returns typed cancellation failure`() = runTest {
    val pendingResult =
      async(start = CoroutineStart.UNDISPATCHED) {
        SSOService.authenticateWithPreparedRedirect(AUTHORIZATION_URL)
      }

    SSOService.cancelPendingAuthentication()

    val failure = pendingResult.await() as ClerkResult.Failure
    assertTrue(failure.throwable is SSOCancellationException)
    assertFalse(SSOService.hasPendingAuthentication())
  }

  @Test
  fun `callback without success marker returns cancellation instead of starting sign up`() =
    runTest {
      mockkObject(SignUp.Companion)
      val pendingResult =
        async(start = CoroutineStart.UNDISPATCHED) {
          SSOService.authenticateWithPreparedRedirect(AUTHORIZATION_URL)
        }

      SSOService.completeAuthenticateWithRedirect(Uri.parse(CALLBACK_URL))

      val failure = pendingResult.await() as ClerkResult.Failure
      assertTrue(failure.throwable is SSOCancellationException)
      coVerify(exactly = 0) { SignUp.create(any<SignUp.CreateParams>()) }
    }

  @Test
  fun `provider error callback returns cancellation instead of starting sign up`() = runTest {
    mockkObject(SignUp.Companion)
    val pendingResult =
      async(start = CoroutineStart.UNDISPATCHED) {
        SSOService.authenticateWithPreparedRedirect(AUTHORIZATION_URL)
      }

    SSOService.completeAuthenticateWithRedirect(
      Uri.parse("$CALLBACK_URL?error=access_denied&error_description=User%20cancelled")
    )

    val failure = pendingResult.await() as ClerkResult.Failure
    assertTrue(failure.throwable is SSOCancellationException)
    coVerify(exactly = 0) { SignUp.create(any<SignUp.CreateParams>()) }
  }

  @Test
  fun `explicit external account not found marker still transfers to sign up`() = runTest {
    val signUp = mockk<SignUp>(relaxed = true)
    mockkObject(SignUp.Companion)
    coEvery { SignUp.create(SignUp.CreateParams.Transfer) } returns ClerkResult.success(signUp)
    val pendingResult =
      async(start = CoroutineStart.UNDISPATCHED) {
        SSOService.authenticateWithPreparedRedirect(AUTHORIZATION_URL)
      }

    SSOService.completeAuthenticateWithRedirect(
      Uri.parse("$CALLBACK_URL?__clerk_status=failed&__clerk_error_code=external_account_not_found")
    )

    val result = pendingResult.await() as ClerkResult.Success
    assertSame(signUp, result.value.signUp)
    coVerify(exactly = 1) { SignUp.create(SignUp.CreateParams.Transfer) }
  }

  private companion object {
    const val AUTHORIZATION_URL = "https://accounts.example.com/oauth/authorize"
    const val CALLBACK_URL = "clerk://com.example.app.callback"
  }
}
