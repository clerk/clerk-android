package com.clerk.ui.userprofile.email

import com.clerk.api.Clerk
import com.clerk.api.emailaddress.EmailAddress
import com.clerk.api.emailaddress.delete
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.user.User
import com.clerk.api.user.User.UpdateParams
import com.clerk.api.user.update
import com.clerk.ui.userprofile.MainDispatcherRule
import io.mockk.any
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
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
class EmailViewModelTest {

  @get:org.junit.Rule val dispatcherRule = MainDispatcherRule()

  @BeforeTest
  fun setUp() {
    mockkObject(Clerk)
    every { Clerk.user } returns null
    mockkStatic("com.clerk.api.user.UserKt")
    mockkStatic("com.clerk.api.emailaddress.EmailAddressKt")
  }

  @AfterTest
  fun tearDown() {
    unmockkStatic("com.clerk.api.emailaddress.EmailAddressKt")
    unmockkStatic("com.clerk.api.user.UserKt")
    unmockkAll()
  }

  @Test
  fun setAsPrimary_success_emitsSuccessState() = runTest {
    val user = mockk<User>()
    val email = mockk<EmailAddress>(relaxed = true)
    every { Clerk.user } returns user
    coEvery { user.update(any<UpdateParams>()) } returns ClerkResult.success(user)

    val viewModel = EmailViewModel()

    viewModel.setAsPrimary(email)
    advanceUntilIdle()

    assertEquals(EmailViewModel.State.SetAsPrimary.Success, viewModel.state.value)
  }

  @Test
  fun setAsPrimary_failure_emitsFailureState() = runTest {
    val user = mockk<User>()
    val email = mockk<EmailAddress>(relaxed = true)
    every { Clerk.user } returns user
    val error = ClerkErrorResponse(errors = listOf(ClerkErrorResponse.Error(longMessage = "oops")))
    coEvery { user.update(any<UpdateParams>()) } returns ClerkResult.Failure(error)

    val viewModel = EmailViewModel()

    viewModel.setAsPrimary(email)
    advanceUntilIdle()

    assertEquals(EmailViewModel.State.Failure("oops"), viewModel.state.value)
  }

  @Test
  fun remove_success_emitsRemoveSuccessState() = runTest {
    val email = mockk<EmailAddress>()
    coEvery { email.delete() } returns ClerkResult.success(mockk())

    val viewModel = EmailViewModel()

    viewModel.remove(email)
    advanceUntilIdle()

    assertEquals(EmailViewModel.State.Remove.Success, viewModel.state.value)
  }

  @Test
  fun remove_failure_emitsFailureState() = runTest {
    val email = mockk<EmailAddress>()
    val error = ClerkErrorResponse(errors = listOf(ClerkErrorResponse.Error(longMessage = "remove")))
    coEvery { email.delete() } returns ClerkResult.Failure(error)

    val viewModel = EmailViewModel()

    viewModel.remove(email)
    advanceUntilIdle()

    assertEquals(EmailViewModel.State.Failure("remove"), viewModel.state.value)
  }
}
