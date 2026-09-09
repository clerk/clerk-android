package com.clerk.ui.userprofile.security.device

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
class DeviceViewModelTest {
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
  fun signOut_success_setsSuccessState() = runTest {
    val session = mockk<SessionWithActivities>()
    val user = mockk<User>()
    every { clerk.user } returns user
    coEvery { session.revoke() } returns session
    coEvery { user.getSessions() } returns emptyList()

    val viewModel = DeviceViewModel(clerk)
    viewModel.state.test {
      assertEquals(DeviceViewModel.State.Idle, awaitItem())
      viewModel.signOut(session)
      assertEquals(DeviceViewModel.State.Loading, awaitItem())
      assertEquals(DeviceViewModel.State.Success, awaitItem())
    }
  }

  @Test
  fun signOut_failure_setsErrorState() = runTest {
    val session = mockk<SessionWithActivities>()
    val error = testCoreError("no")
    coEvery { session.revoke() } throws error

    val viewModel = DeviceViewModel(clerk)
    viewModel.state.test {
      assertEquals(DeviceViewModel.State.Idle, awaitItem())
      viewModel.signOut(session)
      assertEquals(DeviceViewModel.State.Loading, awaitItem())
      assertEquals(DeviceViewModel.State.Error("no"), awaitItem())
    }
  }
}
