package com.clerk.ui.userprofile.account

import com.clerk.api.Clerk
import com.clerk.api.biometriccredential.BiometricCredentials
import com.clerk.api.network.model.deleted.DeletedObject
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.Session
import com.clerk.api.user.User
import com.clerk.api.user.delete
import com.clerk.ui.userprofile.MainDispatcherRule
import com.clerk.ui.userprofile.account.UserProfileAccountViewModel.DeleteAccountState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class UserProfileAccountViewModelTest {

  private val testDispatcher = UnconfinedTestDispatcher()

  @get:org.junit.Rule val dispatcherRule = MainDispatcherRule(testDispatcher)

  @BeforeTest
  fun setUp() {
    mockkObject(Clerk)
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

  @Test
  fun signOut_doesNothingWhenCurrentSessionIsNull() = runTest {
    every { Clerk.session } returns null
    coEvery { Clerk.auth.signOut(sessionId = any()) } returns ClerkResult.success(Unit)
    coEvery { Clerk.auth.signOut(sessionId = null) } returns ClerkResult.success(Unit)

    val viewModel = UserProfileAccountViewModel()

    viewModel.signOut()
    advanceUntilIdle()

    // Must not collapse to signOut(null) / signOut() — that would sign out every account.
    coVerify(exactly = 0) { Clerk.auth.signOut(sessionId = any()) }
    coVerify(exactly = 0) { Clerk.auth.signOut(sessionId = null) }
  }

  @Test
  fun deleteAccount_success_forgetsLocalBiometricCredentials() = runTest {
    givenSignedInUser(deleteResult = ClerkResult.success(mockk()))
    every { BiometricCredentials.forgetLocalCredentialsAfterAccountDeletion("user_1") } returns 1

    val viewModel = UserProfileAccountViewModel(workDispatcher = testDispatcher)
    viewModel.deleteAccount()
    advanceUntilIdle()

    assertEquals(DeleteAccountState.Success, viewModel.deleteAccountStateFlow.value)
    verify(exactly = 1) {
      BiometricCredentials.forgetLocalCredentialsAfterAccountDeletion("user_1")
    }
  }

  @Test
  fun deleteAccount_cleanupFailure_stillSetsSuccessState() = runTest {
    givenSignedInUser(deleteResult = ClerkResult.success(mockk()))
    every { BiometricCredentials.forgetLocalCredentialsAfterAccountDeletion("user_1") } throws
      IllegalStateException("cleanup failed")

    val viewModel = UserProfileAccountViewModel(workDispatcher = testDispatcher)
    viewModel.deleteAccount()
    advanceUntilIdle()

    assertEquals(DeleteAccountState.Success, viewModel.deleteAccountStateFlow.value)
    verify(exactly = 1) {
      BiometricCredentials.forgetLocalCredentialsAfterAccountDeletion("user_1")
    }
  }

  @Test
  fun deleteAccount_failure_setsErrorStateAndKeepsLocalBiometricCredentials() = runTest {
    val error = ClerkErrorResponse(errors = listOf(Error(longMessage = "boom")))
    givenSignedInUser(deleteResult = ClerkResult.Failure(error))
    every { BiometricCredentials.forgetLocalCredentialsAfterAccountDeletion(any()) } returns 1

    val viewModel = UserProfileAccountViewModel(workDispatcher = testDispatcher)
    viewModel.deleteAccount()
    advanceUntilIdle()

    assertEquals(DeleteAccountState.Error("boom"), viewModel.deleteAccountStateFlow.value)
    verify(exactly = 0) { BiometricCredentials.forgetLocalCredentialsAfterAccountDeletion(any()) }
  }

  private fun givenSignedInUser(deleteResult: ClerkResult<DeletedObject, ClerkErrorResponse>) {
    val user = mockk<User>()
    every { user.id } returns "user_1"
    every { Clerk.user } returns user
    mockkStatic("com.clerk.api.user.UserKt")
    coEvery { user.delete() } returns deleteResult
    mockkObject(BiometricCredentials)
  }
}
