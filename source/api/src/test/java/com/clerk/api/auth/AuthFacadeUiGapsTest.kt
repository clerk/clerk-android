package com.clerk.api.auth

import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.SignInApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.passkeys.PasskeyService
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.authenticateWithEnterpriseSso
import com.clerk.api.signin.authenticateWithOAuth
import com.clerk.api.signin.sendResetPasswordEmailCode
import com.clerk.api.signin.sendResetPasswordPhoneCode
import com.clerk.api.signup.SignUp
import com.clerk.api.sso.GoogleSignInService
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.sso.OAuthResult
import com.clerk.api.sso.SSOService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlin.coroutines.Continuation
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Covers the `Clerk.auth` additions the prebuilt UI needs to move off the older entry points. */
class AuthFacadeUiGapsTest {

  private val signInApi = mockk<SignInApi>()
  private val signIn = SignIn(id = SIGN_IN_ID, status = SignIn.Status.NEEDS_FIRST_FACTOR)
  private val oauthResult = OAuthResult(signIn = signIn)

  @Before
  fun setup() {
    Clerk.updateClient(Client())
    mockkObject(ClerkApi)
    every { ClerkApi.signIn } returns signInApi
  }

  @After
  fun tearDown() {
    unmockkAll()
    Clerk.updateClient(Client())
  }

  @Test
  fun `signInWithOAuth forwards transferable and redirectUrl`() = runTest {
    mockkObject(SSOService)
    coEvery {
      SSOService.authenticateWithRedirect(any(), any(), any(), any(), any(), any())
    } returns ClerkResult.success(oauthResult)

    Auth().signInWithOAuth(OAuthProvider.GOOGLE, transferable = false, redirectUrl = REDIRECT)

    coVerify(exactly = 1) {
      SSOService.authenticateWithRedirect("oauth_google", REDIRECT, null, null, null, false)
    }
  }

  @Test
  fun `signUpWithOAuth forwards redirectUrl and unsafe metadata`() = runTest {
    mockkObject(SSOService)
    val metadata = mapOf<String, Any>("plan" to "pro")
    coEvery {
      SSOService.authenticateSignUpWithRedirect(any(), any(), any(), any(), any(), any())
    } returns ClerkResult.success(OAuthResult(signUp = mockk<SignUp>(relaxed = true)))

    Auth().signUpWithOAuth(OAuthProvider.GOOGLE, redirectUrl = REDIRECT, unsafeMetadata = metadata)

    coVerify(exactly = 1) {
      SSOService.authenticateSignUpWithRedirect(
        "oauth_google",
        REDIRECT,
        null,
        null,
        null,
        metadata,
      )
    }
  }

  @Test
  fun `signInWithPasskey forwards preferImmediatelyAvailableCredentials`() = runTest {
    mockkObject(PasskeyService)
    coEvery { PasskeyService.signInWithPasskey(any(), any()) } returns ClerkResult.success(signIn)

    Auth().signInWithPasskey(preferImmediatelyAvailableCredentials = true)

    coVerify(exactly = 1) {
      PasskeyService.signInWithPasskey(
        allowedCredentialIds = emptyList(),
        preferImmediatelyAvailableCredentials = true,
      )
    }
  }

  @Test
  fun `signInWithGoogleOneTap forwards transferable`() = runTest {
    mockkConstructor(GoogleSignInService::class)
    coEvery { anyConstructed<GoogleSignInService>().signInWithGoogle(false) } returns
      ClerkResult.success(oauthResult)

    Auth().signInWithGoogleOneTap(transferable = false)

    coVerify(exactly = 1) { anyConstructed<GoogleSignInService>().signInWithGoogle(false) }
  }

  @Test
  fun `signIn accepts an identifier of unknown type`() = runTest {
    coEvery { signInApi.createSignIn(any()) } returns ClerkResult.success(signIn)

    Auth().signIn { identifier = "typed-into-one-field" }

    coVerify(exactly = 1) {
      signInApi.createSignIn(match { it["identifier"] == "typed-into-one-field" })
    }
  }

  @Test
  fun `authenticateWithOAuth prepares the factor on this sign-in and opens its redirect`() =
    runTest {
      val prepared = signIn.withRedirectUrl()
      mockkObject(SSOService)
      coEvery { signInApi.prepareSignInFirstFactor(any(), any()) } returns
        ClerkResult.success(prepared)
      coEvery { SSOService.authenticateWithPreparedRedirect(any(), any(), any(), any()) } returns
        ClerkResult.success(oauthResult)

      val result =
        signIn.authenticateWithOAuth(
          OAuthProvider.GITHUB,
          transferable = false,
          redirectUrl = REDIRECT,
        )

      assertTrue(result is ClerkResult.Success)
      coVerify(exactly = 1) {
        signInApi.prepareSignInFirstFactor(
          SIGN_IN_ID,
          mapOf("strategy" to "oauth_github", "redirect_url" to REDIRECT),
        )
      }
      coVerify(exactly = 1) {
        SSOService.authenticateWithPreparedRedirect(EXTERNAL_URL, false, any(), any())
      }
    }

