package com.clerk.ui.userprofile.security.passkey

import app.cash.turbine.test
import com.clerk.api.*
import com.clerk.api.Clerk
import com.clerk.api.DeletedObject
import com.clerk.api.Passkey
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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class UserProfilePasskeyViewModelTest {
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
  fun deletePasskey_success_setsSuccessState() = runTest {
    val passkey = mockk<Passkey>()
    coEvery { passkey.delete() } returns mockk<DeletedObject>()

    val viewModel = UserProfilePasskeyViewModel(clerk)
    viewModel.state.test {
      assertEquals(UserProfilePasskeyViewModel.State.Idle, awaitItem())
      viewModel.deletePasskey(passkey)
      assertEquals(UserProfilePasskeyViewModel.State.Loading, awaitItem())
      assertEquals(UserProfilePasskeyViewModel.State.Success, awaitItem())
    }
  }

  @Test
  fun deletePasskey_failure_setsErrorState() = runTest {
    val passkey = mockk<Passkey>()
    val error = testCoreError("bad")
    coEvery { passkey.delete() } throws error

    val viewModel = UserProfilePasskeyViewModel(clerk)
    viewModel.state.test {
      assertEquals(UserProfilePasskeyViewModel.State.Idle, awaitItem())
      viewModel.deletePasskey(passkey)
      assertEquals(UserProfilePasskeyViewModel.State.Loading, awaitItem())
      assertEquals(UserProfilePasskeyViewModel.State.Error("bad"), awaitItem())
    }
  }

  @Test
  fun createPasskey_success_setsSuccessState() = runTest {
    val user = mockk<User>()
    every { clerk.user } returns user
    coEvery { user.createPasskey() } returns mockk()

    val viewModel = UserProfilePasskeyViewModel(clerk)
    viewModel.state.test {
      // No explicit Loading for createPasskey; just assert success eventually
      awaitItem() // initial Idle
      viewModel.createPasskey()
      advanceUntilIdle()
      assertEquals(UserProfilePasskeyViewModel.State.Success, awaitItem())
    }
  }

  @Test
  fun createPasskey_withoutUser_setsErrorState() = runTest {
    every { clerk.user } returns null

    val viewModel = UserProfilePasskeyViewModel(clerk)
    viewModel.state.test {
      awaitItem() // initial Idle
      viewModel.createPasskey()
      assertEquals(UserProfilePasskeyViewModel.State.Error("User does not exist"), awaitItem())
    }
  }

  @Test
  fun createPasskey_cancellation_resetsToIdle() = runTest {
    val user = mockk<User>()
    every { clerk.user } returns user
    coEvery { user.createPasskey() } throws CoreException("user_cancelled")

    val viewModel = UserProfilePasskeyViewModel(clerk)
    viewModel.state.test {
      assertEquals(UserProfilePasskeyViewModel.State.Idle, awaitItem())
      viewModel.createPasskey()
      advanceUntilIdle()
      expectNoEvents()
    }
  }

  @Test
  fun createPasskey_missingActivity_surfacesRetryMessage() = runTest {
    val user = mockk<User>()
    every { clerk.user } returns user
    coEvery { user.createPasskey() } throws
      CoreException(
        "missing_activity",
        "Authentication requires an active screen. Try again from the app.",
      )

    val viewModel = UserProfilePasskeyViewModel(clerk)
    viewModel.state.test {
      assertEquals(UserProfilePasskeyViewModel.State.Idle, awaitItem())
      viewModel.createPasskey()
      assertEquals(
        UserProfilePasskeyViewModel.State.Error(
          "Authentication requires an active screen. Try again from the app."
        ),
        awaitItem(),
      )
    }
  }
}
