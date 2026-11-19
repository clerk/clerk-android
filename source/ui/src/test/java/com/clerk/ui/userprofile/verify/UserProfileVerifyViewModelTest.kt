package com.clerk.ui.userprofile.verify

import app.cash.turbine.test
import com.clerk.api.Clerk
import com.clerk.api.emailaddress.EmailAddress
import com.clerk.api.emailaddress.attemptVerification
import com.clerk.api.emailaddress.prepareVerification
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.phonenumber.PhoneNumber
import com.clerk.api.phonenumber.attemptVerification
import com.clerk.api.user.User
import com.clerk.api.user.attemptTotpVerification
import com.clerk.ui.userprofile.MainDispatcherRule
import com.clerk.ui.userprofile.verify.UserProfileVerifyViewModel.AuthState
import com.clerk.ui.userprofile.verify.UserProfileVerifyViewModel.VerificationTextState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.unmockkStatic
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    viewModel.state.test {
      assertEquals(UserProfileVerifyViewModel.AuthState.Idle, awaitItem())
      viewModel.prepareEmailAddress(email)
      assertEquals(UserProfileVerifyViewModel.AuthState.Loading, awaitItem())
      assertEquals(UserProfileVerifyViewModel.AuthState.Success, awaitItem())
    }
  }

  @Test
  fun prepareEmailAddress_failure_setsError() = runTest {
    val email = mockk<EmailAddress>()
    val error = ClerkErrorResponse(errors = listOf(Error(longMessage = "fail")))
    coEvery { email.prepareVerification(any()) } returns ClerkResult.Failure(error)

    val viewModel = UserProfileVerifyViewModel()
    viewModel.state.test {
      assertEquals(UserProfileVerifyViewModel.AuthState.Idle, awaitItem())
      viewModel.prepareEmailAddress(email)
      assertEquals(UserProfileVerifyViewModel.AuthState.Loading, awaitItem())
      assertEquals(UserProfileVerifyViewModel.AuthState.Error("fail"), awaitItem())
    }
  }

  @Test
  fun attemptEmailAddress_success_updatesVerificationState() = runTest {
    val email = mockk<EmailAddress>()
    coEvery { email.attemptVerification(any()) } returns ClerkResult.success(mockk())

    val viewModel = UserProfileVerifyViewModel()
    viewModel.verificationTextState.test {
      assertEquals(UserProfileVerifyViewModel.VerificationTextState.Default, awaitItem())
      viewModel.attemptEmailAddress(email, "123456")
      assertEquals(UserProfileVerifyViewModel.VerificationTextState.Verifying, awaitItem())
      assertEquals(UserProfileVerifyViewModel.VerificationTextState.Verified(), awaitItem())
    }
    coVerify(exactly = 1) { email.attemptVerification("123456") }
  }

  @Test
  fun attemptEmailAddress_incorrectCode_marksCodeFieldWithoutAnError() = runTest {
    val email = mockk<EmailAddress>()
    coEvery { email.attemptVerification(any()) } returns ClerkResult.Failure(incorrectCode)

    val viewModel = UserProfileVerifyViewModel()
    viewModel.verificationTextState.test {
      assertEquals(VerificationTextState.Default, awaitItem())
      viewModel.attemptEmailAddress(email, "123456")
      assertEquals(VerificationTextState.Verifying, awaitItem())
      assertEquals(VerificationTextState.IncorrectCode, awaitItem())
    }
    assertEquals(AuthState.Idle, viewModel.state.value)
  }

  @Test
  fun attemptEmailAddress_otherFailure_showsApiMessageAndLeavesCodeFieldClean() = runTest {
    val email = mockk<EmailAddress>()
    coEvery { email.attemptVerification(any()) } returns ClerkResult.Failure(rateLimited)

    val viewModel = UserProfileVerifyViewModel()
    viewModel.verificationTextState.test {
      assertEquals(VerificationTextState.Default, awaitItem())
      viewModel.attemptEmailAddress(email, "123456")
      assertEquals(VerificationTextState.Verifying, awaitItem())
      assertEquals(VerificationTextState.Default, awaitItem())
    }
    assertEquals(AuthState.Error(RATE_LIMITED_MESSAGE), viewModel.state.value)
  }

  @Test
  fun attemptPhoneNumber_incorrectCode_marksCodeFieldWithoutAnError() = runTest {
    val phone = mockk<PhoneNumber>()
    coEvery { phone.attemptVerification(any()) } returns ClerkResult.Failure(incorrectCode)

    val viewModel = UserProfileVerifyViewModel()
    viewModel.verificationTextState.test {
      assertEquals(VerificationTextState.Default, awaitItem())
      viewModel.attemptPhoneNumber(phone, "123456")
      assertEquals(VerificationTextState.Verifying, awaitItem())
      assertEquals(VerificationTextState.IncorrectCode, awaitItem())
    }
    assertEquals(AuthState.Idle, viewModel.state.value)
  }

  @Test
  fun attemptPhoneNumber_otherFailure_showsApiMessageAndLeavesCodeFieldClean() = runTest {
    val phone = mockk<PhoneNumber>()
    coEvery { phone.attemptVerification(any()) } returns ClerkResult.Failure(rateLimited)

    val viewModel = UserProfileVerifyViewModel()
    viewModel.verificationTextState.test {
      assertEquals(VerificationTextState.Default, awaitItem())
      viewModel.attemptPhoneNumber(phone, "123456")
      assertEquals(VerificationTextState.Verifying, awaitItem())
      assertEquals(VerificationTextState.Default, awaitItem())
    }
    assertEquals(AuthState.Error(RATE_LIMITED_MESSAGE), viewModel.state.value)
  }

  @Test
  fun clearError_afterOtherFailure_returnsToIdle() = runTest {
    val email = mockk<EmailAddress>()
    coEvery { email.attemptVerification(any()) } returns ClerkResult.Failure(rateLimited)

    val viewModel = UserProfileVerifyViewModel()
    viewModel.state.test {
      assertEquals(AuthState.Idle, awaitItem())
      viewModel.attemptEmailAddress(email, "123456")
      assertEquals(AuthState.Error(RATE_LIMITED_MESSAGE), awaitItem())
      viewModel.clearError()
      assertEquals(AuthState.Idle, awaitItem())
    }
  }

  @Test
  fun attemptTotp_success_setsVerified() = runTest {
    val user = mockk<User>()
    every { Clerk.user } returns user
    val totpResource =
      mockk<com.clerk.api.network.model.totp.TOTPResource>(relaxed = true) {
        every { backupCodes } returns listOf("code-1", "code-2")
      }
    coEvery { user.attemptTotpVerification(any()) } returns ClerkResult.success(totpResource)

    val viewModel = UserProfileVerifyViewModel()
    viewModel.verificationTextState.test {
      assertEquals(UserProfileVerifyViewModel.VerificationTextState.Default, awaitItem())
      viewModel.attemptTotp("654321")
      assertEquals(UserProfileVerifyViewModel.VerificationTextState.Verifying, awaitItem())
      assertEquals(
        UserProfileVerifyViewModel.VerificationTextState.Verified(listOf("code-1", "code-2")),
        awaitItem(),
      )
    }
    coVerify(exactly = 1) { user.attemptTotpVerification("654321") }
  }

  @Test
  fun attemptTotp_incorrectCode_marksCodeFieldWithoutAnError() = runTest {
    val user = mockk<User>()
    every { Clerk.user } returns user
    coEvery { user.attemptTotpVerification(any()) } returns ClerkResult.Failure(incorrectCode)

    val viewModel = UserProfileVerifyViewModel()
    viewModel.verificationTextState.test {
      assertEquals(VerificationTextState.Default, awaitItem())
      viewModel.attemptTotp("654321")
      assertEquals(VerificationTextState.Verifying, awaitItem())
      assertEquals(VerificationTextState.IncorrectCode, awaitItem())
    }
    assertEquals(AuthState.Idle, viewModel.state.value)
  }

  @Test
  fun attemptTotp_otherFailure_showsApiMessageAndLeavesCodeFieldClean() = runTest {
    val user = mockk<User>()
    every { Clerk.user } returns user
    coEvery { user.attemptTotpVerification(any()) } returns ClerkResult.Failure(rateLimited)

    val viewModel = UserProfileVerifyViewModel()
    viewModel.verificationTextState.test {
      assertEquals(VerificationTextState.Default, awaitItem())
      viewModel.attemptTotp("654321")
      assertEquals(VerificationTextState.Verifying, awaitItem())
      assertEquals(VerificationTextState.Default, awaitItem())
    }
    assertEquals(AuthState.Error(RATE_LIMITED_MESSAGE), viewModel.state.value)
  }

  @Test
  fun attemptTotp_withoutUser_showsErrorAndLeavesCodeFieldClean() = runTest {
    every { Clerk.user } returns null

    val viewModel = UserProfileVerifyViewModel()
    viewModel.verificationTextState.test {
      assertEquals(VerificationTextState.Default, awaitItem())
      viewModel.attemptTotp("654321")
      assertEquals(VerificationTextState.Verifying, awaitItem())
      assertEquals(VerificationTextState.Default, awaitItem())
    }
    assertEquals(AuthState.Error("User does not exist"), viewModel.state.value)
  }

  private companion object {
    const val RATE_LIMITED_MESSAGE = "Too many requests. Please try again in a bit."

    val incorrectCode =
      ClerkErrorResponse(
        errors = listOf(Error(code = "form_code_incorrect", longMessage = "Incorrect code"))
      )

    val rateLimited =
      ClerkErrorResponse(
        errors = listOf(Error(code = "too_many_requests", longMessage = RATE_LIMITED_MESSAGE))
      )
  }
}
