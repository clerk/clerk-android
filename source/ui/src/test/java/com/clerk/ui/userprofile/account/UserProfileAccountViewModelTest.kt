package com.clerk.ui.userprofile.account

import com.clerk.api.Clerk
import com.clerk.api.MobileSignOutOptions
import com.clerk.api.Session
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class UserProfileAccountViewModelTest {
  private val clerk = mockk<Clerk>(relaxed = true)

  @BeforeTest
  fun setUp() {
    // Ensure launched coroutines run immediately for this simple verification
    kotlinx.coroutines.Dispatchers.setMain(UnconfinedTestDispatcher())
  }

  @AfterTest
  fun tearDown() {
    kotlinx.coroutines.Dispatchers.resetMain()
    unmockkAll()
  }

  @Test
  fun signOut_invokesClerkSignOutForCurrentSession() = runTest {
    val session = mockk<Session>()
    every { session.id } returns "sess_123"
    every { clerk.session } returns session
    coEvery { clerk.signOut(MobileSignOutOptions("sess_123")) } returns Unit

    val viewModel = UserProfileAccountViewModel(clerk)

    viewModel.signOut()
    advanceUntilIdle()

    coVerify { clerk.signOut(MobileSignOutOptions("sess_123")) }
  }

  @Test
  fun signOut_doesNothingWhenCurrentSessionIsNull() = runTest {
    every { clerk.session } returns null
    coEvery { clerk.signOut(any()) } returns Unit
    coEvery { clerk.signOut(null) } returns Unit

    val viewModel = UserProfileAccountViewModel(clerk)

    viewModel.signOut()
    advanceUntilIdle()

    // Must not collapse to signOut(null) / signOut() — that would sign out every account.
    coVerify(exactly = 0) { clerk.signOut(any()) }
    coVerify(exactly = 0) { clerk.signOut(null) }
  }
}
