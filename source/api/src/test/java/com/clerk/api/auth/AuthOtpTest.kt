package com.clerk.api.auth

import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.SignInApi
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signin.SignIn
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthOtpTest {
  private lateinit var auth: Auth
  private lateinit var signInApi: SignInApi

  @Before
  fun setup() {
    auth = Auth()
    signInApi = mockk(relaxed = true)

    mockkObject(ClerkApi)
    mockkObject(Clerk)

    every { ClerkApi.signIn } returns signInApi
    every { Clerk.locale } returns MutableStateFlow("en")
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `signInWithOtp for email prepares email code instead of native email link`() = runTest {
    val createdSignIn =
      SignIn(
        id = "sign_in_123",
        supportedFirstFactors =
          listOf(Factor(strategy = "email_code", emailAddressId = "email_123")),
      )
    val preparedSignIn =
      createdSignIn.copy(
        firstFactorVerification =
          Verification(strategy = "email_code", status = Verification.Status.UNVERIFIED)
      )

    coEvery { signInApi.createSignIn(any()) } returns ClerkResult.success(createdSignIn)
    coEvery { signInApi.prepareSignInFirstFactor(any(), any()) } returns
      ClerkResult.success(preparedSignIn)

    val result = auth.signInWithOtp { email = "user@example.com" }

    assertTrue(result is ClerkResult.Success)
    coVerify(exactly = 1) {
      signInApi.createSignIn(
        match { it["identifier"] == "user@example.com" && it["locale"] == "en" }
      )
    }
    coVerify(exactly = 1) {
      signInApi.prepareSignInFirstFactor(
        "sign_in_123",
        match {
          it["strategy"] == "email_code" &&
            it["email_address_id"] == "email_123" &&
            !it.containsKey("redirect_uri") &&
            !it.containsKey("code_challenge")
        },
      )
    }
  }
}
