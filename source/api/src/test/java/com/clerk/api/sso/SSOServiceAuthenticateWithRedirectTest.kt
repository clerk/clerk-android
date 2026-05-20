package com.clerk.api.sso

import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.SignInApi
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signin.SignIn
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

class SSOServiceAuthenticateWithRedirectTest {

  @After
  fun tearDown() {
    SSOService.cancelPendingAuthentication()
    unmockkAll()
  }

  @Test
  fun `authenticateWithRedirect creates sign in then prepares OAuth first factor`() = runTest {
    mockkObject(ClerkApi)
    mockkObject(SSOService)
    val signInApi = mockk<SignInApi>()
    val redirectUrl = "clerk://example.callback"
    val externalVerificationRedirectUrl = "https://oauth.example.com/start"
    val createdSignIn = SignIn(id = "sign_in_123")
    val preparedSignIn =
      SignIn(
        id = createdSignIn.id,
        firstFactorVerification =
          Verification(externalVerificationRedirectUrl = externalVerificationRedirectUrl),
      )
    val oauthResult = OAuthResult(signIn = preparedSignIn)
    val createParams = slot<Map<String, String>>()
    val prepareParams = slot<Map<String, String>>()

    every { ClerkApi.signIn } returns signInApi
    coEvery { signInApi.createSignIn(capture(createParams)) } returns
      ClerkResult.success(createdSignIn)
    coEvery { signInApi.prepareSignInFirstFactor(createdSignIn.id, capture(prepareParams)) } returns
      ClerkResult.success(preparedSignIn)
    coEvery {
      SSOService.authenticateWithRedirect(any(), any(), any(), any(), any(), any())
    } coAnswers { callOriginal() }
    coEvery {
      SSOService.authenticateWithPreparedRedirect(
        externalVerificationRedirectUrl = externalVerificationRedirectUrl,
        transferable = true,
      )
    } returns ClerkResult.success(oauthResult)

    val result =
      SSOService.authenticateWithRedirect(
        strategy = "oauth_google",
        redirectUrl = redirectUrl,
        transferable = true,
      )

    assertTrue(result is ClerkResult.Success)
    assertSame(oauthResult, (result as ClerkResult.Success).value)
    assertEquals("oauth_google", createParams.captured["strategy"])
    assertEquals(redirectUrl, createParams.captured["redirect_url"])
    assertTrue(createParams.captured.containsKey("locale"))
    assertEquals("oauth_google", prepareParams.captured["strategy"])
    assertEquals(redirectUrl, prepareParams.captured["redirect_url"])
    coVerify(exactly = 1) { signInApi.createSignIn(any()) }
    coVerify(exactly = 1) { signInApi.prepareSignInFirstFactor(createdSignIn.id, any()) }
    coVerify(exactly = 1) {
      SSOService.authenticateWithPreparedRedirect(
        externalVerificationRedirectUrl = externalVerificationRedirectUrl,
        transferable = true,
      )
    }
  }
}
