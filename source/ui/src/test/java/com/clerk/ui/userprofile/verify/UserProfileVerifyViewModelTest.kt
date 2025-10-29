package com.clerk.ui.userprofile.verify

import com.clerk.api.Clerk
import com.clerk.api.emailaddress.EmailAddress
import com.clerk.api.emailaddress.attemptVerification
import com.clerk.api.emailaddress.prepareVerification
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.phonenumber.PhoneNumber
import com.clerk.api.phonenumber.attemptVerification
import com.clerk.api.phonenumber.prepareVerification
import com.clerk.api.user.User
import com.clerk.api.user.attemptTotpVerification
import com.clerk.ui.userprofile.MainDispatcherRule
import io.mockk.any
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.unmockkStatic
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class UserProfileVerifyViewModelTest {

  @get:org.junit.Rule val dispatcherRule = MainDispatcherRule()

  @BeforeTest
  fun setUp() {
    mockkObject(Clerk)
    every { Clerk.user } returns null
    mockkStatic("com.clerk.api.emailaddress.EmailAddressKt")
    mockkStatic("com.clerk.api.phonenumber.PhoneNumberKt")
    mockkStatic("com.clerk.api.user.UserKt")
  }

  @AfterTest
  fun tearDown() {
    unmockkStatic("com.clerk.api.user.UserKt")
    unmockkStatic("com.clerk.api.phonenumber.PhoneNumberKt")
    unmockkStatic("com.clerk.api.emailaddress.EmailAddressKt")
    unmockkAll()
  }

  @Test
  fun prepareEmailAddress_success_setsAuthSuccess() = runTest {
    val email = mockk<EmailAddress>()
    coEvery { email.prepareVerification(any()) } returns ClerkResult.success(mockk())

    val viewModel = UserProfileVerifyViewModel()

    viewModel.prepareEmailAddress(email)
    advanceUntilIdle()

    assertEquals(UserProfileVerifyViewModel.AuthState.Success, viewModel.state.value)
  }

  @Test
  fun prepareEmailAddress_failure_setsError() = runTest {
    val email = mockk<EmailAddress>()
    val error = ClerkErrorResponse(errors = listOf(ClerkErrorResponse.Error(longMessage = "fail")))
    coEvery { email.prepareVerification(any()) } returns ClerkResult.Failure(error)

    val viewModel = UserProfileVerifyViewModel()

    viewModel.prepareEmailAddress(email)
    advanceUntilIdle()

    assertEquals(UserProfileVerifyViewModel.AuthState.Error("fail"), viewModel.state.value)
  }

  @Test
  fun attemptEmailAddress_success_updatesVerificationState() = runTest {
    val email = mockk<EmailAddress>()
    coEvery { email.attemptVerification(any()) } returns ClerkResult.success(mockk())

    val viewModel = UserProfileVerifyViewModel()

    viewModel.attemptEmailAddress(email, "123456")
    advanceUntilIdle()

    assertEquals(UserProfileVerifyViewModel.VerificationTextState.Verified, viewModel.verificationTextState.value)
  }

  @Test
  fun attemptEmailAddress_failure_updatesVerificationError() = runTest {
    val email = mockk<EmailAddress>()
    val error = ClerkErrorResponse(errors = listOf(ClerkErrorResponse.Error(longMessage = "bad")))
    coEvery { email.attemptVerification(any()) } returns ClerkResult.Failure(error)

    val viewModel = UserProfileVerifyViewModel()

    viewModel.attemptEmailAddress(email, "123456")
    advanceUntilIdle()

    assertEquals(
      UserProfileVerifyViewModel.VerificationTextState.Error("bad"),
      viewModel.verificationTextState.value,
    )
  }

  @Test
  fun attemptTotp_success_setsVerified() = runTest {
    val user = mockk<User>()
    every { Clerk.user } returns user
    coEvery { user.attemptTotpVerification(any()) } returns ClerkResult.success(mockk())

    val viewModel = UserProfileVerifyViewModel()

    viewModel.attemptTotp("654321")
    advanceUntilIdle()

    assertEquals(UserProfileVerifyViewModel.VerificationTextState.Verified, viewModel.verificationTextState.value)
  }

  @Test
  fun attemptTotp_failure_setsError() = runTest {
    val user = mockk<User>()
    every { Clerk.user } returns user
    val error = ClerkErrorResponse(errors = listOf(ClerkErrorResponse.Error(longMessage = "oops")))
    coEvery { user.attemptTotpVerification(any()) } returns ClerkResult.Failure(error)

    val viewModel = UserProfileVerifyViewModel()

    viewModel.attemptTotp("654321")
    advanceUntilIdle()

    assertEquals(
      UserProfileVerifyViewModel.VerificationTextState.Error("oops"),
      viewModel.verificationTextState.value,
    )
  }

  @Test
  fun attemptTotp_withoutUser_setsGuardError() = runTest {
    every { Clerk.user } returns null

    val viewModel = UserProfileVerifyViewModel()

    viewModel.attemptTotp("654321")
    advanceUntilIdle()

    assertEquals(
      UserProfileVerifyViewModel.VerificationTextState.Error("User does not exist"),
      viewModel.verificationTextState.value,
    )
  }
}
