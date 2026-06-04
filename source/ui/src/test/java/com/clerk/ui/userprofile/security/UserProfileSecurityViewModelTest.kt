package com.clerk.ui.userprofile.security

import app.cash.turbine.test
import com.clerk.api.Clerk
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.Session
import com.clerk.api.session.Session.SessionStatus
import com.clerk.api.session.SessionActivity
import com.clerk.api.session.isThisDevice
import com.clerk.api.user.User
import com.clerk.api.user.activeSessions
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
import kotlin.test.assertIs
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
    mockkStatic("com.clerk.api.session.SessionKt")
  }

  @AfterTest
  fun tearDown() {
    unmockkStatic("com.clerk.api.user.UserKt")
    unmockkStatic("com.clerk.api.session.SessionKt")
    unmockkAll()
  }

  @Test
  fun loadSessions_success_setsSuccessState() = runTest {
    val user = mockk<User>()
    val sessions =
      listOf(
        session(id = "first", lastActiveAt = 100L),
        session(id = "second", lastActiveAt = 200L),
      )
    every { Clerk.user } returns user
    every { sessions[0].isThisDevice } returns false
    every { sessions[1].isThisDevice } returns false
    coEvery { user.activeSessions() } returns ClerkResult.success(sessions)

    val viewModel = UserProfileSecurityViewModel()
    viewModel.state.test {
      var item = awaitItem()
      if (item is UserProfileSecurityViewModel.State.Idle) item = awaitItem()
      if (item is UserProfileSecurityViewModel.State.Loading) item = awaitItem()
      assertEquals(UserProfileSecurityViewModel.State.Success(sessions.reversed()), item)
    }
  }

  @Test
  fun loadSessions_success_sortsCurrentDeviceFirstThenLastActive() = runTest {
    val user = mockk<User>()
    val currentSession = session(id = "current", lastActiveAt = 100L)
    val otherRecent = session(id = "other", lastActiveAt = 200L)
    val older = session(id = "older", lastActiveAt = 50L)
    val sessionWithoutActivity =
      session(id = "missing-activity", lastActiveAt = 300L, hasActivity = false)
    every { Clerk.user } returns user
    every { currentSession.isThisDevice } returns true
    every { otherRecent.isThisDevice } returns false
    every { older.isThisDevice } returns false
    every { sessionWithoutActivity.isThisDevice } returns false
    coEvery { user.activeSessions() } returns
      ClerkResult.success(listOf(sessionWithoutActivity, older, otherRecent, currentSession))

    val viewModel = UserProfileSecurityViewModel()

    viewModel.state.test {
      var item = awaitItem()
      if (item is UserProfileSecurityViewModel.State.Idle) item = awaitItem()
      if (item is UserProfileSecurityViewModel.State.Loading) item = awaitItem()
      val successState = assertIs<UserProfileSecurityViewModel.State.Success>(item)
      assertEquals(listOf(currentSession, otherRecent, older), successState.sessions)
    }
  }

  @Test
  fun loadSessions_failure_setsErrorState() = runTest {
    val user = mockk<User>()
    every { Clerk.user } returns user
    val error = ClerkErrorResponse(errors = listOf(Error(longMessage = "fail")))
    coEvery { user.activeSessions() } returns ClerkResult.Failure(error)

    val viewModel = UserProfileSecurityViewModel()
    viewModel.state.test {
      var item = awaitItem()
      if (item is UserProfileSecurityViewModel.State.Idle) item = awaitItem()
      if (item is UserProfileSecurityViewModel.State.Loading) item = awaitItem()
      assertEquals(UserProfileSecurityViewModel.State.Error("fail"), item)
    }
  }

  private fun session(id: String, lastActiveAt: Long, hasActivity: Boolean = true): Session =
    Session(
      id = id,
      status = SessionStatus.ACTIVE,
      expireAt = 0L,
      abandonAt = null,
      lastActiveAt = lastActiveAt,
      latestActivity = if (hasActivity) SessionActivity(id = "activity-$id") else null,
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
