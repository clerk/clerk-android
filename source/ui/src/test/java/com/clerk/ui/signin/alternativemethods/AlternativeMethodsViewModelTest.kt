package com.clerk.ui.signin.alternativemethods

import com.clerk.api.*
import com.clerk.testing.*
import com.clerk.ui.auth.AuthenticationViewState
import com.clerk.ui.signin.authenticateWithRedirect
import com.clerk.ui.userprofile.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AlternativeMethodsViewModelTest {
  private val clerk = mockClerk()

  @get:Rule val dispatcherRule = MainDispatcherRule()

  @Before
  fun setUp() {
    mockkStatic("com.clerk.ui.signin.SignInRedirectKt")
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `OAuth cancellation leaves alternative methods idle`() = runTest {
    val signIn = mockSignIn(SignInStatus.NeedsFirstFactor)
    coEvery { authenticateWithRedirect(clerk, OAuthProvider.Github, true) } throws
      CoreException("user_cancelled")
    val viewModel = AlternativeMethodsViewModel(clerk)

    viewModel.signInWithProvider(OAuthProvider.Github, signIn = signIn)
    advanceUntilIdle()

    assertEquals(AuthenticationViewState.Idle, viewModel.state.value)
  }

  @Test
  fun `missing sign in returns to auth start`() = runTest {
    val viewModel = AlternativeMethodsViewModel(clerk)

    viewModel.signInWithProvider(OAuthProvider.Github, signIn = null)
    advanceUntilIdle()

    assertEquals(AuthenticationViewState.NotStarted, viewModel.state.value)
    coVerify(exactly = 0) { authenticateWithRedirect(any(), any(), any()) }
  }
}
