package com.clerk.ui.userprofile.security.passkey

import com.clerk.api.Clerk
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.passkeys.Passkey
import com.clerk.api.passkeys.delete
import com.clerk.api.user.User
import com.clerk.api.user.createPasskey
import com.clerk.ui.userprofile.MainDispatcherRule
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
class UserProfilePasskeyViewModelTest {

  @get:org.junit.Rule val dispatcherRule = MainDispatcherRule()

  @BeforeTest
  fun setUp() {
    mockkObject(Clerk)
    every { Clerk.user } returns null
    mockkStatic("com.clerk.api.passkeys.PasskeyKt")
    mockkStatic("com.clerk.api.user.UserKt")
  }

  @AfterTest
  fun tearDown() {
    unmockkStatic("com.clerk.api.user.UserKt")
    unmockkStatic("com.clerk.api.passkeys.PasskeyKt")
    unmockkAll()
  }

  @Test
  fun deletePasskey_success_setsSuccessState() = runTest {
    val passkey = mockk<Passkey>()
    coEvery { passkey.delete() } returns ClerkResult.success(passkey)

    val viewModel = UserProfilePasskeyViewModel()

    viewModel.deletePasskey(passkey)
    advanceUntilIdle()

    assertEquals(UserProfilePasskeyViewModel.State.Success, viewModel.state.value)
  }

  @Test
  fun deletePasskey_failure_setsErrorState() = runTest {
    val passkey = mockk<Passkey>()
    val error = ClerkErrorResponse(errors = listOf(ClerkErrorResponse.Error(longMessage = "bad")))
    coEvery { passkey.delete() } returns ClerkResult.Failure(error)

    val viewModel = UserProfilePasskeyViewModel()

    viewModel.deletePasskey(passkey)
    advanceUntilIdle()

    assertEquals(UserProfilePasskeyViewModel.State.Error("bad"), viewModel.state.value)
  }

  @Test
  fun createPasskey_success_setsSuccessState() = runTest {
    val user = mockk<User>()
    every { Clerk.user } returns user
    coEvery { user.createPasskey() } returns ClerkResult.success(mockk())

    val viewModel = UserProfilePasskeyViewModel()

    viewModel.createPasskey()
    advanceUntilIdle()

    assertEquals(UserProfilePasskeyViewModel.State.Success, viewModel.state.value)
  }

  @Test
  fun createPasskey_failure_setsErrorState() = runTest {
    val user = mockk<User>()
    every { Clerk.user } returns user
    val error = ClerkErrorResponse(errors = listOf(ClerkErrorResponse.Error(longMessage = "boom")))
    coEvery { user.createPasskey() } returns ClerkResult.Failure(error)

    val viewModel = UserProfilePasskeyViewModel()

    viewModel.createPasskey()
    advanceUntilIdle()

    assertEquals(UserProfilePasskeyViewModel.State.Error("boom"), viewModel.state.value)
  }
}
