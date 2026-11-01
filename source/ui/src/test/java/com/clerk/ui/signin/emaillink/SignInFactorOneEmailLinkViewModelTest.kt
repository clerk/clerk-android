package com.clerk.ui.signin.emaillink

import com.clerk.api.Clerk
import com.clerk.api.auth.Auth
import com.clerk.api.auth.AuthEvent
import com.clerk.api.magiclink.NativeMagicLinkError
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signin.SignIn
import com.clerk.ui.auth.AuthenticationViewState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SignInFactorOneEmailLinkViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private val auth = mockk<Auth>(relaxed = true)
  private val events = MutableSharedFlow<AuthEvent>(extraBufferCapacity = 1)

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    mockkObject(Clerk)
    every { Clerk.auth } returns auth
    every { auth.events } returns events
    every { auth.currentSignIn } returns null
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

  @Test
  fun `sendLink surfaces backend long message for rate limits`() = runTest {
    every { auth.currentSignIn } returns SignIn(id = "sia_123", identifier = "sam@clerk.dev")
    coEvery { auth.startEmailLinkSignIn("sam@clerk.dev", "idn_123") } returns
      ClerkResult.apiFailure(
        NativeMagicLinkError(
          reasonCode = "too_many_requests",
          message = "Too many requests, retry later",
        )
      )

    val viewModel = SignInFactorOneEmailLinkViewModel(ioDispatcher = testDispatcher)
    advanceUntilIdle()
    viewModel.sendLink("idn_123")
    advanceUntilIdle()

    assertEquals(
      AuthenticationViewState.Error("Too many requests, retry later"),
      viewModel.state.value,
    )
  }

  @Test
  fun `sendLink sends the link to the given email address`() = runTest {
    every { auth.currentSignIn } returns SignIn(id = "sia_123", identifier = "sam@clerk.dev")
    coEvery { auth.startEmailLinkSignIn(any(), any()) } returns
      ClerkResult.apiFailure(NativeMagicLinkError(reasonCode = "email_link_not_supported"))
    coEvery { auth.startEmailLinkSignIn("sam@clerk.dev", "idn_second") } returns
      ClerkResult.success(SignIn(id = "sia_123"))

    val viewModel = SignInFactorOneEmailLinkViewModel(ioDispatcher = testDispatcher)
    advanceUntilIdle()
    viewModel.sendLink("idn_second")
    advanceUntilIdle()

    assertEquals(AuthenticationViewState.Idle, viewModel.state.value)
    coVerify(exactly = 1) { auth.startEmailLinkSignIn("sam@clerk.dev", "idn_second") }
  }

  @Test
  fun `sendInitialLink sends one link however often the screen is rebuilt`() = runTest {
    every { auth.currentSignIn } returns SignIn(id = "sia_123", identifier = "sam@clerk.dev")
    coEvery { auth.startEmailLinkSignIn("sam@clerk.dev", "idn_123") } returns
      ClerkResult.success(SignIn(id = "sia_123"))

    val viewModel = SignInFactorOneEmailLinkViewModel(ioDispatcher = testDispatcher)
    advanceUntilIdle()
    viewModel.sendInitialLink("idn_123")
    advanceUntilIdle()
    viewModel.sendInitialLink("idn_123")
    advanceUntilIdle()

    coVerify(exactly = 1) { auth.startEmailLinkSignIn("sam@clerk.dev", "idn_123") }
  }

  @Test
  fun `sendLink resends after the initial link`() = runTest {
    every { auth.currentSignIn } returns SignIn(id = "sia_123", identifier = "sam@clerk.dev")
    coEvery { auth.startEmailLinkSignIn("sam@clerk.dev", "idn_123") } returns
      ClerkResult.success(SignIn(id = "sia_123"))

    val viewModel = SignInFactorOneEmailLinkViewModel(ioDispatcher = testDispatcher)
    advanceUntilIdle()
    viewModel.sendInitialLink("idn_123")
    advanceUntilIdle()
    viewModel.sendLink("idn_123")
    advanceUntilIdle()

    coVerify(exactly = 2) { auth.startEmailLinkSignIn("sam@clerk.dev", "idn_123") }
  }

  @Test
  fun `mfa sign in started event advances email link flow to second factor`() = runTest {
    val mfaSignIn =
      SignIn(
        id = "sia_mfa",
        status = SignIn.Status.NEEDS_SECOND_FACTOR,
        identifier = "sam@clerk.dev",
      )

    val viewModel = SignInFactorOneEmailLinkViewModel(ioDispatcher = testDispatcher)
    advanceUntilIdle()

    events.tryEmit(AuthEvent.SignInStarted(mfaSignIn))
    advanceUntilIdle()

    assertEquals(AuthenticationViewState.Success.SignIn(mfaSignIn), viewModel.state.value)
  }

  @Test
  fun `resume sync advances email link flow to second factor from current sign in`() = runTest {
    val mfaSignIn =
      SignIn(
        id = "sia_mfa_resume",
        status = SignIn.Status.NEEDS_SECOND_FACTOR,
        identifier = "sam@clerk.dev",
      )
    every { auth.currentSignIn } returns mfaSignIn

    val viewModel = SignInFactorOneEmailLinkViewModel(ioDispatcher = testDispatcher)
    advanceUntilIdle()

    viewModel.onHostResumed()

    assertEquals(AuthenticationViewState.Success.SignIn(mfaSignIn), viewModel.state.value)
  }

  @Test
  fun `first factor sign in started event does not advance email link flow`() = runTest {
    val firstFactorSignIn =
      SignIn(
        id = "sia_first_factor",
        status = SignIn.Status.NEEDS_FIRST_FACTOR,
        identifier = "sam@clerk.dev",
      )

    val viewModel = SignInFactorOneEmailLinkViewModel(ioDispatcher = testDispatcher)
    advanceUntilIdle()

    events.tryEmit(AuthEvent.SignInStarted(firstFactorSignIn))
    advanceUntilIdle()

    assertEquals(AuthenticationViewState.Idle, viewModel.state.value)
  }
}
