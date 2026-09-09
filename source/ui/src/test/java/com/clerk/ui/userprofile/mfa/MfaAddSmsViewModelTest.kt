package com.clerk.ui.userprofile.mfa

import app.cash.turbine.test
import com.clerk.api.*
import com.clerk.api.PhoneNumber
import com.clerk.testing.mockClerk
import com.clerk.testing.testCoreError
import com.clerk.ui.userprofile.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class MfaAddSmsViewModelTest {

  @get:org.junit.Rule val dispatcherRule = MainDispatcherRule()

  @BeforeTest fun setUp() {}

  @AfterTest fun tearDown() {}

  @Test
  fun reserveForSecondFactor_success_setsSuccessState() = runTest {
    val phoneNumber = mockk<PhoneNumber>(relaxed = true)
    coEvery { phoneNumber.backupCodes() } returns null
    coEvery {
      phoneNumber.setReservedForSecondFactor(SetReservedForSecondFactorParams(true))
    } returns phoneNumber

    val viewModel = MfaAddSmsViewModel(mockClerk())
    viewModel.state.test {
      assertEquals(MfaAddSmsViewModel.State.Idle, awaitItem())
      viewModel.reserveForSecondFactor(phoneNumber)
      assertEquals(MfaAddSmsViewModel.State.Loading, awaitItem())
      assertEquals(MfaAddSmsViewModel.State.Success(phoneNumber, null), awaitItem())
    }
  }

  @Test
  fun reserveForSecondFactor_failure_setsErrorState() = runTest {
    val phoneNumber = mockk<PhoneNumber>(relaxed = true)
    coEvery { phoneNumber.backupCodes() } returns null
    val error = testCoreError("fail")
    coEvery {
      phoneNumber.setReservedForSecondFactor(SetReservedForSecondFactorParams(true))
    } throws error

    val viewModel = MfaAddSmsViewModel(mockClerk())
    viewModel.state.test {
      assertEquals(MfaAddSmsViewModel.State.Idle, awaitItem())
      viewModel.reserveForSecondFactor(phoneNumber)
      assertEquals(MfaAddSmsViewModel.State.Loading, awaitItem())
      assertEquals(MfaAddSmsViewModel.State.Error("fail"), awaitItem())
    }
  }
}
