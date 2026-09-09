package com.clerk.ui.signin.emaillink

import com.clerk.api.*
import com.clerk.testing.*
import com.clerk.ui.auth.AuthenticationViewState
import com.clerk.ui.auth.FactorSelection
import com.clerk.ui.userprofile.MainDispatcherRule
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignInFactorOneEmailLinkViewModelTest {
  @get:org.junit.Rule val dispatcherRule = MainDispatcherRule()
  private val clerk = mockClerk()
  private val signIn = mockSignIn(SignInStatus.NeedsFirstFactor)
  private val revisions = MutableStateFlow(0L)

  init {
    every { clerk.signIn } returns signIn
    every { clerk.context.requireRuntime().changes } returns revisions
  }

  @After fun tearDown() = unmockkAll()

  @Test
  fun sendLinkSurfacesBackendLongMessageForRateLimits() = runTest {
    coEvery { signIn.emailLink.sendLink(any()) } throws
      testCoreError("Too many requests, retry later", "too_many_requests")
    val model = SignInFactorOneEmailLinkViewModel(clerk)
    advanceUntilIdle()
    model.sendLink(FactorSelection("email_link", emailAddressId = "email_123"))
    advanceUntilIdle()
    assertEquals(AuthenticationViewState.Error("Too many requests, retry later"), model.state.value)
    coVerify {
      signIn.emailLink.sendLink(
        SignInEmailLinkSendParams.Case2(
          SignInEmailLinkSendLinkParamsCase2(emailAddressId = "email_123")
        )
      )
    }
  }

  @Test
  fun coreRevisionAdvancesEmailLinkFlowToSecondFactor() = runTest {
    val model = SignInFactorOneEmailLinkViewModel(clerk)
    advanceUntilIdle()
    every { signIn.status } returns SignInStatus.NeedsSecondFactor
    revisions.value++
    advanceUntilIdle()
    assertEquals(AuthenticationViewState.Success.SignIn(signIn), model.state.value)
  }

  @Test
  fun resumeAdvancesEmailLinkFlowFromCurrentSignIn() = runTest {
    val model = SignInFactorOneEmailLinkViewModel(clerk)
    advanceUntilIdle()
    every { signIn.status } returns SignInStatus.NeedsSecondFactor
    model.onHostResumed()
    assertEquals(AuthenticationViewState.Success.SignIn(signIn), model.state.value)
  }

  @Test
  fun firstFactorRevisionDoesNotAdvanceEmailLinkFlow() = runTest {
    val model = SignInFactorOneEmailLinkViewModel(clerk)
    advanceUntilIdle()
    revisions.value++
    advanceUntilIdle()
    assertEquals(AuthenticationViewState.Idle, model.state.value)
  }
}
