// These tests pin the deprecated entry points until they are removed in the next major.
@file:Suppress("DEPRECATION")

package com.clerk.api.auth

import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.SignInApi
import com.clerk.api.network.api.SignUpApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.restorecredentials.RestoreCredentials
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.attemptFirstFactor
import com.clerk.api.signin.verifyCode
import com.clerk.api.signup.SignUp
import com.clerk.api.sso.OAuthProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Both auth API layers report failures as [AuthEvent.Error], exactly once per failed call. */
class AuthErrorEventsTest {

  private val signInApi = mockk<SignInApi>()
  private val signUpApi = mockk<SignUpApi>()
  private val apiError =
    ClerkErrorResponse(
      errors =
        listOf(Error(code = "form_code_incorrect", message = "is incorrect", longMessage = MESSAGE))
    )

  @Before
  fun setup() {
    Clerk.updateClient(Client())
    mockkObject(ClerkApi)
    every { ClerkApi.signIn } returns signInApi
    every { ClerkApi.signUp } returns signUpApi
    coEvery { signInApi.createSignIn(any()) } returns ClerkResult.apiFailure(apiError)
    coEvery { signInApi.attemptFirstFactor(any(), any()) } returns ClerkResult.apiFailure(apiError)
    coEvery { signUpApi.createSignUp(any()) } returns ClerkResult.apiFailure(apiError)
  }

  @After
  fun tearDown() {
    unmockkAll()
    Clerk.updateClient(Client())
  }

  @Test
  fun `SignIn create failure emits an auth error event`() = runTest {
    val events = collect(Clerk.auth.events)

    val result = SignIn.create(SignIn.CreateParams.Strategy.Identifier(identifier = "a@b.com"))

    runCurrent()
    assertTrue(result is ClerkResult.Failure)
    assertSingleError(events)
    events.job.cancel()
  }

  @Test
  fun `SignUp create failure emits an auth error event`() = runTest {
    val events = collect(Clerk.auth.events)

    SignUp.create(SignUp.CreateParams.Standard(emailAddress = "a@b.com"))

    runCurrent()
    assertSingleError(events)
    events.job.cancel()
  }

  @Test
  fun `SignIn attemptFirstFactor failure emits an auth error event`() = runTest {
    val events = collect(Clerk.auth.events)

    SignIn(id = "sign_in_123").attemptFirstFactor(SignIn.AttemptFirstFactorParams.EmailCode("1"))

    runCurrent()
    assertSingleError(events)
    events.job.cancel()
  }

  @Test
  fun `SignIn verifyCode failure emits an auth error event`() = runTest {
    val events = collect(Clerk.auth.events)

    SignIn(id = "sign_in_123").verifyCode("123456")

    runCurrent()
    assertSingleError(events)
    events.job.cancel()
  }

  @Test
  fun `old layer redirect failure emits one event although it nests other entry points`() =
    runTest {
      val events = collect(Clerk.auth.events)

      // SSOService creates the sign-in through SignIn.create, itself a reporting entry point.
      SignIn.authenticateWithRedirect(
        SignIn.AuthenticateWithRedirectParams.OAuth(provider = OAuthProvider.GOOGLE)
      )

      runCurrent()
      assertSingleError(events)
      events.job.cancel()
    }

  @Test
  fun `facade failure is reported once, by the facade, when it nests old layer calls`() = runTest {
    val auth = Auth()
    val facadeEvents = collect(auth.events)
    val sharedEvents = collect(Clerk.auth.events)

    auth.signInWithOAuth(OAuthProvider.GOOGLE)

    runCurrent()
    assertSingleError(facadeEvents)
    assertTrue(sharedEvents.isEmpty())
    facadeEvents.job.cancel()
    sharedEvents.job.cancel()
  }

  @Test
  fun `RestoreCredentials signIn failure emits one auth error event when called directly`() =
    runTest {
      val events = collect(Clerk.auth.events)

      val result = RestoreCredentials.signIn()

      runCurrent()
      assertTrue(result is ClerkResult.Failure)
      assertEquals(1, events.size)
      events.job.cancel()
    }

  private fun assertSingleError(events: List<AuthEvent>) {
    assertEquals(1, events.size)
    assertEquals(MESSAGE, (events.single() as AuthEvent.Error).message)
  }

  private fun TestScope.collect(flow: Flow<AuthEvent>): CollectedEvents {
    val events = CollectedEvents()
    events.job =
      launch(start = CoroutineStart.UNDISPATCHED) {
        flow.collect { if (it is AuthEvent.Error) events.add(it) }
      }
    return events
  }

  private class CollectedEvents : ArrayList<AuthEvent>() {
    lateinit var job: Job
  }

  private companion object {
    const val MESSAGE = "The code is incorrect."
  }
}
