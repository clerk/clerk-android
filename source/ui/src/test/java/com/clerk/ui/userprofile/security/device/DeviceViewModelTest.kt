package com.clerk.ui.userprofile.security.device

import com.clerk.api.Clerk
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.Session
import com.clerk.api.session.revoke
import com.clerk.api.user.User
import com.clerk.api.user.allSessions
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
class DeviceViewModelTest {

  @get:org.junit.Rule val dispatcherRule = MainDispatcherRule()

  @BeforeTest
  fun setUp() {
    mockkObject(Clerk)
    every { Clerk.user } returns null
    mockkStatic("com.clerk.api.session.SessionKt")
    mockkStatic("com.clerk.api.user.UserKt")
  }

  @AfterTest
  fun tearDown() {
    unmockkStatic("com.clerk.api.user.UserKt")
    unmockkStatic("com.clerk.api.session.SessionKt")
    unmockkAll()
  }

  @Test
  fun signOut_success_setsSuccessState() = runTest {
    val session = mockk<Session>()
    val user = mockk<User>()
    every { Clerk.user } returns user
    coEvery { session.revoke() } returns ClerkResult.success(session)
    coEvery { user.allSessions() } returns ClerkResult.success(emptyList())

    val viewModel = DeviceViewModel()

    viewModel.signOut(session)
    advanceUntilIdle()

    assertEquals(DeviceViewModel.State.Success, viewModel.state.value)
  }

  @Test
  fun signOut_failure_setsErrorState() = runTest {
    val session = mockk<Session>()
    val error = ClerkErrorResponse(errors = listOf(ClerkErrorResponse.Error(longMessage = "no")))
    coEvery { session.revoke() } returns ClerkResult.Failure(error)

    val viewModel = DeviceViewModel()

    viewModel.signOut(session)
    advanceUntilIdle()

    assertEquals(DeviceViewModel.State.Error("no"), viewModel.state.value)
  }
}
