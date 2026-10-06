package com.clerk.api.passkeys

import android.app.Activity
import android.os.Bundle
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.exceptions.CreateCredentialCancellationException
import com.clerk.api.Clerk
import com.clerk.api.credentials.CredentialFlowException
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.UserApi
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
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PasskeyCreationServiceTest {

  private lateinit var mockActivity: Activity
  private lateinit var mockCredentialManager: PasskeyCredentialManager
  private lateinit var mockCreateCredentialResponse: CreateCredentialResponse
  private lateinit var mockBundle: Bundle
  private lateinit var mockUserApi: UserApi
  private lateinit var passkey: Passkey

  @Before
  fun setup() {
    mockActivity = mockk(relaxed = true)
    mockCredentialManager = mockk(relaxed = true)
    mockCreateCredentialResponse = mockk(relaxed = true)
    mockBundle = mockk(relaxed = true)
    mockUserApi = mockk(relaxed = true)
    passkey = passkey(nonce = CREATION_NONCE)

    mockkObject(Clerk)
    every { Clerk.credentialActivity() } returns mockActivity
    every { Clerk.session } returns null

    mockkObject(ClerkApi)
    every { ClerkApi.user } returns mockUserApi

    PasskeyCreationService.setCredentialManager(mockCredentialManager)
  }

  @After
  fun tearDown() {
    PasskeyCreationService.setCredentialManager(PasskeyCredentialManagerImpl())
    unmockkAll()
  }

  @Test
  fun `createPasskey succeeds when all operations are successful`() = runTest {
    val verifiedPasskey = passkey.copy(id = "verified-passkey")
    stubRegistrationResponse(REGISTRATION_JSON)
    coEvery { mockUserApi.createPasskey(any()) } returns ClerkResult.success(passkey)
    coEvery { mockCredentialManager.createCredential(any(), any()) } returns
      mockCreateCredentialResponse
    coEvery {
      mockUserApi.attemptPasskeyVerification(
        passkeyId = any(),
        strategy = any(),
        publicKeyCredential = any(),
        sessionId = any(),
      )
    } returns ClerkResult.success(verifiedPasskey)

    val result = PasskeyCreationService.createPasskey()

    assertTrue(result is ClerkResult.Success)
    assertSame(verifiedPasskey, (result as ClerkResult.Success).value)
    coVerify(exactly = 1) { mockCredentialManager.createCredential(mockActivity, any()) }
    coVerify(exactly = 1) {
      mockUserApi.attemptPasskeyVerification(
        passkeyId = PASSKEY_ID,
        strategy = "passkey",
        publicKeyCredential = any(),
        sessionId = any(),
      )
    }
  }

  @Test
  fun `createPasskey handles initial API failure gracefully`() = runTest {
    val error =
      Error(code = "test_error", message = "Test error", longMessage = "Test error occurred")
    val errorResponse = ClerkErrorResponse(errors = listOf(error), clerkTraceId = "test-trace")

    coEvery { mockUserApi.createPasskey(any()) } returns ClerkResult.apiFailure(errorResponse)

    val result = PasskeyCreationService.createPasskey()

    assertTrue(result is ClerkResult.Failure)
    assertEquals(errorResponse, (result as ClerkResult.Failure).error)
    coVerify(exactly = 0) { mockCredentialManager.createCredential(any(), any()) }
    coVerify(exactly = 0) {
      mockUserApi.attemptPasskeyVerification(any(), any(), any(), any())
    }
  }

  @Test
  fun `createPasskey uses correct request JSON`() = runTest {
    val requestSlot = slot<CreatePublicKeyCredentialRequest>()
    stubRegistrationResponse(REGISTRATION_JSON)
    coEvery { mockUserApi.createPasskey(any()) } returns ClerkResult.success(passkey)
    coEvery { mockCredentialManager.createCredential(mockActivity, capture(requestSlot)) } returns
      mockCreateCredentialResponse
    coEvery { mockUserApi.attemptPasskeyVerification(any(), any(), any(), any()) } returns
      ClerkResult.success(passkey)

    val result = PasskeyCreationService.createPasskey()

    assertTrue(result is ClerkResult.Success)
    assertEquals(CREATION_NONCE, requestSlot.captured.requestJson)
  }

  @Test
  fun `createPasskey submits the registration response with the raw ID value but no pinned raw ID key`() =
    runTest {
      val publicKeyCredentialSlot = slot<String>()
      stubRegistrationResponse(REGISTRATION_JSON)
      coEvery { mockUserApi.createPasskey(any()) } returns ClerkResult.success(passkey)
      coEvery { mockCredentialManager.createCredential(any(), any()) } returns
        mockCreateCredentialResponse
      coEvery {
        mockUserApi.attemptPasskeyVerification(
          passkeyId = any(),
          strategy = any(),
          publicKeyCredential = capture(publicKeyCredentialSlot),
          sessionId = any(),
        )
      } returns ClerkResult.success(passkey)

      val result = PasskeyCreationService.createPasskey()

      assertTrue(result is ClerkResult.Success)
      val submitted = Json.parseToJsonElement(publicKeyCredentialSlot.captured).jsonObject
      assertEquals(4, submitted.size)
      assertEquals("test-credential-id", submitted.getValue("id").jsonPrimitive.content)
      assertTrue(
        submitted.values.any { value ->
          value is JsonPrimitive && value.isString && value.content == "test-raw-credential-id"
        }
      )
      assertEquals("public-key", submitted.getValue("type").jsonPrimitive.content)
      val response = submitted.getValue("response").jsonObject
      assertEquals(setOf("attestationObject", "clientDataJSON"), response.keys)
      assertEquals(
        "test-attestation-object",
        response.getValue("attestationObject").jsonPrimitive.content,
      )
      assertEquals(
        "test-client-data-json",
        response.getValue("clientDataJSON").jsonPrimitive.content,
      )
    }

  @Test
  fun `createPasskey handles cancellation without surfacing unknown failure`() = runTest {
    coEvery { mockUserApi.createPasskey(any()) } returns ClerkResult.success(passkey)
    coEvery { mockCredentialManager.createCredential(any(), any()) } throws
      CreateCredentialCancellationException()

    val result = PasskeyCreationService.createPasskey()

    assertTrue(result is ClerkResult.Failure)
    assertTrue((result as ClerkResult.Failure).throwable is CredentialFlowException.UserCancelled)
    coVerify(exactly = 0) {
      mockUserApi.attemptPasskeyVerification(any(), any(), any(), any())
    }
  }

  @Test
  fun `createPasskey fails when no activity is available`() = runTest {
    every { Clerk.credentialActivity() } returns null

    val result = PasskeyCreationService.createPasskey()

    assertTrue(result is ClerkResult.Failure)
    assertTrue((result as ClerkResult.Failure).throwable is CredentialFlowException.MissingActivity)
    coVerify(exactly = 0) { mockUserApi.createPasskey(any()) }
    coVerify(exactly = 0) { mockCredentialManager.createCredential(any(), any()) }
  }

  @Test
  fun `createPasskey handles verification failure gracefully`() = runTest {
    val error =
      Error(
        code = "verification_failed",
        message = "Verification failed",
        longMessage = "Passkey verification failed",
      )
    val errorResponse = ClerkErrorResponse(errors = listOf(error), clerkTraceId = "test-trace")
    stubRegistrationResponse(REGISTRATION_JSON)
    coEvery { mockUserApi.createPasskey(any()) } returns ClerkResult.success(passkey)
    coEvery { mockCredentialManager.createCredential(any(), any()) } returns
      mockCreateCredentialResponse
    coEvery { mockUserApi.attemptPasskeyVerification(any(), any(), any(), any()) } returns
      ClerkResult.apiFailure(errorResponse)

    val result = PasskeyCreationService.createPasskey()

    assertTrue(result is ClerkResult.Failure)
    assertEquals(errorResponse, (result as ClerkResult.Failure).error)
  }

  @Test
  fun `createPasskey fails without verifying when bundle is missing registration JSON`() = runTest {
    stubRegistrationResponse(null)
    coEvery { mockUserApi.createPasskey(any()) } returns ClerkResult.success(passkey)
    coEvery { mockCredentialManager.createCredential(any(), any()) } returns
      mockCreateCredentialResponse

    val result = PasskeyCreationService.createPasskey()

    assertTrue(result is ClerkResult.Failure)
    val throwable = (result as ClerkResult.Failure).throwable
    assertTrue(throwable is IllegalArgumentException)
    assertEquals("No registration response JSON found in bundle", throwable?.message)
    coVerify(exactly = 0) {
      mockUserApi.attemptPasskeyVerification(any(), any(), any(), any())
    }
  }

  @Test
  fun `createPasskey handles credential manager exception`() = runTest {
    val exception = RuntimeException("Credential creation failed")
    coEvery { mockUserApi.createPasskey(any()) } returns ClerkResult.success(passkey)
    coEvery { mockCredentialManager.createCredential(any(), any()) } throws exception

    val result = PasskeyCreationService.createPasskey()

    assertTrue(result is ClerkResult.Failure)
    val failure = result as ClerkResult.Failure
    assertEquals(ClerkResult.Failure.ErrorType.UNKNOWN, failure.errorType)
    assertSame(exception, failure.throwable)
    coVerify(exactly = 0) {
      mockUserApi.attemptPasskeyVerification(any(), any(), any(), any())
    }
  }

  private fun stubRegistrationResponse(json: String?) {
    every { mockBundle.getString(REGISTRATION_RESPONSE_KEY) } returns json
    every { mockCreateCredentialResponse.data } returns mockBundle
  }

  private fun passkey(nonce: String) =
    Passkey(
      id = PASSKEY_ID,
      name = "Passkey",
      verification = Verification(nonce = nonce),
      createdAt = 0L,
      updatedAt = 0L,
    )

  private companion object {
    const val PASSKEY_ID = "test-passkey-id"
    const val REGISTRATION_RESPONSE_KEY =
      "androidx.credentials.BUNDLE_KEY_REGISTRATION_RESPONSE_JSON"
    const val CREATION_NONCE =
      """{"challenge":"test-challenge","rp":{"id":"example.com","name":"Example"},""" +
        """"user":{"id":"dXNlcg","name":"user@example.com","displayName":"User"}}"""
    val REGISTRATION_JSON =
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
  }
}
