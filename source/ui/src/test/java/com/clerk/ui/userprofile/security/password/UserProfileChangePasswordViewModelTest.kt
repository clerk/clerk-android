package com.clerk.ui.userprofile.security.password

import app.cash.turbine.test
import com.clerk.api.*
import com.clerk.api.Clerk
import com.clerk.api.User
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
class UserProfileChangePasswordViewModelTest {
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
  fun resetPassword_success_setsSuccessState() = runTest {
    val user = mockk<User>()
    every { clerk.user } returns user
    coEvery { user.updatePassword(any()) } returns user

    val viewModel = UserProfileChangePasswordViewModel(clerk)
    viewModel.state.test {
      assertEquals(UserProfileChangePasswordViewModel.State.Idle, awaitItem())
      viewModel.resetPassword("old", "new", true)
      assertEquals(UserProfileChangePasswordViewModel.State.Success, awaitItem())
    }
  }

  @Test
  fun resetPassword_failure_setsErrorState() = runTest {
    val user = mockk<User>()
    every { clerk.user } returns user
    val error = testCoreError("fail")
    coEvery { user.updatePassword(any()) } throws error

    val viewModel = UserProfileChangePasswordViewModel(clerk)
    viewModel.state.test {
      assertEquals(UserProfileChangePasswordViewModel.State.Idle, awaitItem())
      viewModel.resetPassword("old", "new", false)
      assertEquals(UserProfileChangePasswordViewModel.State.Error("fail"), awaitItem())
    }
  }

  @Test
  fun resetPassword_withoutUser_setsGuardError() = runTest {
    every { clerk.user } returns null

    val viewModel = UserProfileChangePasswordViewModel(clerk)
    viewModel.state.test {
      assertEquals(UserProfileChangePasswordViewModel.State.Idle, awaitItem())
      viewModel.resetPassword("old", "new", false)
      assertEquals(
        UserProfileChangePasswordViewModel.State.Error("User does not exist"),
        awaitItem(),
      )
    }
  }

  @Test
  fun resetState_setsIdle() {
    val viewModel = UserProfileChangePasswordViewModel(clerk)
    // Already Idle; ensure it's Idle
    assertEquals(UserProfileChangePasswordViewModel.State.Idle, viewModel.state.value)
    viewModel.resetState()
    assertEquals(UserProfileChangePasswordViewModel.State.Idle, viewModel.state.value)
  }
}
