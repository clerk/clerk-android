package com.clerk.sso

import android.content.Context
import android.os.Bundle
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialUnknownException
import com.clerk.Clerk
import com.clerk.network.ClerkApi
import com.clerk.network.model.environment.DisplayConfig
import com.clerk.network.model.environment.Environment
import com.clerk.network.model.error.ClerkErrorResponse
import com.clerk.network.model.error.Error
import com.clerk.network.serialization.ClerkResult
import com.clerk.signin.SignIn
import com.clerk.signup.SignUp
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GoogleSignInServiceTest {

  private lateinit var mockContext: Context
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
    mockContext = mockk(relaxed = true)
    mockGoogleCredentialManager = mockk(relaxed = true)
    mockGetCredentialResponse = mockk(relaxed = true)
    mockCustomCredential = mockk(relaxed = true)
    mockBundle = mockk(relaxed = true)
    mockSignIn = mockk(relaxed = true)
    mockSignUp = mockk(relaxed = true)
    mockEnvironment = mockk(relaxed = true)
    mockDisplayConfig = mockk(relaxed = true)

    // Mock Clerk object and its environment
    mockkObject(Clerk)
    every { Clerk.environment } returns mockEnvironment
    every { mockEnvironment.displayConfig } returns mockDisplayConfig
    every { mockDisplayConfig.googleOneTapClientId } returns "test_client_id"

    // Mock the ClerkApi
    mockkObject(ClerkApi)
    every { ClerkApi.signIn } returns mockk(relaxed = true)

    // Mock SignUp.create
    mockkObject(SignUp.Companion)

    // Create service instance with mocked credential manager
    googleSignInService = GoogleSignInService(mockGoogleCredentialManager)
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `signInWithGoogle succeeds when authentication is successful`() = runTest {
    // Given
    val idToken = "test_id_token"

    every { mockGetCredentialResponse.credential } returns mockCustomCredential
    every { mockCustomCredential.type } returns
      GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    every { mockCustomCredential.data } returns mockBundle

    coEvery { mockGoogleCredentialManager.getSignInWithGoogleCredential(mockContext) } returns
      mockGetCredentialResponse
    every { mockGoogleCredentialManager.getIdTokenFromCredential(mockBundle) } returns idToken

    coEvery { ClerkApi.signIn.authenticateWithGoogle(token = idToken) } returns
      ClerkResult.success(mockSignIn)

    // When
    val result = googleSignInService.signInWithGoogle(mockContext)

    // Then
    assertTrue(result is ClerkResult.Success)
    val oauthResult = (result as ClerkResult.Success).value
    assertEquals(mockSignIn, oauthResult.signIn)
    assertEquals(null, oauthResult.signUp)
    assertEquals(ResultType.SIGN_IN, oauthResult.resultType)
  }

  @Test
  fun `signInWithGoogle creates account when external_account_not_found error occurs`() = runTest {
    // Given
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

    coEvery { mockGoogleCredentialManager.getSignInWithGoogleCredential(mockContext) } returns
      mockGetCredentialResponse
    every { mockGoogleCredentialManager.getIdTokenFromCredential(mockBundle) } returns idToken

    coEvery { ClerkApi.signIn.authenticateWithGoogle(token = idToken) } returns
      ClerkResult.apiFailure(errorResponse)
    coEvery { SignUp.create(any<SignUp.CreateParams.GoogleOneTap>()) } returns
      ClerkResult.success(mockSignUp)

    // When
    val result = googleSignInService.signInWithGoogle(mockContext)

    // Then
    assertTrue(result is ClerkResult.Success)
    val oauthResult = (result as ClerkResult.Success).value
    assertEquals(null, oauthResult.signIn)
    assertEquals(mockSignUp, oauthResult.signUp)
    assertEquals(ResultType.SIGN_UP, oauthResult.resultType)
  }

  @Test
  fun `signInWithGoogle returns error when authentication fails with other error`() = runTest {
    // Given
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

    coEvery { mockGoogleCredentialManager.getSignInWithGoogleCredential(mockContext) } returns
      mockGetCredentialResponse
    every { mockGoogleCredentialManager.getIdTokenFromCredential(mockBundle) } returns idToken

    coEvery { ClerkApi.signIn.authenticateWithGoogle(token = idToken) } returns
      ClerkResult.apiFailure(errorResponse)

    // When
    val result = googleSignInService.signInWithGoogle(mockContext)

    // Then
    assertTrue(result is ClerkResult.Failure)
    val failure = result as ClerkResult.Failure
    assertEquals(errorResponse, failure.error)
  }

  @Test
  fun `signInWithGoogle returns error when credential type is unsupported`() = runTest {
    // Given
    every { mockGetCredentialResponse.credential } returns mockCustomCredential
    every { mockCustomCredential.type } returns "unsupported_type"

    coEvery { mockGoogleCredentialManager.getSignInWithGoogleCredential(mockContext) } returns
      mockGetCredentialResponse

    // When & Then
    val exception =
      assertThrows(IllegalStateException::class.java) {
        runBlocking { googleSignInService.signInWithGoogle(mockContext) }
      }
    assertEquals("Unsupported credential type: unsupported_type", exception.message)
  }

  @Test
  fun `signInWithGoogle returns error when GetCredentialException is thrown`() = runTest {
    // Given
    val exception = GetCredentialUnknownException("Credential retrieval failed")

    coEvery { mockGoogleCredentialManager.getSignInWithGoogleCredential(mockContext) } throws
      exception

    // When
    val result = googleSignInService.signInWithGoogle(mockContext)

    // Then
    assertTrue(result is ClerkResult.Failure)
    val failure = result as ClerkResult.Failure
    assertEquals(ClerkResult.Failure.ErrorType.UNKNOWN, failure.errorType)
  }

  @Test
  fun `signInWithGoogle verifies correct SignUp CreateParams are used`() = runTest {
    // Given
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

    coEvery { mockGoogleCredentialManager.getSignInWithGoogleCredential(mockContext) } returns
      mockGetCredentialResponse
    every { mockGoogleCredentialManager.getIdTokenFromCredential(mockBundle) } returns idToken

    coEvery { ClerkApi.signIn.authenticateWithGoogle(token = idToken) } returns
      ClerkResult.apiFailure(errorResponse)
    coEvery { SignUp.create(capture(createParamsSlot)) } returns ClerkResult.success(mockSignUp)

    // When
    googleSignInService.signInWithGoogle(mockContext)

    // Then
    assertEquals(idToken, createParamsSlot.captured.token)
  }
}