  @Test
  fun `authenticateWithEnterpriseSso prepares the enterprise factor on this sign-in`() = runTest {
    mockkObject(SSOService)
    coEvery { signInApi.prepareSignInFirstFactor(any(), any()) } returns
      ClerkResult.success(signIn.withRedirectUrl())
    coEvery { SSOService.authenticateWithPreparedRedirect(any(), any(), any(), any()) } returns
      ClerkResult.success(oauthResult)

    signIn.authenticateWithEnterpriseSso(redirectUrl = REDIRECT)

    coVerify(exactly = 1) {
      signInApi.prepareSignInFirstFactor(
        SIGN_IN_ID,
        mapOf("strategy" to "enterprise_sso", "redirect_url" to REDIRECT),
      )
    }
    coVerify(exactly = 1) {
      SSOService.authenticateWithPreparedRedirect(EXTERNAL_URL, true, any(), any())
    }
  }

  @Test
  fun `sendResetPasswordEmailCode and PhoneCode prepare the reset factor by id`() = runTest {
    coEvery { signInApi.prepareSignInFirstFactor(any(), any()) } returns ClerkResult.success(signIn)

    signIn.sendResetPasswordEmailCode("email_123")
    signIn.sendResetPasswordPhoneCode("phone_123")

    coVerify(exactly = 1) {
      signInApi.prepareSignInFirstFactor(
        SIGN_IN_ID,
        mapOf("strategy" to "reset_password_email_code", "email_address_id" to "email_123"),
      )
    }
    coVerify(exactly = 1) {
      signInApi.prepareSignInFirstFactor(
        SIGN_IN_ID,
        mapOf("strategy" to "reset_password_phone_code", "phone_number_id" to "phone_123"),
      )
    }
  }

  @Test
  fun `sendResetPasswordEmailCode without a reset factor fails locally`() = runTest {
    val noResetFactor = signIn.copy(supportedFirstFactors = listOf(Factor(strategy = "password")))

    val result = noResetFactor.sendResetPasswordEmailCode()

    assertTrue(result is ClerkResult.Failure)
    coVerify(exactly = 0) { signInApi.prepareSignInFirstFactor(any(), any()) }
  }

  @Test
  fun `SignIn create with a passkey forwards preferImmediatelyAvailableCredentials`() = runTest {
    mockkObject(PasskeyService)
    coEvery { PasskeyService.signInWithPasskey(any(), any()) } returns ClerkResult.success(signIn)

    SignIn.create(
      SignIn.CreateParams.Strategy.Passkey(),
      preferImmediatelyAvailableCredentials = true,
    )

    coVerify(exactly = 1) {
      PasskeyService.signInWithPasskey(
        allowedCredentialIds = emptyList(),
        preferImmediatelyAvailableCredentials = true,
      )
    }
  }

  @Test
  fun `signIn rejects an identifier combined with a typed field`() = runTest {
    val error = runCatching {
      Auth().signIn {
        email = "user@example.com"
        identifier = "someone-else"
      }
    }

    assertTrue(error.exceptionOrNull() is IllegalArgumentException)
    coVerify(exactly = 0) { signInApi.createSignIn(any()) }
  }

  @Test
  fun `authenticateWithOAuth does not open a redirect when preparing the factor fails`() = runTest {
    mockkObject(SSOService)
    coEvery { signInApi.prepareSignInFirstFactor(any(), any()) } returns
      ClerkResult.apiFailure(ClerkErrorResponse(errors = emptyList()))

    val result = signIn.authenticateWithOAuth(OAuthProvider.GITHUB)

    assertTrue(result is ClerkResult.Failure)
    coVerify(exactly = 0) {
      SSOService.authenticateWithPreparedRedirect(any(), any(), any(), any())
    }
  }

  @Test
  fun `sendResetPasswordEmailCode without an id uses the reset factor's email address`() = runTest {
    coEvery { signInApi.prepareSignInFirstFactor(any(), any()) } returns ClerkResult.success(signIn)
    val withResetFactor =
      signIn.copy(
        supportedFirstFactors =
          listOf(Factor(strategy = "reset_password_email_code", emailAddressId = "email_456"))
      )

    withResetFactor.sendResetPasswordEmailCode()

    coVerify(exactly = 1) {
      signInApi.prepareSignInFirstFactor(
        SIGN_IN_ID,
        mapOf("strategy" to "reset_password_email_code", "email_address_id" to "email_456"),
      )
    }
  }

  @Test
  fun `widened facade methods keep their previous JVM signatures`() {
    val methods = Auth::class.java.declaredMethods
    fun hasSignature(name: String, vararg params: Class<*>) = methods.any {
      it.name == name && it.parameterTypes.toList() == params.toList()
    }

    assertTrue(hasSignature("signInWithOAuth", OAuthProvider::class.java, Continuation::class.java))
    assertTrue(hasSignature("signUpWithOAuth", OAuthProvider::class.java, Continuation::class.java))
    assertTrue(hasSignature("signInWithPasskey", Continuation::class.java))
    assertEquals(
      1,
      methods.count { it.name == "signInWithPasskey" && it.parameterTypes.size == 2 },
    )
  }

  private fun SignIn.withRedirectUrl() =
    copy(firstFactorVerification = Verification(externalVerificationRedirectUrl = EXTERNAL_URL))

  private companion object {
    const val SIGN_IN_ID = "sign_in_123"
    const val REDIRECT = "com.example.app://callback"
    const val EXTERNAL_URL = "https://accounts.example.com/sso"
  }
}
