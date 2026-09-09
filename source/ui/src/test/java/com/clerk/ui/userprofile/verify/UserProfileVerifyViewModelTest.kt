package com.clerk.ui.userprofile.verify

import app.cash.turbine.test
import com.clerk.api.*
import com.clerk.testing.testCoreError
import com.clerk.ui.userprofile.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class UserProfileVerifyViewModelTest {
  private val clerk = mockk<Clerk>(relaxed = true)

  @get:org.junit.Rule val dispatcherRule = MainDispatcherRule()

  @BeforeTest
  fun setUp() {
    every { clerk.user } returns null
  }

  @AfterTest
  fun tearDown() {

    unmockkAll()
  }

  @Test
  fun prepareEmailAddress_success_setsAuthSuccess() = runTest {
    val email = mockk<EmailAddress>()
    coEvery { email.prepareVerification(any()) } returns mockk()

    val viewModel = UserProfileVerifyViewModel(clerk)
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
    val error = testCoreError("fail")
    coEvery { email.prepareVerification(any()) } throws error

    val viewModel = UserProfileVerifyViewModel(clerk)
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
    coEvery { email.attemptVerification(any()) } returns mockk()

    val viewModel = UserProfileVerifyViewModel(clerk)
    viewModel.verificationTextState.test {
      assertEquals(UserProfileVerifyViewModel.VerificationTextState.Default, awaitItem())
      viewModel.attemptEmailAddress(email, "123456")
      assertEquals(UserProfileVerifyViewModel.VerificationTextState.Verifying, awaitItem())
      assertEquals(UserProfileVerifyViewModel.VerificationTextState.Verified(), awaitItem())
    }
  }

  @Test
  fun attemptEmailAddress_failure_updatesVerificationError() = runTest {
    val email = mockk<EmailAddress>()
    val error = testCoreError("bad")
    coEvery { email.attemptVerification(any()) } throws error

    val viewModel = UserProfileVerifyViewModel(clerk)
    viewModel.verificationTextState.test {
      assertEquals(UserProfileVerifyViewModel.VerificationTextState.Default, awaitItem())
      viewModel.attemptEmailAddress(email, "123456")
      assertEquals(UserProfileVerifyViewModel.VerificationTextState.Verifying, awaitItem())
      assertEquals(UserProfileVerifyViewModel.VerificationTextState.Error("bad"), awaitItem())
    }
  }

  @Test
  fun attemptTotp_success_setsVerified() = runTest {
    val user = mockk<User>()
    every { clerk.user } returns user
    val totpResource =
      mockk<TOTP>(relaxed = true) {
        every { backupCodes } returns null
      }
    coEvery { user.verifyTOTP(any()) } returns totpResource

    val viewModel = UserProfileVerifyViewModel(clerk)
    viewModel.verificationTextState.test {
      assertEquals(UserProfileVerifyViewModel.VerificationTextState.Default, awaitItem())
      viewModel.attemptTotp("654321")
      assertEquals(UserProfileVerifyViewModel.VerificationTextState.Verifying, awaitItem())
      assertEquals(UserProfileVerifyViewModel.VerificationTextState.Verified(), awaitItem())
    }
  }

  @Test
  fun attemptTotp_failure_setsError() = runTest {
    val user = mockk<User>()
    every { clerk.user } returns user
    val error = testCoreError("oops")
    coEvery { user.verifyTOTP(any()) } throws error

    val viewModel = UserProfileVerifyViewModel(clerk)
    viewModel.verificationTextState.test {
      assertEquals(UserProfileVerifyViewModel.VerificationTextState.Default, awaitItem())
      viewModel.attemptTotp("654321")
      assertEquals(UserProfileVerifyViewModel.VerificationTextState.Verifying, awaitItem())
      assertEquals(UserProfileVerifyViewModel.VerificationTextState.Error("oops"), awaitItem())
    }
  }

  @Test
  fun attemptTotp_withoutUser_setsGuardError() = runTest {
    every { clerk.user } returns null

    val viewModel = UserProfileVerifyViewModel(clerk)
    viewModel.verificationTextState.test {
      assertEquals(UserProfileVerifyViewModel.VerificationTextState.Default, awaitItem())
      viewModel.attemptTotp("654321")
      assertEquals(UserProfileVerifyViewModel.VerificationTextState.Verifying, awaitItem())
      assertEquals(
        UserProfileVerifyViewModel.VerificationTextState.Error("User does not exist"),
        awaitItem(),
      )
    }
  }
}
