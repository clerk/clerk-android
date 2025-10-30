package com.clerk.ui.userprofile.update

import app.cash.turbine.test
import com.clerk.api.Clerk
import com.clerk.api.network.model.deleted.DeletedObject
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.user.User
import com.clerk.api.user.User.UpdateParams
import com.clerk.api.user.deleteProfileImage
import com.clerk.api.user.get
import com.clerk.api.user.setProfileImage
import com.clerk.api.user.update
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
class UpdateProfileViewModelTest {

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
  fun removeProfileImage_success_updatesState() = runTest {
    val user = mockk<User>()
    every { Clerk.user } returns user
    coEvery { user.deleteProfileImage() } returns ClerkResult.success(mockk<DeletedObject>())
    coEvery { user.get() } returns ClerkResult.success(user)

    val viewModel = UpdateProfileViewModel()
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
    every { Clerk.user } returns user
    val error = ClerkErrorResponse(errors = listOf(Error(longMessage = "boom")))
    coEvery { user.deleteProfileImage() } returns ClerkResult.Failure(error)

    val viewModel = UpdateProfileViewModel()
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
    every { Clerk.user } returns user
    coEvery { user.setProfileImage(any()) } returns ClerkResult.success(mockk())
    coEvery { user.get() } returns ClerkResult.success(user)

    val viewModel = UpdateProfileViewModel()
    viewModel.state.test {
      assertEquals(UpdateProfileViewModel.State.Idle, awaitItem())
      viewModel.uploadProfileImage(mockk())
      assertEquals(UpdateProfileViewModel.State.Loading, awaitItem())
      assertEquals(UpdateProfileViewModel.State.Success, awaitItem())
    }
  }

  @Test
  fun save_success_setsSuccessState() = runTest {
    val user = mockk<User>()
    every { Clerk.user } returns user
    coEvery { user.update(any<UpdateParams>()) } returns ClerkResult.success(user)
    coEvery { user.get() } returns ClerkResult.success(user)

    val viewModel = UpdateProfileViewModel()
    viewModel.state.test {
      assertEquals(UpdateProfileViewModel.State.Idle, awaitItem())
      viewModel.save("Jane", "Doe", "jane")
      assertEquals(UpdateProfileViewModel.State.Loading, awaitItem())
      assertEquals(UpdateProfileViewModel.State.Success, awaitItem())
    }
  }

  @Test
  fun removeProfileImage_withoutUser_emitsAuthenticationError() = runTest {
    every { Clerk.user } returns null

    val viewModel = UpdateProfileViewModel()
    viewModel.state.test {
      assertEquals(UpdateProfileViewModel.State.Idle, awaitItem())
      viewModel.removeProfileImage()
      // When user is null, guard emits Error without going through Loading
      assertEquals(UpdateProfileViewModel.State.Error("User not authenticated"), awaitItem())
    }
  }
}
