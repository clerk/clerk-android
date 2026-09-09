package com.clerk.ui.userprofile.email

import app.cash.turbine.test
import com.clerk.api.CreateEmailAddressParams
import com.clerk.api.EmailAddress
import com.clerk.api.User
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
class AddEmailViewModelTest {
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
  fun addEmail_success_emitsSuccessState() = runTest {
    val user = mockk<User>()
    every { clerk.user } returns user
    val emailAddress = mockk<EmailAddress>()
    coEvery { user.createEmailAddress(CreateEmailAddressParams("user@example.com")) } returns
      emailAddress

    val viewModel = AddEmailViewModel(clerk)
    viewModel.state.test {
      assertEquals(AddEmailViewModel.State.Idle, awaitItem())
      viewModel.addEmail("user@example.com")
      assertEquals(AddEmailViewModel.State.Loading, awaitItem())
      assertEquals(AddEmailViewModel.State.Success(emailAddress), awaitItem())
    }
  }

  @Test
  fun addEmail_failure_emitsErrorState() = runTest {
    val user = mockk<User>()
    every { clerk.user } returns user
    val error = testCoreError("bad")
    coEvery { user.createEmailAddress(CreateEmailAddressParams("user@example.com")) } throws error

    val viewModel = AddEmailViewModel(clerk)
    viewModel.state.test {
      assertEquals(AddEmailViewModel.State.Idle, awaitItem())
      viewModel.addEmail("user@example.com")
      assertEquals(AddEmailViewModel.State.Loading, awaitItem())
      assertEquals(AddEmailViewModel.State.Error("bad"), awaitItem())
    }
  }

  @Test
  fun addEmail_withoutUser_emitsGuardError() = runTest {
    every { clerk.user } returns null

    val viewModel = AddEmailViewModel(clerk)
    viewModel.state.test {
      assertEquals(AddEmailViewModel.State.Idle, awaitItem())
      viewModel.addEmail("user@example.com")
      assertEquals(AddEmailViewModel.State.Loading, awaitItem())
      assertEquals(AddEmailViewModel.State.Error("No current user found"), awaitItem())
    }
  }

  @Test
  fun addEmail_whenEmailIsImmutable_emitsErrorWithoutCallingApi() = runTest {
    every { clerk.environment.userSettings.attributes } returns
      mapOf("email_address" to mockk(relaxed = true) { every { immutable } returns true })

    val viewModel = AddEmailViewModel(clerk)
    viewModel.state.test {
      assertEquals(AddEmailViewModel.State.Idle, awaitItem())
      viewModel.addEmail("user@example.com")
      assertEquals(
        AddEmailViewModel.State.Error("Email addresses cannot be changed for this application."),
        awaitItem(),
      )
    }
  }
}
