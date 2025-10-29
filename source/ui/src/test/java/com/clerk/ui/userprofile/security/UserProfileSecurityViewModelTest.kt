package com.clerk.ui.userprofile.security

import app.cash.turbine.test
import com.clerk.api.Clerk
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.Session
import com.clerk.api.user.User
import com.clerk.api.user.allSessions
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
class UserProfileSecurityViewModelTest {

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
  fun loadSessions_success_setsSuccessState() = runTest {
    val user = mockk<User>()
    val sessions = listOf(mockk<Session>(), mockk())
    every { Clerk.user } returns user
    coEvery { user.allSessions() } returns ClerkResult.success(sessions)

    val viewModel = UserProfileSecurityViewModel()
    viewModel.state.test {
      var item = awaitItem()
      if (item is UserProfileSecurityViewModel.State.Idle) item = awaitItem()
      if (item is UserProfileSecurityViewModel.State.Loading) item = awaitItem()
      assertEquals(UserProfileSecurityViewModel.State.Success(sessions), item)
    }
  }

  @Test
  fun loadSessions_failure_setsErrorState() = runTest {
    val user = mockk<User>()
    every { Clerk.user } returns user
    val error = ClerkErrorResponse(errors = listOf(Error(longMessage = "fail")))
    coEvery { user.allSessions() } returns ClerkResult.Failure(error)

    val viewModel = UserProfileSecurityViewModel()
    viewModel.state.test {
      var item = awaitItem()
      if (item is UserProfileSecurityViewModel.State.Idle) item = awaitItem()
      if (item is UserProfileSecurityViewModel.State.Loading) item = awaitItem()
      assertEquals(UserProfileSecurityViewModel.State.Error("fail"), item)
    }
  }
}
