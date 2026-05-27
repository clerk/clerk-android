package com.clerk.api.auth

import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.SignInApi
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signin.SignIn
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
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
  fun `signInWithOtp for email creates email code flow instead of native email link`() = runTest {
    val createdSignIn = SignIn(id = "sign_in_123")
    val createParams = slot<Map<String, String>>()

    coEvery { signInApi.createSignIn(capture(createParams)) } returns
      ClerkResult.success(createdSignIn)

    val result = auth.signInWithOtp { email = "user@example.com" }

    assertTrue(result is ClerkResult.Success)
    assertTrue(createParams.captured["identifier"] == "user@example.com")
    assertTrue(createParams.captured["locale"] == "en")
    assertTrue(createParams.captured["strategy"] == "email_code")
    assertTrue(!createParams.captured.containsKey("redirect_uri"))
    assertTrue(!createParams.captured.containsKey("code_challenge"))
    coVerify(exactly = 1) { signInApi.createSignIn(any()) }
    coVerify(exactly = 0) { signInApi.prepareSignInFirstFactor(any(), any()) }
  }
}
