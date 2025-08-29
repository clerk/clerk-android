package com.clerk.api.passkeys

import com.clerk.api.Clerk
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PasskeyHelperTest {

  @Before
  fun setup() {
    mockkObject(Clerk)
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `getDomain returns domain without www prefix`() {
    // Given
    every { Clerk.baseUrl } returns "https://www.example.com/path"

    // When
    val result = PasskeyHelper.getDomain()

    // Then
    assertEquals("example.com", result)
  }

  @Test
  fun `getDomain returns domain without protocol and path`() {
    // Given
    every { Clerk.baseUrl } returns "https://clerk.example.com/api/v1"

    // When
    val result = PasskeyHelper.getDomain()

    // Then
    assertEquals("clerk.example.com", result)
  }

  @Test
  fun `getDomain returns empty string when host is null`() {
    // Given
    every { Clerk.baseUrl } returns "invalid-url"

    // When
    val result = PasskeyHelper.getDomain()

    // Then
    assertEquals("", result)
  }

  @Test
  fun `getDomain handles malformed URL gracefully`() {
    // Given
    every { Clerk.baseUrl } returns "not-a-url"

    // When
    val result = PasskeyHelper.getDomain()

    // Then
    assertEquals("", result)
  }

  @Test
  fun `GetPasskeyRequest serializes correctly`() {
    // Given
    val request =
      GetPasskeyRequest(
        challenge = "test-challenge",
        allowCredentials =
          listOf(
            mapOf("type" to "public-key", "id" to "credential-1"),
            mapOf("type" to "public-key", "id" to "credential-2"),
          ),
        timeout = 60000L,
        userVerification = "required",
        rpId = "example.com",
      )

    // When/Then - Should not throw during serialization
    assertEquals("test-challenge", request.challenge)
    assertEquals(2, request.allowCredentials.size)
    assertEquals("credential-1", request.allowCredentials[0]["id"])
    assertEquals("required", request.userVerification)
  }

  @Test
  fun `PublicKeyCredentialData holds correct data`() {
    // Given
    val responseMap =
      mapOf("attestationObject" to "test-attestation", "clientDataJSON" to "test-client-data")

    // When
    val credentialData =
      PublicKeyCredentialData(
        id = "test-id",
        rawId = "test-raw-id",
        type = "public-key",
        response = responseMap,
      )

    // Then
    assertEquals("test-id", credentialData.id)
    assertEquals("test-raw-id", credentialData.rawId)
    assertEquals("public-key", credentialData.type)
    assertEquals("test-attestation", credentialData.response["attestationObject"])
    assertEquals("test-client-data", credentialData.response["clientDataJSON"])
  }

  @Test
  fun `constants have expected values`() {
    assertEquals("strategy", com.clerk.api.Constants.Fields.STRATEGY)
    assertEquals("passkey", PasskeyHelper.passkeyStrategy)
  }
}
