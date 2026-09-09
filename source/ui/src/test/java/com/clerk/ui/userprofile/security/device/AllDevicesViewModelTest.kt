package com.clerk.ui.userprofile.security.device

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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class AllDevicesViewModelTest {
  private val clerk = mockk<Clerk>(relaxed = true)

  @get:org.junit.Rule val dispatcherRule = MainDispatcherRule()

  @BeforeTest
  fun setUp() {
    every { clerk.user } returns null
    every { clerk.session } returns null
  }

  @AfterTest
  fun tearDown() {

    unmockkAll()
  }

  @Test
  fun activeSessions_success_sortsDevices() = runTest {
    val user = mockk<User>()
    val currentSession = session(id = "current", lastActiveAt = 100L)
    val otherRecent = session(id = "other", lastActiveAt = 200L)
    val older = session(id = "older", lastActiveAt = 50L)

    every { clerk.user } returns user
    every { clerk.session } returns mockSession(id = "current")
    every { clerk.session } returns mockSession(id = "current")
    coEvery { user.getSessions() } returns listOf(older, otherRecent, currentSession)

    val viewModel = AllDevicesViewModel(clerk)
    viewModel.state.test {
      var item = awaitItem()
      if (item is AllDevicesViewModel.State.Idle) item = awaitItem()
      if (item is AllDevicesViewModel.State.Loading) item = awaitItem()
      val successState = item as AllDevicesViewModel.State.Success
      assertEquals(listOf(currentSession, otherRecent, older), successState.devices)
    }
  }

  @Test
  fun activeSessions_failure_setsErrorState() = runTest {
    val user = mockk<User>()
    every { clerk.user } returns user
    val error = testCoreError("bad")
    coEvery { user.getSessions() } throws error

    val viewModel = AllDevicesViewModel(clerk)
    viewModel.state.test {
      var item = awaitItem()
      if (item is AllDevicesViewModel.State.Idle) item = awaitItem()
      if (item is AllDevicesViewModel.State.Loading) item = awaitItem()
      assertEquals(AllDevicesViewModel.State.Error("bad"), item)
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
