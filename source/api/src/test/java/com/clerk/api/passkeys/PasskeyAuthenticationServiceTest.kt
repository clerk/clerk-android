package com.clerk.api.passkeys

import android.content.Context
import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.NoCredentialException
import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.SignInApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.attemptFirstFactor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import java.lang.ref.WeakReference
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PasskeyAuthenticationServiceTest {

  private lateinit var mockContext: Context
  private lateinit var mockCredentialManager: PasskeyCredentialManager
  private lateinit var mockGetCredentialResponse: GetCredentialResponse
  private lateinit var mockSignIn: SignIn
  private lateinit var mockVerification: Verification
  private lateinit var mockPublicKeyCredential: PublicKeyCredential
  private lateinit var mockPasswordCredential: PasswordCredential
  private lateinit var mockCustomCredential: CustomCredential

  @Before
  fun setup() {
    mockContext = mockk(relaxed = true)
    mockCredentialManager = mockk(relaxed = true)
    mockGetCredentialResponse = mockk(relaxed = true)
    mockSignIn = mockk(relaxed = true)
    mockVerification = mockk(relaxed = true)
    mockPublicKeyCredential = mockk(relaxed = true)
    mockPasswordCredential = mockk(relaxed = true)
    mockCustomCredential = mockk(relaxed = true)

    // Mock Clerk application context
    mockkObject(Clerk)
    every { Clerk.applicationContext } returns WeakReference(mockContext)
    every { Clerk.baseUrl } returns "https://test.clerk.com"

    // Mock ClerkApi and its nested objects
    mockkObject(ClerkApi)
    val mockSignInApi = mockk<SignInApi>(relaxed = true)
    every { ClerkApi.signIn } returns mockSignInApi

    // Set up the mock credential manager in the service
    GoogleCredentialAuthenticationService.setCredentialManager(mockCredentialManager)
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Ignore
  @Test
  fun `signInWithPasskey succeeds with public key credential`() = runTest {
    // Given
    val nonce = """{"challenge":"test-challenge"}"""
    val authResponseJson = """{"type":"webauthn.get","response":"test-response"}"""

    every { mockSignIn.firstFactorVerification } returns mockVerification
    every { mockVerification.nonce } returns nonce
    every { mockGetCredentialResponse.credential } returns mockPublicKeyCredential
    every { mockPublicKeyCredential.authenticationResponseJson } returns authResponseJson

    coEvery { ClerkApi.signIn.createSignIn(any()) } returns ClerkResult.success(mockSignIn)
    coEvery { mockCredentialManager.getCredential(any(), any()) } returns mockGetCredentialResponse
    coEvery { mockSignIn.attemptFirstFactor(any()) } returns ClerkResult.success(mockSignIn)

    // When
    val result =
      GoogleCredentialAuthenticationService.signInWithGoogleCredential(
        credentialTypes = listOf(SignIn.CredentialType.PASSKEY)
      )

    // Then
    assertTrue(result is ClerkResult.Success)
    assertEquals(mockSignIn, (result as ClerkResult.Success).value)
    coVerify { ClerkApi.signIn.createSignIn(mapOf("strategy" to "passkey")) }
    coVerify { mockCredentialManager.getCredential(mockContext, any()) }
    coVerify { mockSignIn.attemptFirstFactor(any()) }
  }

  @Test
  fun `signInWithPasskey handles password credential`() = runTest {
    // Given
    val nonce = """{"challenge":"test-challenge"}"""

    every { mockSignIn.firstFactorVerification } returns mockVerification
    every { mockVerification.nonce } returns nonce
    every { mockGetCredentialResponse.credential } returns mockPasswordCredential

    coEvery { ClerkApi.signIn.createSignIn(any()) } returns ClerkResult.success(mockSignIn)
    coEvery { mockCredentialManager.getCredential(any(), any()) } returns mockGetCredentialResponse

    // When
    val result =
      GoogleCredentialAuthenticationService.signInWithGoogleCredential(
        credentialTypes = listOf(SignIn.CredentialType.PASSKEY)
      )

    // Then
    assertTrue(result is ClerkResult.Success)
    assertEquals(mockSignIn, (result as ClerkResult.Success).value)
  }

  @Test
  fun `signInWithPasskey returns error when SignIn creation fails`() = runTest {
    // Given
    val error =
      Error(
        code = "signin_error",
        message = "Failed to create signin",
        longMessage = "SignIn creation failed",
      )
    val errorResponse = ClerkErrorResponse(errors = listOf(error), clerkTraceId = "test-trace")

    coEvery { ClerkApi.signIn.createSignIn(any()) } returns ClerkResult.apiFailure(errorResponse)

    // When
    val result =
      GoogleCredentialAuthenticationService.signInWithGoogleCredential(
        listOf(SignIn.CredentialType.PASSKEY)
      )

    // Then
    assertTrue(result is ClerkResult.Failure)
    assertEquals(errorResponse, (result as ClerkResult.Failure).error)
    coVerify(exactly = 0) { mockCredentialManager.getCredential(any(), any()) }
  }

  @Test
  fun `signInWithPasskey handles NoCredentialException`() = runTest {
    // Given
    val nonce = """{"challenge":"test-challenge"}"""
    val exception = NoCredentialException("No credentials available")

    every { mockSignIn.firstFactorVerification } returns mockVerification
    every { mockVerification.nonce } returns nonce

    coEvery { ClerkApi.signIn.createSignIn(any()) } returns ClerkResult.success(mockSignIn)
    coEvery { mockCredentialManager.getCredential(any(), any()) } throws exception

    // When
    val result =
      GoogleCredentialAuthenticationService.signInWithGoogleCredential(
        listOf(SignIn.CredentialType.PASSKEY)
      )

    // Then
    assertTrue(result is ClerkResult.Failure)
    assertEquals(ClerkResult.Failure.ErrorType.UNKNOWN, (result as ClerkResult.Failure).errorType)
  }

  @Test
  fun `signInWithPasskey returns error for unknown credential type`() = runTest {
    // Given
    val nonce = """{"challenge":"test-challenge"}"""
    val unknownCredential = mockk<Credential>(relaxed = true)

    every { mockSignIn.firstFactorVerification } returns mockVerification
    every { mockVerification.nonce } returns nonce
    every { mockGetCredentialResponse.credential } returns unknownCredential

    coEvery { ClerkApi.signIn.createSignIn(any()) } returns ClerkResult.success(mockSignIn)
    coEvery { mockCredentialManager.getCredential(any(), any()) } returns mockGetCredentialResponse

    // When
    val result =
      GoogleCredentialAuthenticationService.signInWithGoogleCredential(
        listOf(SignIn.CredentialType.PASSKEY)
      )

    // Then
    assertTrue(result is ClerkResult.Failure)
    assertEquals(ClerkResult.Failure.ErrorType.UNKNOWN, (result as ClerkResult.Failure).errorType)
  }
}
