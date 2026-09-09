package com.clerk.ui.userprofile.security.delete

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
class DeleteAccountViewModelTest {
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
  fun deleteAccount_success_setsSuccessState() = runTest {
    val user = mockk<User>()
    every { user.id } returns "user_1"
    every { clerk.user } returns user
    coEvery { user.delete() } returns mockk()

    val viewModel = DeleteAccountViewModel(clerk)
    viewModel.state.test {
      assertEquals(DeleteAccountViewModel.State.Idle, awaitItem())
      viewModel.deleteAccount()
      assertEquals(DeleteAccountViewModel.State.Loading, awaitItem())
      assertEquals(DeleteAccountViewModel.State.Success, awaitItem())
    }
  }

  @Test
  fun deleteAccount_withoutUser_setsErrorState() = runTest {
    every { clerk.user } returns null
    val viewModel = DeleteAccountViewModel(clerk)
    viewModel.state.test {
      assertEquals(DeleteAccountViewModel.State.Idle, awaitItem())
      viewModel.deleteAccount()
      assertEquals(DeleteAccountViewModel.State.Loading, awaitItem())
      assertEquals(DeleteAccountViewModel.State.Error("User does not exist"), awaitItem())
    }
  }

  @Test
  fun deleteAccount_failure_setsErrorState() = runTest {
    val user = mockk<User>()
    every { user.id } returns "user_1"
    every { clerk.user } returns user
    val error = testCoreError("boom")
    coEvery { user.delete() } throws error

    val viewModel = DeleteAccountViewModel(clerk)
    viewModel.state.test {
      assertEquals(DeleteAccountViewModel.State.Idle, awaitItem())
      viewModel.deleteAccount()
      assertEquals(DeleteAccountViewModel.State.Loading, awaitItem())
      assertEquals(DeleteAccountViewModel.State.Error("boom"), awaitItem())
    }
  }
}
