package com.clerk.api.signin

import com.clerk.api.network.model.verification.Verification
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.sso.OAuthResult
import com.clerk.api.sso.SSOService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SignInAuthenticateWithRedirectTest {

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `authenticateWithRedirect uses OAuth provider strategy`() = runTest {
    mockkObject(SSOService)
    val redirectUrl = "clerk://example.callback"
    val oauthResult = OAuthResult(signIn = null)
    coEvery {
      SSOService.authenticateWithRedirect(
        strategy = "oauth_google",
        redirectUrl = redirectUrl,
        identifier = null,
        emailAddress = null,
        legalAccepted = null,
        transferable = true,
      )
    } returns ClerkResult.success(oauthResult)

    val result =
      SignIn.authenticateWithRedirect(
        SignIn.AuthenticateWithRedirectParams.OAuth(
          provider = OAuthProvider.GOOGLE,
          redirectUrl = redirectUrl,
        )
      )

    assertTrue(result is ClerkResult.Success)
    assertSame(oauthResult, (result as ClerkResult.Success).value)
    coVerify(exactly = 1) {
      SSOService.authenticateWithRedirect(
        strategy = "oauth_google",
        redirectUrl = redirectUrl,
        identifier = null,
        emailAddress = null,
        legalAccepted = null,
        transferable = true,
      )
    }
  }

  @Test
  fun `authenticateWithRedirect uses Enterprise SSO strategy`() = runTest {
    mockkObject(SSOService)
    val redirectUrl = "https://sso.example.com/start"
    val oauthResult = OAuthResult(signIn = null)
    coEvery {
      SSOService.authenticateWithRedirect(
        strategy = "enterprise_sso",
        redirectUrl = redirectUrl,
        identifier = null,
        emailAddress = null,
        legalAccepted = null,
        transferable = false,
      )
    } returns ClerkResult.success(oauthResult)

    val result =
      SignIn.authenticateWithRedirect(
        SignIn.AuthenticateWithRedirectParams.EnterpriseSSO(redirectUrl = redirectUrl),
        transferable = false,
      )

    assertTrue(result is ClerkResult.Success)
    assertSame(oauthResult, (result as ClerkResult.Success).value)
    coVerify(exactly = 1) {
      SSOService.authenticateWithRedirect(
        strategy = "enterprise_sso",
        redirectUrl = redirectUrl,
        identifier = null,
        emailAddress = null,
        legalAccepted = null,
        transferable = false,
      )
    }
  }

  @Test
  fun `authenticateWithPreparedRedirect continues prepared external verification`() = runTest {
    mockkObject(SSOService)
    val externalVerificationRedirectUrl = "https://sso.example.com/start"
    val oauthResult = OAuthResult(signIn = null)
    val signIn =
      SignIn(
        id = "sign_in_123",
        firstFactorVerification =
          Verification(externalVerificationRedirectUrl = externalVerificationRedirectUrl),
      )
    coEvery {
      SSOService.authenticateWithPreparedRedirect(
        externalVerificationRedirectUrl = externalVerificationRedirectUrl,
        transferable = false,
      )
    } returns ClerkResult.success(oauthResult)

    val result = signIn.authenticateWithPreparedRedirect(transferable = false)

    assertTrue(result is ClerkResult.Success)
    assertSame(oauthResult, (result as ClerkResult.Success).value)
    coVerify(exactly = 1) {
      SSOService.authenticateWithPreparedRedirect(
        externalVerificationRedirectUrl = externalVerificationRedirectUrl,
        transferable = false,
      )
    }
  }

  @Test
  fun `authenticateWithPreparedRedirect fails when external verification URL is missing`() =
    runTest {
      val signIn = SignIn(id = "sign_in_123")

      val result = signIn.authenticateWithPreparedRedirect()

      assertTrue(result is ClerkResult.Failure)
    }
}
