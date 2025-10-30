package com.clerk.ui.userprofile.security.delete

import app.cash.turbine.test
import com.clerk.api.Clerk
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.user.User
import com.clerk.api.user.delete
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
class DeleteAccountViewModelTest {

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
  fun deleteAccount_success_setsSuccessState() = runTest {
    val user = mockk<User>()
    every { Clerk.user } returns user
    coEvery { user.delete() } returns ClerkResult.success(mockk())

    val viewModel = DeleteAccountViewModel()
    viewModel.state.test {
      assertEquals(DeleteAccountViewModel.State.Idle, awaitItem())
      viewModel.deleteAccount()
      assertEquals(DeleteAccountViewModel.State.Loading, awaitItem())
      assertEquals(DeleteAccountViewModel.State.Success, awaitItem())
    }
  }

  @Test
  fun deleteAccount_failure_setsErrorState() = runTest {
    val user = mockk<User>()
    every { Clerk.user } returns user
    val error = ClerkErrorResponse(errors = listOf(Error(longMessage = "boom")))
    coEvery { user.delete() } returns ClerkResult.Failure(error)

    val viewModel = DeleteAccountViewModel()
    viewModel.state.test {
      assertEquals(DeleteAccountViewModel.State.Idle, awaitItem())
      viewModel.deleteAccount()
      assertEquals(DeleteAccountViewModel.State.Loading, awaitItem())
      assertEquals(DeleteAccountViewModel.State.Error("boom"), awaitItem())
    }
  }
}
