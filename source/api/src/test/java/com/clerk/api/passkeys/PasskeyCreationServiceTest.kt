package com.clerk.api.passkeys

import android.content.Context
import android.os.Bundle
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.CreatePublicKeyCredentialRequest
import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.network.serialization.ClerkResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
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

@Ignore
@RunWith(RobolectricTestRunner::class)
class PasskeyCreationServiceTest {

  private lateinit var mockContext: Context
  private lateinit var mockCredentialManager: PasskeyCredentialManager
  private lateinit var mockCreateCredentialResponse: CreateCredentialResponse
  private lateinit var mockBundle: Bundle
  private lateinit var mockPasskey: Passkey
  private lateinit var mockVerification: Verification

  @Before
  fun setup() {
    mockContext = mockk(relaxed = true)
    mockCredentialManager = mockk(relaxed = true)
    mockCreateCredentialResponse = mockk(relaxed = true)
    mockBundle = mockk(relaxed = true)
    mockPasskey = mockk(relaxed = true)
    mockVerification = mockk(relaxed = true)

    // Mock Clerk application context
    mockkObject(Clerk)
    every { Clerk.applicationContext } returns WeakReference(mockContext)

    // Mock ClerkApi and its nested objects
    mockkObject(ClerkApi)
    every { ClerkApi.user } returns mockk(relaxed = true)
    every { ClerkApi.json } returns mockk(relaxed = true)

    // Set up the mock credential manager in the service
    PasskeyCreationService.setCredentialManager(mockCredentialManager)
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `createPasskey succeeds when all operations are successful`() = runTest {
    // Given
    val nonce = """{"challenge":"test-challenge","other":"data"}"""
    val passkeyId = "test-passkey-id"
    val bundleJson =
      """
      {
        "id": "credential-id",
        "rawId": "raw-credential-id", 
        "type": "public-key",
        "response": {
          "attestationObject": "test-attestation",
          "clientDataJSON": "test-client-data"
        }
      }
    """
        .trimIndent()

    every { mockVerification.nonce } returns nonce
    every { mockPasskey.id } returns passkeyId
    every { mockPasskey.verification } returns mockVerification
    every {
      mockBundle.getString("androidx.credentials.BUNDLE_KEY_REGISTRATION_RESPONSE_JSON")
    } returns bundleJson
    every { mockCreateCredentialResponse.data } returns mockBundle

    coEvery { ClerkApi.user.createPasskey() } returns ClerkResult.success(mockPasskey)
    coEvery { mockCredentialManager.createCredential(any(), any()) } returns
      mockCreateCredentialResponse
    coEvery {
      ClerkApi.user.attemptPasskeyVerification(passkeyId = any(), publicKeyCredential = any())
    } returns ClerkResult.success(mockPasskey)

    // When
    val result = PasskeyCreationService.createPasskey()

    // Then
    assertTrue(result is ClerkResult.Success)
    coVerify { ClerkApi.user.createPasskey() }
    coVerify { mockCredentialManager.createCredential(eq(mockContext), any()) }
    coVerify {
      ClerkApi.user.attemptPasskeyVerification(passkeyId = passkeyId, publicKeyCredential = any())
    }
  }

  @Test
  fun `createPasskey handles initial API failure gracefully`() = runTest {
    // Given
    val error =
      Error(code = "test_error", message = "Test error", longMessage = "Test error occurred")
    val errorResponse = ClerkErrorResponse(errors = listOf(error), clerkTraceId = "test-trace")

    coEvery { ClerkApi.user.createPasskey() } returns ClerkResult.apiFailure(errorResponse)

    // When
    val result = PasskeyCreationService.createPasskey()

    // Then
    assertTrue(result is ClerkResult.Failure)
    assertEquals(errorResponse, (result as ClerkResult.Failure).error)
    coVerify { ClerkApi.user.createPasskey() }
    coVerify(exactly = 0) { mockCredentialManager.createCredential(any(), any()) }
    coVerify(exactly = 0) {
      ClerkApi.user.attemptPasskeyVerification(passkeyId = any(), publicKeyCredential = any())
    }
  }

  @Test
  fun `createPasskey uses correct request JSON`() = runTest {
    // Given
    val nonce = """{"challenge":"test-challenge","rp":{"id":"example.com"}}"""
    val passkeyId = "test-passkey-id"
    val requestSlot = slot<CreatePublicKeyCredentialRequest>()

    every { mockVerification.nonce } returns nonce
    every { mockPasskey.id } returns passkeyId
    every { mockPasskey.verification } returns mockVerification
    every {
      mockBundle.getString("androidx.credentials.BUNDLE_KEY_REGISTRATION_RESPONSE_JSON")
    } returns
      """
      {
        "id": "test-id",
        "rawId": "test-raw-id",
        "type": "public-key",
        "response": {
          "attestationObject": "test-attestation-object",
          "clientDataJSON": "test-client-data-json",
          "extraField": "should-be-ignored"
        }
      }
    """
        .trimIndent()
    every { mockCreateCredentialResponse.data } returns mockBundle

    coEvery { ClerkApi.user.createPasskey() } returns ClerkResult.success(mockPasskey)
    coEvery {
      mockCredentialManager.createCredential(eq(mockContext), capture(requestSlot))
    } returns mockCreateCredentialResponse
    coEvery {
      ClerkApi.user.attemptPasskeyVerification(passkeyId = any(), publicKeyCredential = any())
    } returns ClerkResult.success(mockPasskey)

    // When
    val result = PasskeyCreationService.createPasskey()

    // Then
    assertTrue(result is ClerkResult.Success)
    assertEquals(nonce, requestSlot.captured.requestJson)
    coVerify { ClerkApi.user.createPasskey() }
    coVerify { mockCredentialManager.createCredential(eq(mockContext), any()) }
    coVerify {
      ClerkApi.user.attemptPasskeyVerification(passkeyId = passkeyId, publicKeyCredential = any())
    }
  }

  @Test
  fun `parsePasskeyDataDirectFromBundle processes bundle correctly`() = runTest {
    // Given
    val bundleJson =
      """
      {
        "id": "test-credential-id",
        "rawId": "test-raw-credential-id",
        "type": "public-key",
        "response": {
          "attestationObject": "test-attestation-object",
          "clientDataJSON": "test-client-data-json",
          "extraField": "should-be-ignored"
        }
      }
    """
        .trimIndent()

    val publicKeyCredentialSlot = slot<String>()

    every { mockVerification.nonce } returns """{"challenge":"test"}"""
    every { mockPasskey.id } returns "test-passkey-id"
    every { mockPasskey.verification } returns mockVerification
    every {
      mockBundle.getString("androidx.credentials.BUNDLE_KEY_REGISTRATION_RESPONSE_JSON")
    } returns bundleJson
    every { mockCreateCredentialResponse.data } returns mockBundle

    coEvery { ClerkApi.user.createPasskey() } returns ClerkResult.success(mockPasskey)
    coEvery { mockCredentialManager.createCredential(any(), any()) } returns
      mockCreateCredentialResponse
    coEvery {
      ClerkApi.user.attemptPasskeyVerification(
        passkeyId = any(),
        publicKeyCredential = capture(publicKeyCredentialSlot),
      )
    } returns ClerkResult.success(mockPasskey)

    // When
    val result = PasskeyCreationService.createPasskey()

    // Then
    assertTrue(result is ClerkResult.Success)
    val capturedJson = publicKeyCredentialSlot.captured
    assertTrue("Should contain credential ID", capturedJson.contains("test-credential-id"))
    assertTrue("Should contain raw ID", capturedJson.contains("test-raw-credential-id"))
    assertTrue(
      "Should contain attestation object",
      capturedJson.contains("test-attestation-object"),
    )
    assertTrue("Should contain client data JSON", capturedJson.contains("test-client-data-json"))
  }

  @Test
  fun `createPasskey handles verification failure gracefully`() = runTest {
    // Given
    val nonce = """{"challenge":"test-challenge"}"""
    val passkeyId = "test-passkey-id"
    val error =
      Error(
        code = "verification_failed",
        message = "Verification failed",
        longMessage = "Passkey verification failed",
      )
    val errorResponse = ClerkErrorResponse(errors = listOf(error), clerkTraceId = "test-trace")

    every { mockVerification.nonce } returns nonce
    every { mockPasskey.id } returns passkeyId
    every { mockPasskey.verification } returns mockVerification
    every {
      mockBundle.getString("androidx.credentials.BUNDLE_KEY_REGISTRATION_RESPONSE_JSON")
    } returns
      """
      {
        "id": "test-id",
        "rawId": "test-raw-id", 
        "type": "public-key",
        "response": {
          "attestationObject": "test-attestation",
          "clientDataJSON": "test-client-data"
        }
      }
    """
        .trimIndent()
    every { mockCreateCredentialResponse.data } returns mockBundle

    coEvery { ClerkApi.user.createPasskey() } returns ClerkResult.success(mockPasskey)
    coEvery { mockCredentialManager.createCredential(any(), any()) } returns
      mockCreateCredentialResponse
    coEvery {
      ClerkApi.user.attemptPasskeyVerification(passkeyId = any(), publicKeyCredential = any())
    } returns ClerkResult.apiFailure(errorResponse)

    // When
    val result = PasskeyCreationService.createPasskey()

    // Then
    assertTrue(result is ClerkResult.Failure)
    assertEquals(errorResponse, (result as ClerkResult.Failure).error)
    coVerify { ClerkApi.user.createPasskey() }
    coVerify { mockCredentialManager.createCredential(any(), any()) }
    coVerify {
      ClerkApi.user.attemptPasskeyVerification(passkeyId = any(), publicKeyCredential = any())
    }
  }

  @Test
  fun `parsePasskeyDataDirectFromBundle throws when bundle is missing JSON`() = runTest {
    // Given
    every { mockVerification.nonce } returns """{"challenge":"test"}"""
    every { mockPasskey.id } returns "test-passkey-id"
    every { mockPasskey.verification } returns mockVerification
    every {
      mockBundle.getString("androidx.credentials.BUNDLE_KEY_REGISTRATION_RESPONSE_JSON")
    } returns null
    every { mockCreateCredentialResponse.data } returns mockBundle

    coEvery { ClerkApi.user.createPasskey() } returns ClerkResult.success(mockPasskey)
    coEvery { mockCredentialManager.createCredential(any(), any()) } returns
      mockCreateCredentialResponse

    var exceptionThrown = false

    // When
    try {
      PasskeyCreationService.createPasskey()
    } catch (e: IllegalArgumentException) {
      exceptionThrown = true
      assertEquals("No registration response JSON found in bundle", e.message)
    }

    // Then
    assertTrue("Should have thrown IllegalArgumentException", exceptionThrown)
  }

  @Test
  fun `createPasskey handles credential manager exception`() = runTest {
    // Given
    val nonce = """{"challenge":"test-challenge"}"""
    val passkeyId = "test-passkey-id"
    val exception = RuntimeException("Credential creation failed")

    every { mockVerification.nonce } returns nonce
    every { mockPasskey.id } returns passkeyId
    every { mockPasskey.verification } returns mockVerification

    coEvery { ClerkApi.user.createPasskey() } returns ClerkResult.success(mockPasskey)
    coEvery { mockCredentialManager.createCredential(any(), any()) } throws exception

    // When
    val result = PasskeyCreationService.createPasskey()

    // Then
    assertTrue(result is ClerkResult.Failure)
    assertEquals(ClerkResult.Failure.ErrorType.UNKNOWN, (result as ClerkResult.Failure).errorType)
    coVerify { ClerkApi.user.createPasskey() }
    coVerify { mockCredentialManager.createCredential(any(), any()) }
    coVerify(exactly = 0) {
      ClerkApi.user.attemptPasskeyVerification(passkeyId = any(), publicKeyCredential = any())
    }
  }
}
