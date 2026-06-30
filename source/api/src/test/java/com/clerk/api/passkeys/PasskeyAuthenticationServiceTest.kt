package com.clerk.api.passkeys

import android.app.Activity
import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.clerk.api.Clerk
import com.clerk.api.credentials.CredentialFlowException
import com.clerk.api.credentials.shouldSuppressAutomaticCredentialFlowError
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.SessionApi
import com.clerk.api.network.api.SignInApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.Session
import com.clerk.api.session.SessionVerification
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.attemptFirstFactor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PasskeyAuthenticationServiceTest {

  private lateinit var mockActivity: Activity
  private lateinit var mockCredentialManager: PasskeyCredentialManager
  private lateinit var mockGetCredentialResponse: GetCredentialResponse
  private lateinit var mockSignIn: SignIn
  private lateinit var mockVerification: Verification
  private lateinit var mockPublicKeyCredential: PublicKeyCredential
  private lateinit var mockPasswordCredential: PasswordCredential
  private lateinit var mockCustomCredential: CustomCredential

  @Before
  fun setup() {
    mockActivity = mockk(relaxed = true)
    mockCredentialManager = mockk(relaxed = true)
    mockGetCredentialResponse = mockk(relaxed = true)
    mockSignIn = mockk(relaxed = true)
    mockVerification = mockk(relaxed = true)
    mockPublicKeyCredential = mockk(relaxed = true)
    mockPasswordCredential = mockk(relaxed = true)
    mockCustomCredential = mockk(relaxed = true)

    // Mock Clerk application context
    mockkObject(Clerk)
    every { Clerk.credentialActivity() } returns mockActivity
    every { Clerk.baseUrl } returns "https://test.clerk.com"

    // Mock ClerkApi and its nested objects
    mockkObject(ClerkApi)
    val mockSignInApi = mockk<SignInApi>(relaxed = true)
    val mockSessionApi = mockk<SessionApi>(relaxed = true)
    every { ClerkApi.signIn } returns mockSignInApi
    every { ClerkApi.session } returns mockSessionApi

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
    coVerify { mockCredentialManager.getCredential(mockActivity, any()) }
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
  fun `signInWithPasskey can prefer immediately available credentials`() = runTest {
    val nonce = """{"challenge":"test-challenge"}"""
    val requestSlot = slot<GetCredentialRequest>()

    every { mockSignIn.firstFactorVerification } returns mockVerification
    every { mockVerification.nonce } returns nonce
    every { mockGetCredentialResponse.credential } returns mockPasswordCredential

    coEvery { ClerkApi.signIn.createSignIn(any()) } returns ClerkResult.success(mockSignIn)
    coEvery { mockCredentialManager.getCredential(any(), capture(requestSlot)) } returns
      mockGetCredentialResponse

    val result =
      GoogleCredentialAuthenticationService.signInWithGoogleCredential(
        credentialTypes = listOf(SignIn.CredentialType.PASSKEY),
        preferImmediatelyAvailableCredentials = true,
      )

    assertTrue(result is ClerkResult.Success)
    assertTrue(requestSlot.captured.preferImmediatelyAvailableCredentials)
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
    val failure = result as ClerkResult.Failure
    assertEquals(ClerkResult.Failure.ErrorType.UNKNOWN, failure.errorType)
    assertTrue(failure.throwable is CredentialFlowException.NoSavedCredential)
  }

  @Test
  fun `automatic signInWithPasskey clears suppressed current sign in`() = runTest {
    val nonce = """{"challenge":"test-challenge"}"""
    val client = Client(id = "client_123", signIn = mockSignIn)

    every { mockSignIn.id } returns "sign_in_123"
    every { mockSignIn.firstFactorVerification } returns mockVerification
    every { mockVerification.nonce } returns nonce
    every { Clerk.clientInitialized } returns true
    every { Clerk.client } returns client
    every { Clerk.updateClient(any()) } just runs

    coEvery { ClerkApi.signIn.createSignIn(any()) } returns ClerkResult.success(mockSignIn)
    coEvery { mockCredentialManager.getCredential(any(), any()) } throws
      NoCredentialException("No credentials available")

    val result =
      GoogleCredentialAuthenticationService.signInWithGoogleCredential(
        credentialTypes = listOf(SignIn.CredentialType.PASSKEY),
        preferImmediatelyAvailableCredentials = true,
      )

    assertTrue(result is ClerkResult.Failure)
    assertTrue(
      (result as ClerkResult.Failure).throwable is CredentialFlowException.NoSavedCredential
    )
    verify(exactly = 1) { Clerk.updateClient(client.copy(signIn = null)) }
  }

  @Test
  fun `signInWithPasskey handles cancellation without generic failure`() = runTest {
    val nonce = """{"challenge":"test-challenge"}"""

    every { mockSignIn.firstFactorVerification } returns mockVerification
    every { mockVerification.nonce } returns nonce

    coEvery { ClerkApi.signIn.createSignIn(any()) } returns ClerkResult.success(mockSignIn)
    coEvery { mockCredentialManager.getCredential(any(), any()) } throws
      GetCredentialCancellationException()

    val result =
      GoogleCredentialAuthenticationService.signInWithGoogleCredential(
        listOf(SignIn.CredentialType.PASSKEY)
      )

    assertTrue(result is ClerkResult.Failure)
    assertTrue((result as ClerkResult.Failure).throwable is CredentialFlowException.UserCancelled)
  }

  @Test
  fun `automatic credential flow suppresses user cancellation`() {
    val result = ClerkResult.unknownFailure(CredentialFlowException.UserCancelled())

    assertTrue(result.shouldSuppressAutomaticCredentialFlowError)
  }

  @Test
  fun `automatic credential flow suppresses no saved credential`() {
    val result = ClerkResult.unknownFailure(CredentialFlowException.NoSavedCredential())

    assertTrue(result.shouldSuppressAutomaticCredentialFlowError)
  }

  @Test
  fun `automatic credential flow does not suppress provider unavailable`() {
    val result = ClerkResult.unknownFailure(CredentialFlowException.ProviderUnavailable())

    assertFalse(result.shouldSuppressAutomaticCredentialFlowError)
  }

  @Test
  fun `signInWithPasskey fails when no activity is available`() = runTest {
    every { Clerk.credentialActivity() } returns null

    val result =
      GoogleCredentialAuthenticationService.signInWithGoogleCredential(
        listOf(SignIn.CredentialType.PASSKEY)
      )

    assertTrue(result is ClerkResult.Failure)
    assertTrue((result as ClerkResult.Failure).throwable is CredentialFlowException.MissingActivity)
    coVerify(exactly = 0) { mockCredentialManager.getCredential(any(), any()) }
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

  @Test
  fun `verifySessionWithPasskey returns clear error when prepared verification nonce is missing`() =
    runTest {
      val session = testSession()
      val preparedVerification =
        SessionVerification(
          id = "ver_123",
          status = SessionVerification.Status.NEEDS_FIRST_FACTOR,
          level = SessionVerification.Level.FIRST_FACTOR,
        )

      coEvery {
        ClerkApi.session.prepareFirstFactorVerification("sess_123", mapOf("strategy" to "passkey"))
      } returns ClerkResult.success(preparedVerification)

      val result = GoogleCredentialAuthenticationService.verifySessionWithPasskey(session)

      assertTrue(result is ClerkResult.Failure)
      val failure = result as ClerkResult.Failure
      assertEquals(ClerkResult.Failure.ErrorType.UNKNOWN, failure.errorType)
      assertTrue(failure.throwable is IllegalStateException)
      assertEquals("Missing nonce in prepared verification", failure.throwable?.message)
      coVerify(exactly = 0) { mockCredentialManager.getCredential(any(), any()) }
    }

  private fun testSession(): Session {
    return Session(
      id = "sess_123",
      status = Session.SessionStatus.ACTIVE,
      expireAt = 0L,
      lastActiveAt = 0L,
      createdAt = 0L,
      updatedAt = 0L,
    )
  }
}
