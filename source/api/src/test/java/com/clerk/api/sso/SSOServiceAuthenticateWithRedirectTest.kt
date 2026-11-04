package com.clerk.api.sso

import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.SignInApi
import com.clerk.api.network.api.SignUpApi
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.redirect.RedirectCoordinator
import com.clerk.api.signin.SignIn
import com.clerk.api.signup.SignUp
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SSOServiceAuthenticateWithRedirectTest {

  @After
  fun tearDown() {
    RedirectCoordinator.cancelPending()
    unmockkAll()
  }

  @Test
  fun `authenticateWithRedirect creates sign in then prepares OAuth first factor`() = runTest {
    mockkObject(ClerkApi)
    mockkObject(SSOService)
    val signInApi = mockk<SignInApi>()
    val redirectUrl = "clerk://example.callback"
    val externalVerificationRedirectUrl = "https://oauth.example.com/start"
    val createdSignIn =
      SignIn(
        id = "sign_in_123",
        status = SignIn.Status.NEEDS_FIRST_FACTOR,
        supportedFirstFactors = listOf(Factor(strategy = "oauth_google")),
      )
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
    val preparedRedirectUrl = prepareParams.captured.getValue("redirect_url")
    assertTrue(preparedRedirectUrl.startsWith("$redirectUrl?clerk_redirect_state="))
    coVerify(exactly = 1) { signInApi.createSignIn(any()) }
    coVerify(exactly = 1) { signInApi.prepareSignInFirstFactor(createdSignIn.id, any()) }
    coVerify(exactly = 1) {
      SSOService.authenticateWithPreparedRedirect(
        externalVerificationRedirectUrl = externalVerificationRedirectUrl,
        transferable = true,
      )
    }
  }

  @Test
  fun `authenticateWithRedirect keeps legal_accepted off the sign in and hands it to the transfer`() =
    runTest {
      mockkObject(ClerkApi)
      mockkObject(SSOService)
      val signInApi = mockk<SignInApi>()
      val externalVerificationRedirectUrl = "https://oauth.example.com/start"
      val createdSignIn = SignIn(id = "sign_in_123", status = SignIn.Status.NEEDS_FIRST_FACTOR)
      val preparedSignIn =
        SignIn(
          id = createdSignIn.id,
          firstFactorVerification =
            Verification(externalVerificationRedirectUrl = externalVerificationRedirectUrl),
        )
      val oauthResult = OAuthResult(signIn = preparedSignIn)
      val createParams = slot<Map<String, String>>()

      every { ClerkApi.signIn } returns signInApi
      coEvery { signInApi.createSignIn(capture(createParams)) } returns
        ClerkResult.success(createdSignIn)
      coEvery { signInApi.prepareSignInFirstFactor(createdSignIn.id, any()) } returns
        ClerkResult.success(preparedSignIn)
      coEvery {
        SSOService.authenticateWithRedirect(any(), any(), any(), any(), any(), any())
      } coAnswers { callOriginal() }
      coEvery {
        SSOService.authenticateWithPreparedRedirect(
          externalVerificationRedirectUrl = externalVerificationRedirectUrl,
          transferable = true,
          legalAccepted = true,
        )
      } returns ClerkResult.success(oauthResult)

      val result =
        SSOService.authenticateWithRedirect(
          strategy = "oauth_google",
          redirectUrl = "clerk://example.callback",
          legalAccepted = true,
          transferable = true,
        )

      assertTrue(result is ClerkResult.Success)
      assertFalse(createParams.captured.containsKey("legal_accepted"))
      coVerify(exactly = 1) {
        SSOService.authenticateWithPreparedRedirect(
          externalVerificationRedirectUrl = externalVerificationRedirectUrl,
          transferable = true,
          legalAccepted = true,
        )
      }
    }

  @Test
  fun `enterprise SSO sign in prepares the chosen enterprise connection`() = runTest {
    mockkObject(ClerkApi)
    mockkObject(SSOService)
    val signInApi = mockk<SignInApi>()
    val createdSignIn = SignIn(id = "sign_in_123", status = SignIn.Status.NEEDS_FIRST_FACTOR)
    val preparedSignIn =
      SignIn(
        id = createdSignIn.id,
        firstFactorVerification = Verification(externalVerificationRedirectUrl = IDP_URL),
      )
    val createParams = slot<Map<String, String>>()
    val prepareParams = slot<Map<String, String>>()

    every { ClerkApi.signIn } returns signInApi
    coEvery { signInApi.createSignIn(capture(createParams)) } returns
      ClerkResult.success(createdSignIn)
    coEvery { signInApi.prepareSignInFirstFactor(createdSignIn.id, capture(prepareParams)) } returns
      ClerkResult.success(preparedSignIn)
    coEvery { SSOService.authenticateWithPreparedRedirect(IDP_URL, true) } returns
      ClerkResult.success(OAuthResult(signIn = preparedSignIn))

    SSOService.authenticateWithRedirect(
      strategy = "enterprise_sso",
      redirectUrl = "clerk://example.callback",
      emailAddress = "user@company.com",
      enterpriseConnectionId = "ent_conn_2",
    )

    assertEquals("enterprise_sso", prepareParams.captured["strategy"])
    assertEquals("ent_conn_2", prepareParams.captured["enterprise_connection_id"])
    assertFalse(createParams.captured.containsKey("enterprise_connection_id"))
  }

  @Test
  fun `enterprise SSO sign up sends the chosen enterprise connection on the update`() = runTest {
    mockkObject(ClerkApi)
    mockkObject(SSOService)
    val signUpApi = mockk<SignUpApi>()
    val createdSignUp = signUp(verifications = emptyMap())
    val updatedSignUp =
      signUp(
        verifications =
          mapOf("external_account" to Verification(externalVerificationRedirectUrl = IDP_URL))
      )
    val createParams = slot<Map<String, String>>()
    val updateParams = slot<Map<String, String>>()

    every { ClerkApi.signUp } returns signUpApi
    coEvery { signUpApi.createSignUp(capture(createParams)) } returns
      ClerkResult.success(createdSignUp)
    coEvery { signUpApi.updateSignUp(createdSignUp.id, capture(updateParams)) } returns
      ClerkResult.success(updatedSignUp)
    coEvery { SSOService.authenticateWithPreparedRedirect(IDP_URL, any(), any(), any()) } returns
      ClerkResult.success(OAuthResult(signUp = updatedSignUp))

    val result =
      SSOService.authenticateSignUpWithRedirect(
        strategy = "enterprise_sso",
        redirectUrl = "clerk://example.callback",
        emailAddress = "user@company.com",
        legalAccepted = true,
        enterpriseConnectionId = "ent_conn_2",
      )

    assertTrue(result is ClerkResult.Success)
    assertEquals("user@company.com", createParams.captured["email_address"])
    assertEquals("true", createParams.captured["legal_accepted"])
    assertFalse(createParams.captured.containsKey("strategy"))
    assertFalse(createParams.captured.containsKey("redirect_url"))
    assertFalse(createParams.captured.containsKey("enterprise_connection_id"))
    assertEquals("enterprise_sso", updateParams.captured["strategy"])
    assertEquals("ent_conn_2", updateParams.captured["enterprise_connection_id"])
    assertTrue(
      updateParams.captured
        .getValue("redirect_url")
        .startsWith("clerk://example.callback?clerk_redirect_state=")
    )
  }

  @Test
  fun `enterprise SSO sign up without a connection creates the sign up in one request`() = runTest {
    mockkObject(ClerkApi)
    mockkObject(SSOService)
    val signUpApi = mockk<SignUpApi>()
    val createdSignUp =
      signUp(
        verifications =
          mapOf("external_account" to Verification(externalVerificationRedirectUrl = IDP_URL))
      )
    val createParams = slot<Map<String, String>>()

    every { ClerkApi.signUp } returns signUpApi
    coEvery { signUpApi.createSignUp(capture(createParams)) } returns
      ClerkResult.success(createdSignUp)
    coEvery { SSOService.authenticateWithPreparedRedirect(IDP_URL, any(), any(), any()) } returns
      ClerkResult.success(OAuthResult(signUp = createdSignUp))

    SSOService.authenticateSignUpWithRedirect(
      strategy = "enterprise_sso",
      redirectUrl = "clerk://example.callback",
      emailAddress = "user@company.com",
    )

    assertEquals("enterprise_sso", createParams.captured["strategy"])
    assertTrue(createParams.captured.containsKey("redirect_url"))
    assertFalse(createParams.captured.containsKey("enterprise_connection_id"))
    coVerify(exactly = 0) { signUpApi.updateSignUp(any(), any()) }
  }

  private fun signUp(verifications: Map<String, Verification?>) =
    SignUp(
      id = "sign_up_123",
      status = SignUp.Status.MISSING_REQUIREMENTS,
      requiredFields = emptyList(),
      optionalFields = emptyList(),
      missingFields = emptyList(),
      unverifiedFields = emptyList(),
      verifications = verifications,
      passwordEnabled = false,
    )

  private companion object {
    const val IDP_URL = "https://idp.example.com/start"
  }
}
