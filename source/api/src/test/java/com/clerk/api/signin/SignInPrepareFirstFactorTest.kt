// These tests pin the deprecated entry points until they are removed in the next major.
@file:Suppress("DEPRECATION")

package com.clerk.api.signin

import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.SignInApi
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.redirect.RedirectState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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
    val externalUrl = "https://accounts.example.com/oauth/authorize"
    val preparedSignIn =
      SignIn(
        id = "sign_in_123",
        status = SignIn.Status.NEEDS_FIRST_FACTOR,
        firstFactorVerification = Verification(externalVerificationRedirectUrl = externalUrl),
      )
    val params =
      SignIn.PrepareFirstFactorParams.OAuth(
        strategy = "oauth_google",
        redirectUrl = "clerk://callback",
      )
    val fields = slot<Map<String, String>>()
    mockkObject(ClerkApi)
    every { ClerkApi.signIn } returns signInApi
    coEvery { signInApi.prepareSignInFirstFactor("sign_in_123", capture(fields)) } returns
      ClerkResult.success(preparedSignIn)

    val result = signIn.prepareFirstFactor(params)

    assertTrue(result is ClerkResult.Success)
    assertSame(preparedSignIn, (result as ClerkResult.Success).value)
    coVerify(exactly = 1) { signInApi.prepareSignInFirstFactor("sign_in_123", any()) }
    assertEquals("oauth_google", fields.captured["strategy"])
    val state =
      fields.captured
        .getValue("redirect_url")
        .substringAfter("clerk://callback?clerk_redirect_state=")
    assertTrue(state.isNotBlank())
    assertEquals(state, RedirectState.take(externalUrl))
  }
}
