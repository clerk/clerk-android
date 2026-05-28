package com.clerk.ui.signup.emaillink

import com.clerk.api.Clerk
import com.clerk.api.auth.Auth
import com.clerk.api.auth.AuthEvent
import com.clerk.api.signup.SignUp
import com.clerk.ui.auth.AuthenticationViewState
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
class SignUpEmailLinkViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private val auth = mockk<Auth>(relaxed = true)
  private val events = MutableSharedFlow<AuthEvent>(extraBufferCapacity = 1)

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    mockkObject(Clerk)
    every { Clerk.auth } returns auth
    every { auth.events } returns events
    every { auth.currentSignUp } returns null
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

  @Test
  fun `sign up started event advances email link flow to next sign up step`() = runTest {
    val pendingSignUp = signUp(id = "sua_pending", unverifiedFields = listOf("phone_number"))

    val viewModel = SignUpEmailLinkViewModel(ioDispatcher = testDispatcher)
    advanceUntilIdle()

    events.tryEmit(AuthEvent.SignUpStarted(pendingSignUp))
    advanceUntilIdle()

    assertEquals(AuthenticationViewState.Success.SignUp(pendingSignUp), viewModel.state.value)
  }

  @Test
  fun `resume sync advances email link flow from current sign up`() = runTest {
    val pendingSignUp = signUp(id = "sua_resume", unverifiedFields = listOf("phone_number"))
    every { auth.currentSignUp } returns pendingSignUp

    val viewModel = SignUpEmailLinkViewModel(ioDispatcher = testDispatcher)
    advanceUntilIdle()

    viewModel.onHostResumed()

    assertEquals(AuthenticationViewState.Success.SignUp(pendingSignUp), viewModel.state.value)
  }

  private fun signUp(
    id: String = "sua_123",
    unverifiedFields: List<String> = listOf("email_address"),
  ): SignUp =
    SignUp(
      id = id,
      status = SignUp.Status.MISSING_REQUIREMENTS,
      requiredFields = listOf("email_address", "phone_number", "username"),
      optionalFields = emptyList(),
      missingFields = emptyList(),
      unverifiedFields = unverifiedFields,
      verifications = emptyMap(),
      emailAddress = "sam@clerk.dev",
      phoneNumber = "+13012370655",
      username = "swolfand",
      passwordEnabled = false,
    )
}
