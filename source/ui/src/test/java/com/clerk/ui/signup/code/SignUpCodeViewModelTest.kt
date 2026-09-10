package com.clerk.ui.signup.code

import com.clerk.api.*
import com.clerk.testing.mockClerk
import com.clerk.ui.auth.AuthenticationViewState
import com.clerk.ui.userprofile.MainDispatcherRule
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignUpCodeViewModelTest {
  @get:Rule val dispatcherRule = MainDispatcherRule()
  private val clerk = mockClerk()
  private val signUp = mockk<SignUp>(relaxed = true)
  private val verifications = mockk<SignUpVerifications>(relaxed = true)

  init {
    every { clerk.signUp } returns signUp
    every { signUp.id } returns "sua_123"
    every { signUp.verifications } returns verifications
    every { verifications.emailAddress.strategy } returns null
  }

  @After fun tearDown() = unmockkAll()

  @Test
  fun emailLinkRoutesToLinkScreenWithoutSendingCode() = runTest {
    every { verifications.emailAddress.strategy } returns "email_link"
    val model = SignUpCodeViewModel(clerk)
    model.prepare(SignUpCodeField.Email("sam@clerk.dev"))
    advanceUntilIdle()
    assertEquals(AuthenticationViewState.Success.SignUp(signUp), model.state.value)
    coVerify(exactly = 0) { verifications.sendEmailCode() }
    coVerify(exactly = 0) { verifications.sendPhoneCode(any()) }
    coVerify(exactly = 0) { verifications.sendEmailLink(any()) }
  }

  @Test
  fun emailCodeCallsCurrentSignUpVerificationGroup() = runTest {
    val model = SignUpCodeViewModel(clerk)
    model.prepare(SignUpCodeField.Email("sam@clerk.dev"))
    advanceUntilIdle()
    assertEquals(AuthenticationViewState.Idle, model.state.value)
    coVerify(exactly = 1) { verifications.sendEmailCode() }
    coVerify(exactly = 0) { verifications.sendPhoneCode(any()) }
    coVerify(exactly = 0) { verifications.sendEmailLink(any()) }
  }

  @Test
  fun phoneCodeIgnoresEmailLinkPreference() = runTest {
    every { verifications.emailAddress.strategy } returns "email_link"
    val model = SignUpCodeViewModel(clerk)
    model.prepare(SignUpCodeField.Phone("+15555550123"))
    advanceUntilIdle()
    assertEquals(AuthenticationViewState.Idle, model.state.value)
    coVerify(exactly = 1) { verifications.sendPhoneCode(null) }
    coVerify(exactly = 0) { verifications.sendEmailCode() }
    coVerify(exactly = 0) { verifications.sendEmailLink(any()) }
  }
}
