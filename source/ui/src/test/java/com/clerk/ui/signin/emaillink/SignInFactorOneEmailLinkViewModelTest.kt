package com.clerk.ui.signin.emaillink

import com.clerk.api.Clerk
import com.clerk.api.auth.Auth
import com.clerk.api.auth.AuthEvent
import com.clerk.api.magiclink.NativeMagicLinkError
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signin.SignIn
import com.clerk.ui.auth.AuthenticationViewState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
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
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

  @Test
  fun `sendLink surfaces backend long message for rate limits`() = runTest {
    every { auth.currentSignIn } returns SignIn(id = "sia_123", identifier = "sam@clerk.dev")
    coEvery { auth.startEmailLinkSignIn("sam@clerk.dev") } returns
      ClerkResult.apiFailure(
        NativeMagicLinkError(
          reasonCode = "too_many_requests",
          message = "Too many requests, retry later",
        )
      )

    val viewModel = SignInFactorOneEmailLinkViewModel()
    viewModel.sendLink()
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(
      AuthenticationViewState.Error("Too many requests, retry later"),
      viewModel.state.value,
    )
  }
}
