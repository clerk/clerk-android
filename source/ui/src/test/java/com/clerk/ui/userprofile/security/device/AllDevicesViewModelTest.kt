package com.clerk.ui.userprofile.security.device

import app.cash.turbine.test
import com.clerk.api.Clerk
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.Session
import com.clerk.api.session.Session.SessionStatus
import com.clerk.api.session.SessionActivity
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
class AllDevicesViewModelTest {

  @get:org.junit.Rule val dispatcherRule = MainDispatcherRule()

  @BeforeTest
  fun setUp() {
    mockkObject(Clerk)
    every { Clerk.user } returns null
    every { Clerk.session } returns null
    mockkStatic("com.clerk.api.user.UserKt")
  }

  @AfterTest
  fun tearDown() {
    unmockkStatic("com.clerk.api.user.UserKt")
    unmockkAll()
  }

  @Test
  fun allSessions_success_sortsDevices() = runTest {
    val user = mockk<User>()
    val currentSession = session(id = "current", lastActiveAt = 100L)
    val otherRecent = session(id = "other", lastActiveAt = 200L)
    val older = session(id = "older", lastActiveAt = 50L)

    every { Clerk.user } returns user
    every { Clerk.session } returns currentSession
    coEvery { user.allSessions() } returns
      ClerkResult.success(listOf(older, otherRecent, currentSession))

    val viewModel = AllDevicesViewModel()
    viewModel.state.test {
      var item = awaitItem()
      if (item is AllDevicesViewModel.State.Idle) item = awaitItem()
      if (item is AllDevicesViewModel.State.Loading) item = awaitItem()
      val successState = item as AllDevicesViewModel.State.Success
      assertEquals(listOf(currentSession, otherRecent, older), successState.devices)
    }
  }

  @Test
  fun allSessions_failure_setsErrorState() = runTest {
    val user = mockk<User>()
    every { Clerk.user } returns user
    val error = ClerkErrorResponse(errors = listOf(Error(longMessage = "bad")))
    coEvery { user.allSessions() } returns ClerkResult.Failure(error)

    val viewModel = AllDevicesViewModel()
    viewModel.state.test {
      var item = awaitItem()
      if (item is AllDevicesViewModel.State.Idle) item = awaitItem()
      if (item is AllDevicesViewModel.State.Loading) item = awaitItem()
      assertEquals(AllDevicesViewModel.State.Error("bad"), item)
    }
  }

  private fun session(id: String, lastActiveAt: Long): Session =
    Session(
      id = id,
      status = SessionStatus.ACTIVE,
      expireAt = 0L,
      abandonAt = null,
      lastActiveAt = lastActiveAt,
      latestActivity = SessionActivity(id = "activity-$id"),
      lastActiveOrganizationId = null,
      actor = null,
      user = null,
      publicUserData = null,
      factorVerificationAge = null,
      createdAt = 0L,
      updatedAt = 0L,
      tasks = emptyList(),
      lastActiveToken = null,
    )
}
