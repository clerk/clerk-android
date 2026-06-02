package com.clerk.api.signin

import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.SignInApi
import com.clerk.api.network.serialization.ClerkResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SignInPrepareFirstFactorTest {

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `oauth first factor can be prepared from needs identifier status`() = runTest {
    val signInApi = mockk<SignInApi>()
    val signIn = SignIn(id = "sign_in_123", status = SignIn.Status.NEEDS_IDENTIFIER)
    val preparedSignIn = SignIn(id = "sign_in_123", status = SignIn.Status.NEEDS_FIRST_FACTOR)
    val params =
      SignIn.PrepareFirstFactorParams.OAuth(
        strategy = "oauth_google",
        redirectUrl = "clerk://callback",
      )
    val paramsMap = mapOf("strategy" to "oauth_google", "redirect_url" to "clerk://callback")
    mockkObject(ClerkApi)
    every { ClerkApi.signIn } returns signInApi
    coEvery { signInApi.prepareSignInFirstFactor("sign_in_123", paramsMap) } returns
      ClerkResult.success(preparedSignIn)

    val result = signIn.prepareFirstFactor(params)

    assertTrue(result is ClerkResult.Success)
    assertSame(preparedSignIn, (result as ClerkResult.Success).value)
    coVerify(exactly = 1) { signInApi.prepareSignInFirstFactor("sign_in_123", paramsMap) }
  }
}
