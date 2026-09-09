package com.clerk.ui.userprofile.update

import app.cash.turbine.test
import com.clerk.api.*
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
class UpdateProfileViewModelTest {
  private val clerk = mockk<Clerk>(relaxed = true)

  @get:org.junit.Rule val temporaryFolder = org.junit.rules.TemporaryFolder()

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
  fun removeProfileImage_success_updatesState() = runTest {
    val user = mockk<User>()
    every { clerk.user } returns user
    coEvery { user.setProfileImage(SetProfileImageParams(file = null)) } returns mockk()
    coEvery { user.reload() } returns user

    val viewModel = UpdateProfileViewModel(clerk)
    viewModel.state.test {
      assertEquals(UpdateProfileViewModel.State.Idle, awaitItem())
      viewModel.removeProfileImage()
      assertEquals(UpdateProfileViewModel.State.Loading, awaitItem())
      assertEquals(UpdateProfileViewModel.State.Success, awaitItem())
    }
  }

  @Test
  fun removeProfileImage_failure_emitsError() = runTest {
    val user = mockk<User>()
    every { clerk.user } returns user
    val error = testCoreError("boom")
    coEvery { user.setProfileImage(SetProfileImageParams(file = null)) } throws error

    val viewModel = UpdateProfileViewModel(clerk)
    viewModel.state.test {
      assertEquals(UpdateProfileViewModel.State.Idle, awaitItem())
      viewModel.removeProfileImage()
      assertEquals(UpdateProfileViewModel.State.Loading, awaitItem())
      assertEquals(
        UpdateProfileViewModel.State.Error("Failed to delete profile image: boom"),
        awaitItem(),
      )
    }
  }

  @Test
  fun uploadProfileImage_success_emitsSuccess() = runTest {
    val user = mockk<User>()
    every { clerk.user } returns user
    coEvery { user.setProfileImage(any()) } returns mockk()
    coEvery { user.reload() } returns user

    val viewModel = UpdateProfileViewModel(clerk)
    viewModel.state.test {
      assertEquals(UpdateProfileViewModel.State.Idle, awaitItem())
      viewModel.uploadProfileImage(
        temporaryFolder.newFile("avatar.png").apply { writeBytes(byteArrayOf(1, 2, 3)) }
      )
      assertEquals(UpdateProfileViewModel.State.Loading, awaitItem())
      assertEquals(UpdateProfileViewModel.State.Success, awaitItem())
    }
  }

  @Test
  fun save_success_setsSuccessState() = runTest {
    val user = mockk<User>()
    every { clerk.user } returns user
    coEvery { user.update(any<UpdateUserParams>()) } returns user
    coEvery { user.reload() } returns user

    val viewModel = UpdateProfileViewModel(clerk)
    viewModel.state.test {
      assertEquals(UpdateProfileViewModel.State.Idle, awaitItem())
      viewModel.save("Jane", "Doe", "jane")
      assertEquals(UpdateProfileViewModel.State.Loading, awaitItem())
      assertEquals(UpdateProfileViewModel.State.Success, awaitItem())
    }
  }

  @Test
  fun removeProfileImage_withoutUser_emitsAuthenticationError() = runTest {
    every { clerk.user } returns null

    val viewModel = UpdateProfileViewModel(clerk)
    viewModel.state.test {
      assertEquals(UpdateProfileViewModel.State.Idle, awaitItem())
      viewModel.removeProfileImage()
      // When user is null, guard emits Error without going through Loading
      assertEquals(UpdateProfileViewModel.State.Error("User not authenticated"), awaitItem())
    }
  }
}
