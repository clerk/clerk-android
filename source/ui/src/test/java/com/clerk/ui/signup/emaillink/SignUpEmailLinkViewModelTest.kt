package com.clerk.ui.signup.emaillink

import com.clerk.api.*
import com.clerk.testing.*
import com.clerk.ui.auth.AuthenticationViewState
import com.clerk.ui.userprofile.MainDispatcherRule
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignUpEmailLinkViewModelTest {
  @get:org.junit.Rule val dispatcherRule = MainDispatcherRule()
  private val clerk = mockClerk()
  private val signUp = mockk<SignUp>(relaxed = true)
  private val revisions = MutableStateFlow(0L)

  init {
    every { clerk.signUp } returns signUp
    every { signUp.id } returns "sign_up_123"
    every { signUp.status } returns SignUpStatus.MissingRequirements
    every { signUp.verifications.emailAddress.status } returns VerificationStatus.Unverified
    every { clerk.context.requireRuntime().changes } returns revisions
  }

  @After fun tearDown() = unmockkAll()

  @Test
  fun verifiedEmailRevisionAdvancesToNextSignUpStep() = runTest {
    val model = SignUpEmailLinkViewModel(clerk)
    advanceUntilIdle()
    every { signUp.verifications.emailAddress.status } returns VerificationStatus.Verified
    revisions.value++
    advanceUntilIdle()
    assertEquals(AuthenticationViewState.Success.SignUp(signUp), model.state.value)
  }

  @Test
  fun resumeAdvancesFromVerifiedEmailState() = runTest {
    val model = SignUpEmailLinkViewModel(clerk)
    advanceUntilIdle()
    every { signUp.verifications.emailAddress.status } returns VerificationStatus.Verified
    model.onHostResumed()
    assertEquals(AuthenticationViewState.Success.SignUp(signUp), model.state.value)
  }
}
