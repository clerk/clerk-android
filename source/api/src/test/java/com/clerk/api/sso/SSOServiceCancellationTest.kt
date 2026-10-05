package com.clerk.api.sso

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.clerk.api.Clerk
import com.clerk.api.auth.Auth
import com.clerk.api.auth.AuthEvent
import com.clerk.api.auth.createSignUp
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.SignUpApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.shortErrorMessageOrNull
import com.clerk.api.signup.SignUp
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.lang.ref.WeakReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SSOServiceCancellationTest {

  // Captured once so verifications count calls on Auth, not reads of the mocked Clerk.auth.
  private val auth: Auth = Clerk.auth

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
      mockkStatic("com.clerk.api.auth.AuthFlowsKt")
      val pendingResult =
        async(start = CoroutineStart.UNDISPATCHED) {
          SSOService.authenticateWithPreparedRedirect(AUTHORIZATION_URL)
        }

      SSOService.completeAuthenticateWithRedirect(Uri.parse(CALLBACK_URL))

      val failure = pendingResult.await() as ClerkResult.Failure
      assertTrue(failure.throwable is SSOCancellationException)
      coVerify(exactly = 0) { auth.createSignUp(any<SignUp.CreateParams>()) }
    }

  @Test
  fun `provider error callback returns cancellation instead of starting sign up`() = runTest {
    mockkStatic("com.clerk.api.auth.AuthFlowsKt")
    val pendingResult =
      async(start = CoroutineStart.UNDISPATCHED) {
        SSOService.authenticateWithPreparedRedirect(AUTHORIZATION_URL)
      }

    SSOService.completeAuthenticateWithRedirect(
      Uri.parse("$CALLBACK_URL?error=access_denied&error_description=User%20cancelled")
    )

    val failure = pendingResult.await() as ClerkResult.Failure
    assertTrue(failure.throwable is SSOCancellationException)
    coVerify(exactly = 0) { auth.createSignUp(any<SignUp.CreateParams>()) }
  }

  @Test
  fun `external account not found returns readable API error when transfer is disabled`() =
    runTest {
      mockkStatic("com.clerk.api.auth.AuthFlowsKt")
      val pendingResult =
        async(start = CoroutineStart.UNDISPATCHED) {
          SSOService.authenticateWithPreparedRedirect(AUTHORIZATION_URL, transferable = false)
        }

      SSOService.completeAuthenticateWithRedirect(
        Uri.parse(
          "$CALLBACK_URL?__clerk_status=failed&__clerk_error_code=external_account_not_found"
        )
      )

      val failure = pendingResult.await() as ClerkResult.Failure
      assertEquals(ClerkResult.Failure.ErrorType.API, failure.errorType)
      assertEquals("external_account_not_found", failure.error?.errors?.single()?.code)
      assertEquals("The External Account was not found.", failure.errorMessage)
      assertEquals("The External Account was not found.", failure.shortErrorMessageOrNull())
      assertFalse(SSOService.hasPendingAuthentication())
      coVerify(exactly = 0) { auth.createSignUp(any<SignUp.CreateParams>()) }
    }

  @Test
  fun `explicit external account not found marker still transfers to sign up`() = runTest {
    val signUp = mockk<SignUp>(relaxed = true)
    mockkStatic("com.clerk.api.auth.AuthFlowsKt")
    coEvery { auth.createSignUp(SignUp.CreateParams.Transfer) } returns ClerkResult.success(signUp)
    val pendingResult =
      async(start = CoroutineStart.UNDISPATCHED) {
        SSOService.authenticateWithPreparedRedirect(AUTHORIZATION_URL)
      }

    SSOService.completeAuthenticateWithRedirect(
      Uri.parse("$CALLBACK_URL?__clerk_status=failed&__clerk_error_code=external_account_not_found")
    )

    val result = pendingResult.await() as ClerkResult.Success
    assertSame(signUp, result.value.signUp)
    coVerify(exactly = 1) { auth.createSignUp(SignUp.CreateParams.Transfer) }
  }

  @Test
  fun `a failing callback leaves error reporting to the awaiting caller`() = runTest {
    val signUpApi = mockk<SignUpApi>()
    mockkObject(ClerkApi)
    every { ClerkApi.signUp } returns signUpApi
    coEvery { signUpApi.createSignUp(any()) } returns
      ClerkResult.apiFailure(ClerkErrorResponse(errors = listOf(Error(code = "transfer_failed"))))
    val errors = mutableListOf<AuthEvent.Error>()
    val collector =
      launch(start = CoroutineStart.UNDISPATCHED) {
        Clerk.auth.events.collect { if (it is AuthEvent.Error) errors += it }
      }
    val pendingResult =
      async(start = CoroutineStart.UNDISPATCHED) {
        SSOService.authenticateWithPreparedRedirect(AUTHORIZATION_URL)
      }

    SSOService.completeAuthenticateWithRedirect(
      Uri.parse("$CALLBACK_URL?__clerk_status=failed&__clerk_error_code=external_account_not_found")
    )

    assertTrue(pendingResult.await() is ClerkResult.Failure)
    runCurrent()
    assertTrue(errors.isEmpty())
    collector.cancel()
  }

  @Test
  fun `cancellation thrown while completing redirect propagates and fails pending auth as interrupted`() =
    runTest {
      mockkStatic("com.clerk.api.auth.AuthFlowsKt")
      coEvery { auth.createSignUp(SignUp.CreateParams.Transfer) } throws
        CancellationException("caller cancelled")
      val pendingResult =
        async(start = CoroutineStart.UNDISPATCHED) {
          SSOService.authenticateWithPreparedRedirect(AUTHORIZATION_URL)
        }

      val thrown = runCatching {
        SSOService.completeAuthenticateWithRedirect(Uri.parse(TRANSFER_CALLBACK_URL))
      }
        .exceptionOrNull()

      assertTrue(thrown is CancellationException)
      assertFalse(SSOService.hasPendingAuthentication())
      assertInterrupted(pendingResult.await())
    }

  @Test
  fun `cancelling the completing job mid-request propagates and fails pending auth as interrupted`() =
    runTest {
      mockkStatic("com.clerk.api.auth.AuthFlowsKt")
      val neverCompletes = CompletableDeferred<ClerkResult<SignUp, ClerkErrorResponse>>()
      coEvery { auth.createSignUp(SignUp.CreateParams.Transfer) } coAnswers
        {
          neverCompletes.await()
        }
      val pendingResult =
        async(start = CoroutineStart.UNDISPATCHED) {
          SSOService.authenticateWithPreparedRedirect(AUTHORIZATION_URL)
        }
      val completionJob = launch {
        SSOService.completeAuthenticateWithRedirect(Uri.parse(TRANSFER_CALLBACK_URL))
      }
      runCurrent()
      assertTrue(completionJob.isActive)
      assertFalse(pendingResult.isCompleted)

      completionJob.cancel()
      completionJob.join()

      assertTrue(completionJob.isCancelled)
      assertFalse(SSOService.hasPendingAuthentication())
      assertInterrupted(pendingResult.await())
    }

  @Test
  fun `stale completion does not clobber a newer redirect flow`() = runTest {
    mockkStatic("com.clerk.api.auth.AuthFlowsKt")
    val neverCompletes = CompletableDeferred<ClerkResult<SignUp, ClerkErrorResponse>>()
    coEvery { auth.createSignUp(SignUp.CreateParams.Transfer) } coAnswers
      {
        neverCompletes.await()
      }
    val firstResult =
      async(start = CoroutineStart.UNDISPATCHED) {
        SSOService.authenticateWithPreparedRedirect(AUTHORIZATION_URL)
      }
    val staleCompletion = launch {
      SSOService.completeAuthenticateWithRedirect(Uri.parse(TRANSFER_CALLBACK_URL))
    }
    runCurrent()

    val secondResult =
      async(start = CoroutineStart.UNDISPATCHED) {
        SSOService.authenticateWithPreparedRedirect(AUTHORIZATION_URL)
      }
    staleCompletion.cancel()
    staleCompletion.join()

    val firstFailure = firstResult.await() as ClerkResult.Failure
    assertTrue(firstFailure.throwable is SSOCancellationException)
    assertTrue(SSOService.hasPendingAuthentication())
    assertFalse(secondResult.isCompleted)

    SSOService.cancelPendingAuthentication()
    val secondFailure = secondResult.await() as ClerkResult.Failure
    assertTrue(secondFailure.throwable is SSOCancellationException)
  }

  private fun assertInterrupted(result: ClerkResult<OAuthResult, ClerkErrorResponse>) {
    val failure = result as ClerkResult.Failure
    assertEquals(ClerkResult.Failure.ErrorType.UNKNOWN, failure.errorType)
    assertFalse(failure.throwable is SSOCancellationException)
    assertFalse(failure.throwable is CancellationException)
    assertTrue(failure.throwable?.cause is CancellationException)
  }

  private companion object {
    const val AUTHORIZATION_URL = "https://accounts.example.com/oauth/authorize"
    const val CALLBACK_URL = "clerk://com.example.app.callback"
    const val TRANSFER_CALLBACK_URL =
      "$CALLBACK_URL?__clerk_status=failed&__clerk_error_code=external_account_not_found"
  }
}
