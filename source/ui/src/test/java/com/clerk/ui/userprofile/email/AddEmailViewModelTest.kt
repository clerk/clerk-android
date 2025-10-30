package com.clerk.ui.userprofile.email

import app.cash.turbine.test
import com.clerk.api.Clerk
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.user.User
import com.clerk.api.user.createEmailAddress
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
class AddEmailViewModelTest {

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
  fun addEmail_success_emitsSuccessState() = runTest {
    val user = mockk<User>()
    every { Clerk.user } returns user
    coEvery { user.createEmailAddress("user@example.com") } returns ClerkResult.success(mockk())

    val viewModel = AddEmailViewModel()
    viewModel.state.test {
      assertEquals(AddEmailViewModel.State.Idle, awaitItem())
      viewModel.addEmail("user@example.com")
      assertEquals(AddEmailViewModel.State.Loading, awaitItem())
      assertEquals(AddEmailViewModel.State.Success, awaitItem())
    }
  }

  @Test
  fun addEmail_failure_emitsErrorState() = runTest {
    val user = mockk<User>()
    every { Clerk.user } returns user
    val error = ClerkErrorResponse(errors = listOf(Error(longMessage = "bad")))
    coEvery { user.createEmailAddress("user@example.com") } returns ClerkResult.Failure(error)

    val viewModel = AddEmailViewModel()
    viewModel.state.test {
      assertEquals(AddEmailViewModel.State.Idle, awaitItem())
      viewModel.addEmail("user@example.com")
      assertEquals(AddEmailViewModel.State.Loading, awaitItem())
      assertEquals(AddEmailViewModel.State.Error("bad"), awaitItem())
    }
  }

  @Test
  fun addEmail_withoutUser_emitsGuardError() = runTest {
    every { Clerk.user } returns null

    val viewModel = AddEmailViewModel()
    viewModel.state.test {
      assertEquals(AddEmailViewModel.State.Idle, awaitItem())
      viewModel.addEmail("user@example.com")
      assertEquals(AddEmailViewModel.State.Loading, awaitItem())
      assertEquals(AddEmailViewModel.State.Error("No current user found"), awaitItem())
    }
  }
}
