package com.clerk.api.auth

import com.clerk.api.Clerk
import com.clerk.api.auth.types.MfaType
import com.clerk.api.auth.types.VerificationType
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.SignInApi
import com.clerk.api.network.api.SignUpApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.attemptFirstFactor
import com.clerk.api.signin.attemptSecondFactor
import com.clerk.api.signin.verifyCode
import com.clerk.api.signin.verifyMfaCode
import com.clerk.api.signin.verifyWithPassword
import com.clerk.api.signup.SignUp
import com.clerk.api.signup.attemptVerification
import com.clerk.api.signup.update
import com.clerk.api.signup.verifyCode
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.sso.OAuthResult
import com.clerk.api.sso.SSOService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Each `Clerk.auth` method and the older `SignIn` / `SignUp` entry point for the same operation
 * must send Clerk the same request. These tests pin that, so the layers cannot drift apart again.
 */
class AuthLayerParityTest {

  private val signInApi = mockk<SignInApi>()
  private val signUpApi = mockk<SignUpApi>()
  private val signInRequests = mutableListOf<Map<String, String>>()
  private val signUpRequests = mutableListOf<Map<String, String>>()
  private val signIn = SignIn(id = "sign_in_123")
  private val signUp = mockk<SignUp>(relaxed = true) { every { id } returns "sign_up_123" }

  @Before
  fun setup() {
    Clerk.updateClient(Client())
    mockkObject(ClerkApi)
    every { ClerkApi.signIn } returns signInApi
    every { ClerkApi.signUp } returns signUpApi
    coEvery { signInApi.createSignIn(capture(signInRequests)) } returns ClerkResult.success(signIn)
    coEvery { signInApi.attemptFirstFactor(any(), capture(signInRequests)) } returns
      ClerkResult.success(signIn)
    coEvery { signInApi.attemptSecondFactor(any(), capture(signInRequests)) } returns
      ClerkResult.success(signIn)
    coEvery { signUpApi.createSignUp(capture(signUpRequests)) } returns ClerkResult.success(signUp)
    coEvery { signUpApi.updateSignUp(any(), capture(signUpRequests)) } returns
      ClerkResult.success(signUp)
    coEvery { signUpApi.attemptSignUpVerification(any(), any(), any()) } returns
      ClerkResult.success(signUp)
  }

  @After
  fun tearDown() {
    unmockkAll()
    Clerk.updateClient(Client())
  }

  @Test
  fun `password sign-in sends the same request from both layers`() = runTest {
    Clerk.auth.signInWithPassword {
      identifier = "user@example.com"
      password = "secret"
    }
    SignIn.create(SignIn.CreateParams.Strategy.Password("user@example.com", "secret"))

    assertEachLayerSent(
      signInRequests,
      mapOf(
        "identifier" to "user@example.com",
        "password" to "secret",
        "strategy" to "password",
        "locale" to "",
      ),
    )
  }

  @Test
  fun `otp sign-in sends the same request from both layers`() = runTest {
    Clerk.auth.signInWithOtp { email = "user@example.com" }
    SignIn.create(SignIn.CreateParams.Strategy.EmailCode("user@example.com"))
    Clerk.auth.signInWithOtp { phone = "+15555550100" }
    SignIn.create(SignIn.CreateParams.Strategy.PhoneCode("+15555550100"))

    assertEachLayerSent(
      signInRequests,
      mapOf("identifier" to "user@example.com", "strategy" to "email_code", "locale" to ""),
      mapOf("identifier" to "+15555550100", "strategy" to "phone_code", "locale" to ""),
    )
  }

  @Test
  fun `identifier and ticket sign-ins send the same request from both layers`() = runTest {
    Clerk.auth.signIn { username = "user_123" }
    SignIn.create(SignIn.CreateParams.Strategy.Identifier(identifier = "user_123"))
    Clerk.auth.signInWithTicket("ticket_123")
    SignIn.create(SignIn.CreateParams.Strategy.Ticket("ticket_123"))

    assertEachLayerSent(
      signInRequests,
      mapOf("identifier" to "user_123", "locale" to ""),
      mapOf("ticket" to "ticket_123", "strategy" to "ticket", "locale" to ""),
    )
  }

  @Test
  fun `sign-up sends the same request from both layers`() = runTest {
    Clerk.auth.signUp {
      email = "user@example.com"
      firstName = "Ada"
      legalAccepted = true
      unsafeMetadata = mapOf("plan" to "pro")
    }
    SignUp.create(
      SignUp.CreateParams.Standard(
        emailAddress = "user@example.com",
        firstName = "Ada",
        legalAccepted = true,
        unsafeMetadata = mapOf("plan" to "pro"),
      )
    )
    Clerk.auth.signUpWithTicket("ticket_123")
    SignUp.create(SignUp.CreateParams.Ticket("ticket_123"))

    assertEachLayerSent(
      signUpRequests,
      mapOf(
        "email_address" to "user@example.com",
        "first_name" to "Ada",
        "legal_accepted" to "true",
        "unsafe_metadata" to """{"plan":"pro"}""",
        "locale" to "",
      ),
      mapOf("strategy" to "ticket", "ticket" to "ticket_123", "locale" to ""),
    )
  }

