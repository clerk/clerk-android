package com.clerk.ui.userprofile.security.password

import app.cash.turbine.test
import com.clerk.api.Clerk
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.user.User
import com.clerk.api.user.updatePassword
import com.clerk.ui.userprofile.MainDispatcherRule
import io.mockk.coEvery
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
class UserProfileChangePasswordViewModelTest {

  @get:org.junit.Rule val dispatcherRule = MainDispatcherRule()

  @BeforeTest
  fun setUp() {
    mockkObject(Clerk)
    every { Clerk.user } returns null
    mockkStatic("com.clerk.api.user.UserKt")
  }

  @AfterTest
  fun tearDown() {
    unmockkStatic("com.clerk.api.user.UserKt")
    unmockkAll()
  }

  @Test
  fun resetPassword_success_setsSuccessState() = runTest {
    val user = mockk<User>()
    every { Clerk.user } returns user
    coEvery { user.updatePassword(any()) } returns ClerkResult.success(user)

    val viewModel = UserProfileChangePasswordViewModel()
    viewModel.state.test {
      assertEquals(UserProfileChangePasswordViewModel.State.Idle, awaitItem())
      viewModel.resetPassword("old", "new", true)
      assertEquals(UserProfileChangePasswordViewModel.State.Success, awaitItem())
    }
  }

  @Test
  fun resetPassword_failure_setsErrorState() = runTest {
    val user = mockk<User>()
    every { Clerk.user } returns user
    val error = ClerkErrorResponse(errors = listOf(Error(longMessage = "fail")))
    coEvery { user.updatePassword(any()) } returns ClerkResult.Failure(error)

    val viewModel = UserProfileChangePasswordViewModel()
    viewModel.state.test {
      assertEquals(UserProfileChangePasswordViewModel.State.Idle, awaitItem())
      viewModel.resetPassword("old", "new", false)
      assertEquals(UserProfileChangePasswordViewModel.State.Error("fail"), awaitItem())
    }
  }

  @Test
  fun resetPassword_withoutUser_setsGuardError() = runTest {
    every { Clerk.user } returns null

    val viewModel = UserProfileChangePasswordViewModel()
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
    val viewModel = UserProfileChangePasswordViewModel()
    // Already Idle; ensure it's Idle
    assertEquals(UserProfileChangePasswordViewModel.State.Idle, viewModel.state.value)
    viewModel.resetState()
    assertEquals(UserProfileChangePasswordViewModel.State.Idle, viewModel.state.value)
  }
}
