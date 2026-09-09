package com.clerk.ui.userprofile.email

import app.cash.turbine.test
import com.clerk.api.*
import com.clerk.testing.mockClerk
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
class EmailViewModelTest {
  private val clerk = mockClerk()

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
  fun setAsPrimary_success_emitsSuccessState() = runTest {
    val user = mockk<User>()
    val email = mockk<EmailAddress>(relaxed = true)
    every { clerk.user } returns user
    coEvery { user.update(any<UpdateUserParams>()) } returns user

    val viewModel = EmailViewModel(clerk)
    viewModel.state.test {
      assertEquals(EmailViewModel.State.Idle, awaitItem())
      viewModel.setAsPrimary(email)
      assertEquals(EmailViewModel.State.Loading, awaitItem())
      assertEquals(EmailViewModel.State.SetAsPrimary.Success, awaitItem())
    }
  }

  @Test
  fun setAsPrimary_failure_emitsFailureState() = runTest {
    val user = mockk<User>()
    val email = mockk<EmailAddress>(relaxed = true)
    every { clerk.user } returns user
    val error = testCoreError("oops")
    coEvery { user.update(any<UpdateUserParams>()) } throws error

    val viewModel = EmailViewModel(clerk)
    viewModel.state.test {
      assertEquals(EmailViewModel.State.Idle, awaitItem())
      viewModel.setAsPrimary(email)
      assertEquals(EmailViewModel.State.Loading, awaitItem())
      assertEquals(EmailViewModel.State.Failure("oops"), awaitItem())
    }
  }

  @Test
  fun remove_success_emitsRemoveSuccessState() = runTest {
    val email = mockk<EmailAddress>()
    coEvery { email.destroy() } returns mockk()

    val viewModel = EmailViewModel(clerk)
    viewModel.state.test {
      assertEquals(EmailViewModel.State.Idle, awaitItem())
      viewModel.remove(email)
      assertEquals(EmailViewModel.State.Loading, awaitItem())
      assertEquals(EmailViewModel.State.Remove.Success, awaitItem())
    }
  }

  @Test
  fun remove_failure_emitsFailureState() = runTest {
    val email = mockk<EmailAddress>()
    val error = testCoreError("remove")
    coEvery { email.destroy() } throws error

    val viewModel = EmailViewModel(clerk)
    viewModel.state.test {
      assertEquals(EmailViewModel.State.Idle, awaitItem())
      viewModel.remove(email)
      assertEquals(EmailViewModel.State.Loading, awaitItem())
      assertEquals(EmailViewModel.State.Failure("remove"), awaitItem())
    }
  }
}
