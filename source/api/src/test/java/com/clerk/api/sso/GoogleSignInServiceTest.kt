package com.clerk.api.sso

import android.os.Bundle
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.NoCredentialException
import com.clerk.api.Clerk
import com.clerk.api.auth.Auth
import com.clerk.api.auth.createSignIn
import com.clerk.api.auth.createSignUp
import com.clerk.api.credentials.CredentialFlowException
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.environment.DisplayConfig
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signin.SignIn
import com.clerk.api.signup.SignUp
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
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
class GoogleSignInServiceTest {

  // Captured once so verifications count calls on Auth, not reads of the mocked Clerk.auth.
  private val auth: Auth = Clerk.auth

  private lateinit var mockGoogleCredentialManager: GoogleCredentialManager
  private lateinit var mockGetCredentialResponse: GetCredentialResponse
  private lateinit var mockCustomCredential: CustomCredential
  private lateinit var mockBundle: Bundle
  private lateinit var mockSignIn: SignIn
  private lateinit var mockSignUp: SignUp
  private lateinit var mockEnvironment: Environment
  private lateinit var mockDisplayConfig: DisplayConfig
  private lateinit var googleSignInService: GoogleSignInService

  @Before
  fun setup() {
    mockGoogleCredentialManager = mockk(relaxed = true)
    mockGetCredentialResponse = mockk(relaxed = true)
    mockCustomCredential = mockk(relaxed = true)
    mockBundle = mockk(relaxed = true)
    mockSignIn = mockk(relaxed = true)
    mockSignUp = mockk(relaxed = true)
    mockEnvironment = mockk(relaxed = true)
    mockDisplayConfig = mockk(relaxed = true)
    every { mockSignUp.verifications } returns emptyMap()

    mockkObject(Clerk)
    every { Clerk.environment } returns mockEnvironment
    every { mockEnvironment.displayConfig } returns mockDisplayConfig
    every { mockDisplayConfig.googleOneTapClientId } returns "test_client_id"

    mockkObject(ClerkApi)
    every { ClerkApi.signIn } returns mockk(relaxed = true)

    mockkStatic("com.clerk.api.auth.AuthFlowsKt")

    googleSignInService = GoogleSignInService(mockGoogleCredentialManager)
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `signInWithGoogle succeeds when authentication is successful`() = runTest {
    val idToken = "test_id_token"

    every { mockGetCredentialResponse.credential } returns mockCustomCredential
    every { mockCustomCredential.type } returns
      GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    every { mockCustomCredential.data } returns mockBundle

    coEvery { mockGoogleCredentialManager.getSignInWithGoogleCredential() } returns
      mockGetCredentialResponse
    every { mockGoogleCredentialManager.getIdTokenFromCredential(mockBundle) } returns idToken

    coEvery { ClerkApi.signIn.authenticateWithGoogle(token = idToken) } returns
      ClerkResult.success(mockSignIn)

    val result = googleSignInService.signInWithGoogle()

    assertTrue(result is ClerkResult.Success)
    val oauthResult = (result as ClerkResult.Success).value
    assertEquals(mockSignIn, oauthResult.signIn)
    assertEquals(null, oauthResult.signUp)
    assertEquals(ResultType.SIGN_IN, oauthResult.resultType)
  }

  @Test
  fun `signInWithGoogle creates account when external_account_not_found error occurs`() = runTest {
    val idToken = "test_id_token"
    val error =
      Error(
        code = "external_account_not_found",
        message = "Account not found",
        longMessage = "External account not found for this user",
      )
    val errorResponse = ClerkErrorResponse(errors = listOf(error), clerkTraceId = "test_trace_id")

    every { mockGetCredentialResponse.credential } returns mockCustomCredential
    every { mockCustomCredential.type } returns
      GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    every { mockCustomCredential.data } returns mockBundle

    coEvery { mockGoogleCredentialManager.getSignInWithGoogleCredential() } returns
      mockGetCredentialResponse
    every { mockGoogleCredentialManager.getIdTokenFromCredential(mockBundle) } returns idToken

    coEvery { ClerkApi.signIn.authenticateWithGoogle(token = idToken) } returns
      ClerkResult.apiFailure(errorResponse)
    coEvery { auth.createSignUp(any<SignUp.CreateParams.GoogleOneTap>()) } returns
      ClerkResult.success(mockSignUp)

    val result = googleSignInService.signInWithGoogle()

    assertTrue(result is ClerkResult.Success)
    val oauthResult = (result as ClerkResult.Success).value
    assertEquals(null, oauthResult.signIn)
    assertEquals(mockSignUp, oauthResult.signUp)
    assertEquals(ResultType.SIGN_UP, oauthResult.resultType)
  }

  @Test
  fun `signInWithGoogle transfers created sign-up back to sign-in when external account exists`() =
    runTest {
      val idToken = "test_id_token"
      val error =
        Error(
          code = "external_account_not_found",
          message = "Account not found",
          longMessage = "External account not found for this user",
        )
      val errorResponse = ClerkErrorResponse(errors = listOf(error), clerkTraceId = "test_trace_id")
      val transferableSignUp =
        testSignUp(
          verifications =
            mapOf("external_account" to Verification(status = Verification.Status.TRANSFERABLE))
        )

      every { mockGetCredentialResponse.credential } returns mockCustomCredential
      every { mockCustomCredential.type } returns
        GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
      every { mockCustomCredential.data } returns mockBundle

      coEvery { mockGoogleCredentialManager.getSignInWithGoogleCredential() } returns
        mockGetCredentialResponse
      every { mockGoogleCredentialManager.getIdTokenFromCredential(mockBundle) } returns idToken

      coEvery { ClerkApi.signIn.authenticateWithGoogle(token = idToken) } returns
        ClerkResult.apiFailure(errorResponse)
      coEvery { auth.createSignUp(any<SignUp.CreateParams.GoogleOneTap>()) } returns
        ClerkResult.success(transferableSignUp)
      coEvery { auth.createSignIn(any<SignIn.CreateParams.Strategy.Transfer>()) } returns
        ClerkResult.success(mockSignIn)

      val result = googleSignInService.signInWithGoogle()

      assertTrue(result is ClerkResult.Success)
      val oauthResult = (result as ClerkResult.Success).value
      assertEquals(mockSignIn, oauthResult.signIn)
      assertEquals(null, oauthResult.signUp)
      assertEquals(ResultType.SIGN_IN, oauthResult.resultType)
      coVerify(exactly = 1) {
        auth.createSignIn(any<SignIn.CreateParams.Strategy.Transfer>())
      }
    }

  @Test
  fun `signInWithGoogle returns error when authentication fails with other error`() = runTest {
    val idToken = "test_id_token"
    val error =
      Error(
        code = "invalid_token",
        message = "Invalid token",
        longMessage = "The provided token is invalid",
      )
    val errorResponse = ClerkErrorResponse(errors = listOf(error), clerkTraceId = "test_trace_id")

    every { mockGetCredentialResponse.credential } returns mockCustomCredential
    every { mockCustomCredential.type } returns
      GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    every { mockCustomCredential.data } returns mockBundle

    coEvery { mockGoogleCredentialManager.getSignInWithGoogleCredential() } returns
      mockGetCredentialResponse
    every { mockGoogleCredentialManager.getIdTokenFromCredential(mockBundle) } returns idToken

    coEvery { ClerkApi.signIn.authenticateWithGoogle(token = idToken) } returns
      ClerkResult.apiFailure(errorResponse)

    val result = googleSignInService.signInWithGoogle()

    assertTrue(result is ClerkResult.Failure)
    val failure = result as ClerkResult.Failure
    assertEquals(errorResponse, failure.error)
  }

  @Test
  fun `signInWithGoogle returns error when credential type is unsupported`() = runTest {
    every { mockGetCredentialResponse.credential } returns mockCustomCredential
    every { mockCustomCredential.type } returns "unsupported_type"

    coEvery { mockGoogleCredentialManager.getSignInWithGoogleCredential() } returns
      mockGetCredentialResponse

    val result = googleSignInService.signInWithGoogle()

    assertTrue(result is ClerkResult.Failure)
    val failure = result as ClerkResult.Failure
    assertEquals(ClerkResult.Failure.ErrorType.UNKNOWN, failure.errorType)
    assertTrue(failure.throwable is IllegalStateException)
    assertEquals("Unsupported credential type: unsupported_type", failure.throwable?.message)
  }

  @Test
  fun `signInWithGoogle returns error when GetCredentialException is thrown`() = runTest {
    val exception = GetCredentialUnknownException("Credential retrieval failed")

    coEvery { mockGoogleCredentialManager.getSignInWithGoogleCredential() } throws exception

    val result = googleSignInService.signInWithGoogle()

    assertTrue(result is ClerkResult.Failure)
    val failure = result as ClerkResult.Failure
    assertEquals(ClerkResult.Failure.ErrorType.UNKNOWN, failure.errorType)
  }

  @Test
  fun `signInWithGoogle classifies missing Google account`() = runTest {
    coEvery { mockGoogleCredentialManager.getSignInWithGoogleCredential() } throws
      NoCredentialException("No credentials available")

    val result = googleSignInService.signInWithGoogle()

    assertTrue(result is ClerkResult.Failure)
    val failure = result as ClerkResult.Failure
    assertTrue(failure.throwable is CredentialFlowException.NoGoogleAccount)
  }

  @Test
  fun `signInWithGoogle classifies user cancellation`() = runTest {
    coEvery { mockGoogleCredentialManager.getSignInWithGoogleCredential() } throws
      GetCredentialCancellationException()

    val result = googleSignInService.signInWithGoogle()

    assertTrue(result is ClerkResult.Failure)
    val failure = result as ClerkResult.Failure
    assertTrue(failure.throwable is CredentialFlowException.UserCancelled)
  }

  @Test
  fun `signInWithGoogle verifies correct SignUp CreateParams are used`() = runTest {
    val idToken = "test_id_token"
    val error =
      Error(
        code = "external_account_not_found",
        message = "Account not found",
        longMessage = "External account not found for this user",
      )
    val errorResponse = ClerkErrorResponse(errors = listOf(error), clerkTraceId = "test_trace_id")
    val createParamsSlot = slot<SignUp.CreateParams.GoogleOneTap>()

    every { mockGetCredentialResponse.credential } returns mockCustomCredential
    every { mockCustomCredential.type } returns
      GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    every { mockCustomCredential.data } returns mockBundle

    coEvery { mockGoogleCredentialManager.getSignInWithGoogleCredential() } returns
      mockGetCredentialResponse
    every { mockGoogleCredentialManager.getIdTokenFromCredential(mockBundle) } returns idToken

    coEvery { ClerkApi.signIn.authenticateWithGoogle(token = idToken) } returns
      ClerkResult.apiFailure(errorResponse)
    coEvery { auth.createSignUp(capture(createParamsSlot)) } returns ClerkResult.success(mockSignUp)

    googleSignInService.signInWithGoogle()

    assertEquals(idToken, createParamsSlot.captured.token)
  }

  @Test
  fun `signUpWithGoogle transfers existing external account to sign-in`() = runTest {
    val idToken = "test_id_token"
    val transferableSignUp =
      testSignUp(
        verifications =
          mapOf("external_account" to Verification(status = Verification.Status.TRANSFERABLE))
      )

    every { mockGetCredentialResponse.credential } returns mockCustomCredential
    every { mockCustomCredential.type } returns
      GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    every { mockCustomCredential.data } returns mockBundle

    coEvery { mockGoogleCredentialManager.getSignInWithGoogleCredential() } returns
      mockGetCredentialResponse
    every { mockGoogleCredentialManager.getIdTokenFromCredential(mockBundle) } returns idToken

    coEvery { auth.createSignUp(any<SignUp.CreateParams.GoogleOneTap>()) } returns
      ClerkResult.success(transferableSignUp)
    coEvery { auth.createSignIn(any<SignIn.CreateParams.Strategy.Transfer>()) } returns
      ClerkResult.success(mockSignIn)

    val result = googleSignInService.signUpWithGoogle()

    assertTrue(result is ClerkResult.Success)
    val oauthResult = (result as ClerkResult.Success).value
    assertEquals(mockSignIn, oauthResult.signIn)
    assertEquals(null, oauthResult.signUp)
    assertEquals(ResultType.SIGN_IN, oauthResult.resultType)
    coVerify(exactly = 1) { auth.createSignIn(any<SignIn.CreateParams.Strategy.Transfer>()) }
  }

  private fun testSignUp(verifications: Map<String, Verification?> = emptyMap()): SignUp {
    return SignUp(
      id = "sign_up_123",
      requiredFields = emptyList(),
      optionalFields = emptyList(),
      missingFields = emptyList(),
      unverifiedFields = emptyList(),
      verifications = verifications,
      passwordEnabled = false,
    )
  }
}
