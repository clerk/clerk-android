package com.clerk.ui.userprofile.totp

import com.clerk.api.Clerk
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.model.totp.TOTPResource
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.user.User
import com.clerk.api.user.createTotp
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
class UserProfileMfaTotpViewModelTest {

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
  fun createTotp_success_setsSuccessState() = runTest {
    val user = mockk<User>()
    val totpResource = mockk<TOTPResource>()
    every { Clerk.user } returns user
    coEvery { user.createTotp() } returns ClerkResult.success(totpResource)

    val viewModel = UserProfileMfaTotpViewModel()
    advanceUntilIdle()

    assertEquals(UserProfileMfaTotpViewModel.State.Success(totpResource), viewModel.state.value)
  }

  @Test
  fun createTotp_failure_setsErrorState() = runTest {
    val user = mockk<User>()
    val error = ClerkErrorResponse(errors = listOf(Error(longMessage = "fail")))
    every { Clerk.user } returns user
    coEvery { user.createTotp() } returns ClerkResult.Failure(error)

    val viewModel = UserProfileMfaTotpViewModel()
    advanceUntilIdle()

    assertEquals(UserProfileMfaTotpViewModel.State.Error("fail"), viewModel.state.value)
  }

  @Test
  fun createTotp_withoutUser_setsErrorState() = runTest {
    val viewModel = UserProfileMfaTotpViewModel()
    advanceUntilIdle()

    assertEquals(
      UserProfileMfaTotpViewModel.State.Error("User does not exist"),
      viewModel.state.value,
    )
  }
}
