package com.clerk.ui.userprofile.mfa

import app.cash.turbine.test
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.phonenumber.PhoneNumber
import com.clerk.api.phonenumber.setReservedForSecondFactor
import com.clerk.ui.userprofile.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class MfaAddSmsViewModelTest {

  @get:org.junit.Rule val dispatcherRule = MainDispatcherRule()

  @BeforeTest
  fun setUp() {
    mockkStatic("com.clerk.api.phonenumber.PhoneNumberKt")
  }

  @AfterTest
  fun tearDown() {
    unmockkStatic("com.clerk.api.phonenumber.PhoneNumberKt")
  }

  @Test
  fun reserveForSecondFactor_success_setsSuccessState() = runTest {
    val phoneNumber = mockk<PhoneNumber>()
    coEvery { phoneNumber.setReservedForSecondFactor(true) } returns ClerkResult.success(mockk())

    val viewModel = MfaAddSmsViewModel()
    viewModel.state.test {
      assertEquals(MfaAddSmsViewModel.State.Idle, awaitItem())
      viewModel.reserveForSecondFactor(phoneNumber)
      assertEquals(MfaAddSmsViewModel.State.Loading, awaitItem())
      assertEquals(MfaAddSmsViewModel.State.Success, awaitItem())
    }
  }

  @Test
  fun reserveForSecondFactor_failure_setsErrorState() = runTest {
    val phoneNumber = mockk<PhoneNumber>()
    val error = ClerkErrorResponse(errors = listOf(Error(longMessage = "fail")))
    coEvery { phoneNumber.setReservedForSecondFactor(true) } returns ClerkResult.Failure(error)

    val viewModel = MfaAddSmsViewModel()
    viewModel.state.test {
      assertEquals(MfaAddSmsViewModel.State.Idle, awaitItem())
      viewModel.reserveForSecondFactor(phoneNumber)
      assertEquals(MfaAddSmsViewModel.State.Loading, awaitItem())
      assertEquals(MfaAddSmsViewModel.State.Error("fail"), awaitItem())
    }
  }
}
