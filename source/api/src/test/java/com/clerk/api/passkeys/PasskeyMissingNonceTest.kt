package com.clerk.api.passkeys

import android.app.Activity
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialResponse
import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.SessionApi
import com.clerk.api.network.api.SignInApi
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.LocalFailureCodes
import com.clerk.api.session.Session
import com.clerk.api.session.SessionVerification
import com.clerk.api.signin.SignIn
import com.clerk.api.signup.SignUp
import com.clerk.api.sso.GoogleSignInService
import com.clerk.api.sso.OAuthResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PasskeyMissingNonceTest {
  private val credentialManager = mockk<PasskeyCredentialManager>(relaxed = true)
  private val signInApi = mockk<SignInApi>(relaxed = true)
  private val sessionApi = mockk<SessionApi>(relaxed = true)

  @Before
  fun setup() {
    mockkObject(Clerk)
    every { Clerk.credentialActivity() } returns mockk<Activity>(relaxed = true)
    every { Clerk.baseUrl } returns "https://test.clerk.com"
    mockkObject(ClerkApi)
    every { ClerkApi.signIn } returns signInApi
    every { ClerkApi.session } returns sessionApi
    GoogleCredentialAuthenticationService.setCredentialManager(credentialManager)
  }

  @After
  fun tearDown() {
    GoogleCredentialAuthenticationService.setGoogleSignInService(GoogleSignInService())
    unmockkAll()
  }

  @Test
  fun `signInWithPasskey returns a coded failure when the first factor nonce is missing`() =
    runTest {
      coEvery { signInApi.createSignIn(any()) } returns
        ClerkResult.success(SignIn(id = "sign_in_123"))

      val result =
        GoogleCredentialAuthenticationService.signInWithGoogleCredential(
          listOf(SignIn.CredentialType.PASSKEY)
        )

      assertMissingNonceFailure(result)
      coVerify(exactly = 0) { credentialManager.getCredential(any(), any()) }
    }

  @Test
  fun `signInWithGoogleCredential does not need a nonce without the passkey type`() = runTest {
    val googleCredential = mockk<CustomCredential>(relaxed = true)
    val response = mockk<GetCredentialResponse>()
    every { response.credential } returns googleCredential
    val environment = mockk<Environment>(relaxed = true)
    every { environment.displayConfig.googleOneTapClientId } returns "google-client-id"
    every { Clerk.environment } returns environment
    val signUp = mockk<SignUp>(relaxed = true)
    every { signUp.status } returns SignUp.Status.COMPLETE
    every { signUp.createdSessionId } returns "sess_new"
    val googleSignInService = mockk<GoogleSignInService>()
    coEvery { googleSignInService.handleSignInResult(googleCredential, any()) } returns
      ClerkResult.success(OAuthResult(signUp = signUp))
    GoogleCredentialAuthenticationService.setGoogleSignInService(googleSignInService)
    coEvery { signInApi.createSignIn(any()) } returns
      ClerkResult.success(SignIn(id = "sign_in_123"))
    coEvery { credentialManager.getCredential(any(), any()) } returns response

    val result =
      GoogleCredentialAuthenticationService.signInWithGoogleCredential(
        credentialTypes = listOf(SignIn.CredentialType.GOOGLE)
      )

    assertTrue(result is ClerkResult.Success)
  }

  @Test
  fun `authenticateWithPasskey returns a coded failure when the second factor nonce is missing`() =
    runTest {
      val signIn =
        SignIn(
          id = "sign_in_123",
          status = SignIn.Status.NEEDS_SECOND_FACTOR,
          supportedSecondFactors = listOf(Factor(strategy = "passkey")),
        )
      coEvery {
        signInApi.prepareSecondFactor("sign_in_123", mapOf("strategy" to "passkey"))
      } returns
        ClerkResult.success(
          signIn.copy(secondFactorVerification = Verification(nonce = null, strategy = "passkey"))
        )

      val result = GoogleCredentialAuthenticationService.authenticateWithPasskey(signIn)

      assertMissingNonceFailure(result)
      coVerify(exactly = 0) { credentialManager.getCredential(any(), any()) }
    }

  @Test
  fun `verifySessionWithPasskey returns a coded failure when the prepared nonce is missing`() =
    runTest {
      coEvery {
        sessionApi.prepareFirstFactorVerification("sess_123", mapOf("strategy" to "passkey"))
      } returns
        ClerkResult.success(
          SessionVerification(
            id = "ver_123",
            status = SessionVerification.Status.NEEDS_FIRST_FACTOR,
            level = SessionVerification.Level.FIRST_FACTOR,
          )
        )

      val result = GoogleCredentialAuthenticationService.verifySessionWithPasskey(SESSION)

      assertMissingNonceFailure(result)
      coVerify(exactly = 0) { credentialManager.getCredential(any(), any()) }
    }

  @Test
  fun `verifySessionWithPasskey second factor returns a coded failure when the nonce is missing`() =
    runTest {
      coEvery {
        sessionApi.prepareSecondFactorVerification("sess_123", mapOf("strategy" to "passkey"))
      } returns
        ClerkResult.success(
          SessionVerification(
            id = "ver_123",
            status = SessionVerification.Status.NEEDS_SECOND_FACTOR,
            level = SessionVerification.Level.SECOND_FACTOR,
          )
        )

      val result =
        GoogleCredentialAuthenticationService.verifySessionWithPasskey(
          SESSION,
          level = SessionVerification.Level.SECOND_FACTOR,
        )

      assertMissingNonceFailure(result)
      coVerify(exactly = 0) { credentialManager.getCredential(any(), any()) }
    }

  private fun assertMissingNonceFailure(result: ClerkResult<*, ClerkErrorResponse>) {
    val failure = result as ClerkResult.Failure
    assertEquals(ClerkResult.Failure.ErrorType.API, failure.errorType)
    assertEquals(LocalFailureCodes.MISSING_RESOURCE_DATA, failure.error?.errors?.single()?.code)
  }

  private companion object {
    val SESSION =
      Session(
        id = "sess_123",
        status = Session.SessionStatus.ACTIVE,
        expireAt = 0L,
        lastActiveAt = 0L,
        createdAt = 0L,
        updatedAt = 0L,
      )
  }
}
