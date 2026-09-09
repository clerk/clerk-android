package com.clerk.ui.userprofile.totp

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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class UserProfileMfaTotpViewModelTest {
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
  fun createTotp_success_setsSuccessState() = runTest {
    val user = mockk<User>()
    val totpResource = mockk<TOTP>()
    every { clerk.user } returns user
    coEvery { user.createTOTP() } returns totpResource

    val viewModel = UserProfileMfaTotpViewModel(clerk)
    advanceUntilIdle()

    assertEquals(UserProfileMfaTotpViewModel.State.Success(totpResource), viewModel.state.value)
  }

  @Test
  fun createTotp_failure_setsErrorState() = runTest {
    val user = mockk<User>()
    val error = testCoreError("fail")
    every { clerk.user } returns user
    coEvery { user.createTOTP() } throws error

    val viewModel = UserProfileMfaTotpViewModel(clerk)
    advanceUntilIdle()

    assertEquals(UserProfileMfaTotpViewModel.State.Error("fail"), viewModel.state.value)
  }

  @Test
  fun createTotp_withoutUser_setsErrorState() = runTest {
    val viewModel = UserProfileMfaTotpViewModel(clerk)
    advanceUntilIdle()

    assertEquals(
      UserProfileMfaTotpViewModel.State.Error("User does not exist"),
      viewModel.state.value,
    )
  }
}
