package com.clerk.ui.userprofile.security

import app.cash.turbine.test
import com.clerk.api.*
import com.clerk.testing.mockSession
import com.clerk.testing.testCoreError
import com.clerk.ui.userprofile.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import java.time.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class UserProfileSecurityViewModelTest {
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
  fun loadSessions_success_setsSuccessState() = runTest {
    val user = mockk<User>()
    val sessions =
      listOf(
        session(id = "first", lastActiveAt = 100L),
        session(id = "second", lastActiveAt = 200L),
      )
    every { clerk.user } returns user
    coEvery { user.getSessions() } returns sessions

    val viewModel = UserProfileSecurityViewModel(clerk)
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
    every { clerk.user } returns user
    every { clerk.session } returns mockSession(id = "current")
    coEvery { user.getSessions() } returns listOf(older, otherRecent, currentSession)

    val viewModel = UserProfileSecurityViewModel(clerk)

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
    every { clerk.user } returns user
    val error = testCoreError("fail")
    coEvery { user.getSessions() } throws error

    val viewModel = UserProfileSecurityViewModel(clerk)
    viewModel.state.test {
      var item = awaitItem()
      if (item is UserProfileSecurityViewModel.State.Idle) item = awaitItem()
      if (item is UserProfileSecurityViewModel.State.Loading) item = awaitItem()
      assertEquals(UserProfileSecurityViewModel.State.Error("fail"), item)
    }
  }

  private fun session(
    id: String,
    lastActiveAt: Long,
    hasActivity: Boolean = true,
  ): SessionWithActivities {
    val session = mockk<SessionWithActivities>(relaxed = true)
    every { session.id } returns id
    every { session.status } returns "active"
    every { session.lastActiveAt } returns Instant.ofEpochMilli(lastActiveAt)
    every { session.latestActivity } returns mockk<SessionActivity>(relaxed = true)
    return session
  }
}
