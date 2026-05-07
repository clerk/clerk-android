package com.clerk.ui.userprofile.account

import com.clerk.api.Clerk
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.Session
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class UserProfileAccountViewModelTest {

  @BeforeTest
  fun setUp() {
    mockkObject(Clerk)
    // Ensure launched coroutines run immediately for this simple verification
    kotlinx.coroutines.Dispatchers.setMain(UnconfinedTestDispatcher())
  }

  @AfterTest
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun signOut_invokesClerkSignOutForCurrentSession() = runTest {
    val session = mockk<Session>()
    every { session.id } returns "sess_123"
    every { Clerk.session } returns session
    coEvery { Clerk.auth.signOut(sessionId = "sess_123") } returns ClerkResult.success(Unit)

    val viewModel = UserProfileAccountViewModel()

    viewModel.signOut()
    advanceUntilIdle()

    coVerify { Clerk.auth.signOut(sessionId = "sess_123") }
  }
}
