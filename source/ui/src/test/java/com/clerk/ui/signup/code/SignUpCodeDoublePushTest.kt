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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SignUpCodeDoublePushTest {

  private val testDispatcher = StandardTestDispatcher()
  private val client = mockk<Client>(relaxed = true)

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    mockkObject(Clerk)
    mockkStatic("com.clerk.api.signup.SignUpKt")
    every { Clerk.client } returns client
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

  @Test
  fun preparingAnEmailCodeDoesNotPushTheCodeScreenAgain() = runTest {
    every { client.signUp } returns emailUnverifiedSignUp()
    coEvery { any<SignUp>().sendEmailCode() } returns ClerkResult.success(emailUnverifiedSignUp())

    assertPrepareKeepsBackStack(SignUpCodeField.Email("sam@clerk.dev"))
    coVerify(exactly = 1) { any<SignUp>().sendEmailCode() }
  }

  @Test
  fun preparingAPhoneCodeDoesNotPushTheCodeScreenAgain() = runTest {
    every { client.signUp } returns phoneUnverifiedSignUp()
    coEvery { any<SignUp>().sendPhoneCode() } returns ClerkResult.success(phoneUnverifiedSignUp())

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

    routeSignUpSuccess(viewModel.state.value, authState)

    assertEquals(
      listOf(AuthDestination.AuthStart, AuthDestination.SignUpCode(field)),
      backStack.toList(),
    )
  }

  private fun routeSignUpSuccess(state: AuthenticationViewState, authState: AuthState) {
    (state as? AuthenticationViewState.Success.SignUp)?.let {
      authState.setToStepForStatus(it.signUp, session = null) {}
    }
  }

  private fun emailUnverifiedSignUp() =
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

  private fun phoneUnverifiedSignUp() =
    SignUp(
      id = "sign_up_123",
      status = SignUp.Status.MISSING_REQUIREMENTS,
      requiredFields = listOf("phone_number"),
      optionalFields = emptyList(),
      missingFields = emptyList(),
      passwordEnabled = false,
      unverifiedFields = listOf("phone_number"),
      verifications =
        mapOf(
          "phone_number" to
            Verification(
              status = Verification.Status.UNVERIFIED,
              strategy = Constants.Strategy.PHONE_CODE,
            )
        ),
      phoneNumber = "+15555550100",
    )

  private fun preferences() =
    ApplicationProvider.getApplicationContext<Context>()
      .getSharedPreferences(Constants.Storage.CLERK_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)
}
