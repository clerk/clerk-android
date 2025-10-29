package com.clerk.ui.userprofile.security.password

import com.clerk.api.Clerk
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.user.User
import com.clerk.api.user.updatePassword
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

    viewModel.resetPassword("old", "new", true)
    advanceUntilIdle()

    assertEquals(UserProfileChangePasswordViewModel.State.Success, viewModel.state.value)
  }

  @Test
  fun resetPassword_failure_setsErrorState() = runTest {
    val user = mockk<User>()
    every { Clerk.user } returns user
    val error = ClerkErrorResponse(errors = listOf(ClerkErrorResponse.Error(longMessage = "fail")))
    coEvery { user.updatePassword(any()) } returns ClerkResult.Failure(error)

    val viewModel = UserProfileChangePasswordViewModel()

    viewModel.resetPassword("old", "new", false)
    advanceUntilIdle()

    assertEquals(UserProfileChangePasswordViewModel.State.Error("fail"), viewModel.state.value)
  }

  @Test
  fun resetPassword_withoutUser_setsGuardError() = runTest {
    every { Clerk.user } returns null

    val viewModel = UserProfileChangePasswordViewModel()

    viewModel.resetPassword("old", "new", false)
    advanceUntilIdle()

    assertEquals(UserProfileChangePasswordViewModel.State.Error("User does not exist"), viewModel.state.value)
  }

  @Test
  fun resetState_setsIdle() {
    val viewModel = UserProfileChangePasswordViewModel()

    viewModel.resetState()

    assertEquals(UserProfileChangePasswordViewModel.State.Idle, viewModel.state.value)
  }
}
