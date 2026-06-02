package com.clerk.api.signup

import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.sso.OAuthResult
import com.clerk.api.sso.SSOService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SignUpAuthenticateWithRedirectTest {

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `authenticateWithRedirect uses sign-up redirect flow for OAuth provider`() = runTest {
    mockkObject(SSOService)
    val redirectUrl = "clerk://example.callback"
    val signUp = mockk<SignUp>(relaxed = true)
    val oauthResult = OAuthResult(signUp = signUp)
    coEvery {
      SSOService.authenticateSignUpWithRedirect(
        strategy = "oauth_google",
        redirectUrl = redirectUrl,
        identifier = null,
        emailAddress = null,
        legalAccepted = null,
      )
    } returns ClerkResult.success(oauthResult)

    val result =
      SignUp.authenticateWithRedirect(
        SignUp.AuthenticateWithRedirectParams.OAuth(
          provider = OAuthProvider.GOOGLE,
          redirectUrl = redirectUrl,
        )
      )

    assertTrue(result is ClerkResult.Success)
    assertSame(oauthResult, (result as ClerkResult.Success).value)
    coVerify(exactly = 1) {
      SSOService.authenticateSignUpWithRedirect(
        strategy = "oauth_google",
        redirectUrl = redirectUrl,
        identifier = null,
        emailAddress = null,
        legalAccepted = null,
      )
    }
  }

  @Test
  fun `authenticateWithRedirect forwards unsafe metadata`() = runTest {
    mockkObject(SSOService)
    val unsafeMetadataJson = """{"test":"test"}"""
    val signUp = mockk<SignUp>(relaxed = true)
    val oauthResult = OAuthResult(signUp = signUp)
    coEvery {
      SSOService.authenticateSignUpWithRedirect(
        strategy = "oauth_google",
        redirectUrl = any(),
        identifier = null,
        emailAddress = null,
        legalAccepted = null,
        unsafeMetadata = unsafeMetadataJson,
      )
    } returns ClerkResult.success(oauthResult)

    val result =
      SignUp.authenticateWithRedirect(
        SignUp.AuthenticateWithRedirectParams.OAuth(
          provider = OAuthProvider.GOOGLE,
          unsafeMetadata = unsafeMetadataJson,
        )
      )

    assertTrue(result is ClerkResult.Success)
    assertSame(oauthResult, (result as ClerkResult.Success).value)
    coVerify(exactly = 1) {
      SSOService.authenticateSignUpWithRedirect(
        strategy = "oauth_google",
        redirectUrl = any(),
        identifier = null,
        emailAddress = null,
        legalAccepted = null,
        unsafeMetadata = unsafeMetadataJson,
      )
    }
  }
}
