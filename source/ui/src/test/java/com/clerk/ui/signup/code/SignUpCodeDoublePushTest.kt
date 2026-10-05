package com.clerk.ui.signup.code

import android.content.Context
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.test.core.app.ApplicationProvider
import com.clerk.api.Clerk
import com.clerk.api.Constants
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signup.SignUp
import com.clerk.api.signup.sendEmailCode
import com.clerk.api.signup.sendPhoneCode
import com.clerk.ui.auth.AuthDestination
import com.clerk.ui.auth.AuthState
import com.clerk.ui.auth.AuthenticationViewState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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

/**
 * Regression test for the sign-up code screen being pushed twice: preparing the code (on entry or
 * on resend) returned the still-unverified sign-up as a success state, which the screen routed,
 * pushing a second copy of itself.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SignUpCodeDoublePushTest {

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    mockkObject(Clerk)
    mockkStatic("com.clerk.api.signup.SignUpKt")
    val client = mockk<Client>(relaxed = true)
    every { Clerk.client } returns client
    every { client.signUp } returns unverifiedSignUp()
    coEvery { any<SignUp>().sendEmailCode() } returns ClerkResult.success(unverifiedSignUp())
    coEvery { any<SignUp>().sendPhoneCode() } returns ClerkResult.success(unverifiedSignUp())
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

  @Test
  fun preparingAnEmailCodeDoesNotPushTheCodeScreenAgain() = runTest {
    assertPrepareKeepsBackStack(SignUpCodeField.Email("sam@clerk.dev"))
    coVerify(exactly = 1) { any<SignUp>().sendEmailCode() }
  }

  @Test
  fun preparingAPhoneCodeDoesNotPushTheCodeScreenAgain() = runTest {
    assertPrepareKeepsBackStack(SignUpCodeField.Phone("+15555550100"))
    coVerify(exactly = 1) { any<SignUp>().sendPhoneCode() }
  }

  private fun TestScope.assertPrepareKeepsBackStack(field: SignUpCodeField) {
    val backStack =
      NavBackStack<NavKey>(AuthDestination.AuthStart, AuthDestination.SignUpCode(field))
    val authState = AuthState(backStack = backStack, sharedPreferences = preferences())
    val viewModel = SignUpCodeViewModel()

    viewModel.prepare(field)
    advanceUntilIdle()

    // Mirrors AuthStateEffects, which routes every sign-up success state.
    (viewModel.state.value as? AuthenticationViewState.Success.SignUp)?.let {
      authState.setToStepForStatus(it.signUp, session = null) {}
    }

    assertEquals(
      listOf(AuthDestination.AuthStart, AuthDestination.SignUpCode(field)),
      backStack.toList(),
    )
  }

  private fun unverifiedSignUp() =
    SignUp(
      id = "sign_up_123",
      status = SignUp.Status.MISSING_REQUIREMENTS,
      requiredFields = listOf("email_address"),
      optionalFields = emptyList(),
      missingFields = emptyList(),
      passwordEnabled = false,
      unverifiedFields = listOf("email_address"),
      verifications =
        mapOf(
          "email_address" to
            Verification(
              status = Verification.Status.UNVERIFIED,
              strategy = Constants.Strategy.EMAIL_CODE,
            )
        ),
      emailAddress = "sam@clerk.dev",
    )

  private fun preferences() =
    ApplicationProvider.getApplicationContext<Context>()
      .getSharedPreferences(Constants.Storage.CLERK_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)
}
