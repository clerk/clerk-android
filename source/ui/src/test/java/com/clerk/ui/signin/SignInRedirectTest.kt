package com.clerk.ui.signin

import com.clerk.api.network.model.factor.Factor
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.authenticateWithOAuth
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.sso.OAuthResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Test

class SignInRedirectTest {

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `redirect reuses the current sign in attempt`() = runTest {
    mockkStatic("com.clerk.api.signin.SignInExtensionsKt")
    val currentSignIn =
      SignIn(
        id = "sign_in_existing",
        status = SignIn.Status.NEEDS_FIRST_FACTOR,
        identifier = "user@example.com",
        supportedFirstFactors = listOf(Factor(strategy = "password")),
      )
    val oauthResult = OAuthResult(signIn = currentSignIn)

    coEvery {
      currentSignIn.authenticateWithOAuth(OAuthProvider.GITHUB, transferable = true, any())
    } returns ClerkResult.success(oauthResult)

    val result =
      authenticateWithRedirect(
        signIn = currentSignIn,
        provider = OAuthProvider.GITHUB,
        transferable = true,
      )

    assertSame(oauthResult, (result as ClerkResult.Success).value)
    coVerify(exactly = 1) {
      currentSignIn.authenticateWithOAuth(OAuthProvider.GITHUB, transferable = true, any())
    }
  }
}
