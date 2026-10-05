package com.clerk.ui.signup.code

import com.clerk.api.Clerk
import com.clerk.api.auth.types.VerificationType
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signup.SignUp
import com.clerk.api.signup.sendEmailCode
import com.clerk.api.signup.sendPhoneCode
import com.clerk.api.signup.verifyCode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignUpCodeViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private val signUp = mockk<SignUp>(relaxed = true)
  private val client = mockk<Client>()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockkObject(Clerk)
    every { Clerk.client } returns client
    every { client.signUp } returns signUp
    mockkStatic("com.clerk.api.signup.SignUpKt", "com.clerk.api.signup.SignUpExtensionsKt")
    coEvery { signUp.sendEmailCode() } returns ClerkResult.success(signUp)
    coEvery { signUp.sendPhoneCode() } returns ClerkResult.success(signUp)
    coEvery { signUp.verifyCode(any(), any()) } returns ClerkResult.success(signUp)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

  @Test
  fun `prepare sends the code to the field's channel`() =
    runTest(testDispatcher) {
      val viewModel = SignUpCodeViewModel()

      viewModel.prepare(SignUpCodeField.Phone("+15555550100"))
      advanceUntilIdle()

      coVerify(exactly = 1) { signUp.sendPhoneCode() }
      coVerify(exactly = 0) { signUp.sendEmailCode() }
    }

  @Test
  fun `attempt verifies the code against the field's channel`() =
    runTest(testDispatcher) {
      val viewModel = SignUpCodeViewModel()

      viewModel.attempt("123456", SignUpCodeField.Email("user@example.com"))
      viewModel.attempt("654321", SignUpCodeField.Phone("+15555550100"))
      advanceUntilIdle()

      coVerify(exactly = 1) { signUp.verifyCode("123456", VerificationType.EMAIL) }
      coVerify(exactly = 1) { signUp.verifyCode("654321", VerificationType.PHONE) }
    }
}
