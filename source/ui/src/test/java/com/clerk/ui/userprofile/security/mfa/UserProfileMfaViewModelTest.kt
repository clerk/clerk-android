package com.clerk.ui.userprofile.security.mfa

import com.clerk.api.*
import com.clerk.api.Clerk
import com.clerk.api.PhoneNumber
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
class UserProfileMfaViewModelTest {
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
  fun makeDefaultSecondFactor_failure_setsErrorState() = runTest {
    val user = mockk<User>()
    val phone = mockk<PhoneNumber>()
    every { clerk.user } returns user
    val error = testCoreError("fail")
    coEvery { phone.makeDefaultSecondFactor() } throws error

    val viewModel = UserProfileMfaViewModel(clerk)

    advanceUntilIdle()

    viewModel.makeDefaultSecondFactor(phone)
    assertEquals(UserProfileMfaViewModel.State.Loading, viewModel.state.value)
    advanceUntilIdle()
    assertEquals(UserProfileMfaViewModel.State.Error("fail"), viewModel.state.value)
  }

  @Test
  fun makeDefaultSecondFactor_noUser_setsErrorState() = runTest {
    every { clerk.user } returns null

    val viewModel = UserProfileMfaViewModel(clerk)

    viewModel.makeDefaultSecondFactor(mockk())
    advanceUntilIdle()

    assertEquals(UserProfileMfaViewModel.State.Error("User does not exist"), viewModel.state.value)
  }

  @Test
  fun regenerateBackupCodes_failure_setsErrorState() = runTest {
    val user = mockk<User>()
    every { clerk.user } returns user
    val error = testCoreError("boom")
    coEvery { user.createBackupCode() } throws error

    val viewModel = UserProfileMfaViewModel(clerk)

    viewModel.regenerateBackupCodes()
    advanceUntilIdle()

    assertEquals(UserProfileMfaViewModel.State.Error("boom"), viewModel.state.value)
  }
}
