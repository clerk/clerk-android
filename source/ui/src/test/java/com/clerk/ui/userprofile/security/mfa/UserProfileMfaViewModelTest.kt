package com.clerk.ui.userprofile.security.mfa

import com.clerk.api.Clerk
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.phonenumber.PhoneNumber
import com.clerk.api.phonenumber.setReservedForSecondFactor
import com.clerk.api.user.User
import com.clerk.api.user.createBackupCodes
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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class UserProfileMfaViewModelTest {

  @get:org.junit.Rule val dispatcherRule = MainDispatcherRule()

  @BeforeTest
  fun setUp() {
    mockkObject(Clerk)
    every { Clerk.user } returns null
    mockkStatic("com.clerk.api.phonenumber.PhoneNumberKt")
    mockkStatic("com.clerk.api.user.UserKt")
  }

  @AfterTest
  fun tearDown() {
    unmockkStatic("com.clerk.api.user.UserKt")
    unmockkStatic("com.clerk.api.phonenumber.PhoneNumberKt")
    unmockkAll()
  }

  @Test
  fun makeDefaultSecondFactor_failure_setsErrorState() = runTest {
    val user = mockk<User>()
    val phone = mockk<PhoneNumber>()
    every { Clerk.user } returns user
    val error = ClerkErrorResponse(errors = listOf(Error(longMessage = "fail")))
    coEvery { phone.setReservedForSecondFactor(true) } returns ClerkResult.Failure(error)

    val viewModel = UserProfileMfaViewModel()

    viewModel.makeDefaultSecondFactor(phone)
    advanceUntilIdle()

    assertEquals(UserProfileMfaViewModel.State.Error("fail"), viewModel.state.value)
  }

  @Test
  fun makeDefaultSecondFactor_noUser_setsErrorState() = runTest {
    every { Clerk.user } returns null

    val viewModel = UserProfileMfaViewModel()

    viewModel.makeDefaultSecondFactor(mockk())
    advanceUntilIdle()

    assertEquals(UserProfileMfaViewModel.State.Error("User does not exist"), viewModel.state.value)
  }

  @Test
  fun regenerateBackupCodes_failure_setsErrorState() = runTest {
    val user = mockk<User>()
    every { Clerk.user } returns user
    val error = ClerkErrorResponse(errors = listOf(Error(longMessage = "boom")))
    coEvery { user.createBackupCodes() } returns ClerkResult.Failure(error)

    val viewModel = UserProfileMfaViewModel()

    viewModel.regenerateBackupCodes()
    advanceUntilIdle()

    assertEquals(UserProfileMfaViewModel.State.Error("boom"), viewModel.state.value)
  }
}
