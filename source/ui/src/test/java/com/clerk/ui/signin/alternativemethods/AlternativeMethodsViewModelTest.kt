package com.clerk.ui.signin.alternativemethods

import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signin.SignIn
import com.clerk.api.sso.OAuthProvider
import com.clerk.ui.auth.AuthenticationViewState
import com.clerk.ui.userprofile.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockkObject
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

  @get:Rule val dispatcherRule = MainDispatcherRule()

  @Before
  fun setUp() {
    mockkObject(SignIn.Companion)
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `OAuth cancellation leaves alternative methods idle`() = runTest {
    coEvery { SignIn.authenticateWithRedirect(any(), any()) } returns
      ClerkResult.unknownFailure(ssoCancellation())
    val viewModel = AlternativeMethodsViewModel()

    viewModel.signInWithProvider(OAuthProvider.GITHUB)
    advanceUntilIdle()

    assertEquals(AuthenticationViewState.Idle, viewModel.state.value)
  }

  private fun ssoCancellation(): Throwable =
    Class.forName("com.clerk.api.sso.SSOCancellationException")
      .getDeclaredConstructor(String::class.java)
      .apply { isAccessible = true }
      .newInstance("Authentication cancelled") as Throwable
}
