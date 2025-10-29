package com.clerk.ui.userprofile.account

import com.clerk.api.Clerk
import com.clerk.api.network.serialization.ClerkResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class UserProfileAccountViewModelTest {

  @BeforeTest
  fun setUp() {
    mockkObject(Clerk)
  }

  @AfterTest
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun signOut_invokesClerkSignOut() = runTest {
    coEvery { Clerk.signOut() } returns ClerkResult.success(Unit)

    val viewModel = UserProfileAccountViewModel()

    viewModel.signOut()

    coVerify { Clerk.signOut() }
  }
}