  @Test
  fun `sign-up update sends the same request from both layers`() = runTest {
    signUp.update {
      username = "ada"
      legalAccepted = true
    }
    signUp.update(SignUp.SignUpUpdateParams.Standard(username = "ada", legalAccepted = true))

    assertEachLayerSent(signUpRequests, mapOf("username" to "ada", "legal_accepted" to "true"))
  }

  @Test
  fun `first factor verification sends the same request from both layers`() = runTest {
    val emailCodeSignIn =
      signIn.copy(firstFactorVerification = Verification(strategy = "email_code"))
    emailCodeSignIn.verifyCode("123456")
    emailCodeSignIn.attemptFirstFactor(SignIn.AttemptFirstFactorParams.EmailCode("123456"))
    signIn.verifyWithPassword("secret")
    signIn.attemptFirstFactor(SignIn.AttemptFirstFactorParams.Password("secret"))

    assertEachLayerSent(
      signInRequests,
      mapOf("code" to "123456", "strategy" to "email_code"),
      mapOf("password" to "secret", "strategy" to "password"),
    )
  }

  @Test
  fun `second factor verification sends the same request from both layers`() = runTest {
    signIn.verifyMfaCode("123456", MfaType.TOTP)
    signIn.attemptSecondFactor(SignIn.AttemptSecondFactorParams.TOTP("123456"))

    assertEachLayerSent(signInRequests, mapOf("code" to "123456", "strategy" to "totp"))
  }

  @Test
  fun `sign-up verification sends the same request from both layers`() = runTest {
    signUp.verifyCode("123456", VerificationType.PHONE)
    signUp.attemptVerification(SignUp.AttemptVerificationParams.PhoneCode("123456"))

    coVerify(exactly = 2) {
      signUpApi.attemptSignUpVerification("sign_up_123", "phone_code", "123456")
    }
  }

  @Test
  fun `sign-in redirects start the same flow from both layers`() = runTest {
    mockkObject(SSOService)
    coEvery {
      SSOService.authenticateWithRedirect(any(), any(), any(), any(), any(), any())
    } returns ClerkResult.success(OAuthResult(signIn = signIn))

    Clerk.auth.signInWithOAuth(OAuthProvider.GITHUB)
    SignIn.authenticateWithRedirect(
      SignIn.AuthenticateWithRedirectParams.OAuth(OAuthProvider.GITHUB)
    )
    Clerk.auth.signInWithEnterpriseSso { email = "user@company.com" }
    SignIn.authenticateWithRedirect(
      SignIn.AuthenticateWithRedirectParams.EnterpriseSSO(
        redirectUrl = DEFAULT_REDIRECT,
        emailAddress = "user@company.com",
      )
    )

    coVerify(exactly = 2) {
      SSOService.authenticateWithRedirect("oauth_github", DEFAULT_REDIRECT, null, null, null, true)
    }
    coVerify(exactly = 2) {
      SSOService.authenticateWithRedirect(
        "enterprise_sso",
        DEFAULT_REDIRECT,
        null,
        "user@company.com",
        null,
        true,
      )
    }
  }

  @Test
  fun `sign-up redirects start the same flow from both layers`() = runTest {
    mockkObject(SSOService)
    coEvery {
      SSOService.authenticateSignUpWithRedirect(any(), any(), any(), any(), any(), any())
    } returns ClerkResult.success(OAuthResult(signUp = signUp))

    Clerk.auth.signUpWithOAuth(OAuthProvider.GITHUB)
    SignUp.authenticateWithRedirect(
      SignUp.AuthenticateWithRedirectParams.OAuth(OAuthProvider.GITHUB)
    )
    Clerk.auth.signUpWithEnterpriseSso { email = "user@company.com" }
    SignUp.authenticateWithRedirect(
      SignUp.AuthenticateWithRedirectParams.EnterpriseSSO(emailAddress = "user@company.com")
    )

    coVerify(exactly = 2) {
      SSOService.authenticateSignUpWithRedirect(
        "oauth_github",
        DEFAULT_REDIRECT,
        null,
        null,
        null,
        null,
      )
    }
    coVerify(exactly = 2) {
      SSOService.authenticateSignUpWithRedirect(
        "enterprise_sso",
        DEFAULT_REDIRECT,
        null,
        "user@company.com",
        null,
        null,
      )
    }
  }

  /**
   * Asserts that the facade and then the older entry point each sent every [expected] request, in
   * order. Both layers share one implementation, so comparing them with each other alone would not
   * catch a change to the request itself.
   */
  private fun assertEachLayerSent(
    requests: List<Map<String, String>>,
    vararg expected: Map<String, String>,
  ) {
    assertEquals(expected.flatMap { listOf(it, it) }, requests)
  }

  private companion object {
    val DEFAULT_REDIRECT = com.clerk.api.sso.RedirectConfiguration.DEFAULT_REDIRECT_URL
  }
}
