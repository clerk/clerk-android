package com.clerk.api.biometriccredential

import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.environment.AuthConfig
import com.clerk.api.network.model.verification.Verification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BiometricCredentialSerializationTest {

  @Test
  fun `serializer preserves the public descriptor name`() {
    assertEquals(
      "com.clerk.api.biometriccredential.BiometricCredential",
      BiometricCredential.serializer().descriptor.serialName,
    )
  }

  @Test
  fun `biometric credential decodes from snake_case json`() {
    val json =
      """
      {
        "id": "td_123",
        "object": "trusted_device",
        "platform": "android",
        "app_identifier": "com.example.app",
        "name": "Pixel 9",
        "algorithm": "ES256",
        "status": "active",
        "created_at": 1720000000000,
        "updated_at": 1720000001000,
        "last_used_at": 1720000002000,
        "revoked_at": null
      }
      """
        .trimIndent()

    val biometricCredential = ClerkApi.json.decodeFromString<BiometricCredential>(json)

    assertEquals("td_123", biometricCredential.id)
    assertEquals(BiometricCredential.Platform.ANDROID, biometricCredential.platform)
    assertEquals("android", biometricCredential.platformRawValue)
    assertEquals("com.example.app", biometricCredential.appIdentifier)
    assertEquals("Pixel 9", biometricCredential.name)
    assertEquals("ES256", biometricCredential.algorithm)
    assertEquals(BiometricCredential.Status.ACTIVE, biometricCredential.status)
    assertEquals("active", biometricCredential.statusRawValue)
    assertEquals(1720000000000, biometricCredential.createdAt)
    assertEquals(1720000001000, biometricCredential.updatedAt)
    assertEquals(1720000002000L, biometricCredential.lastUsedAt)
    assertNull(biometricCredential.revokedAt)
  }

  @Test
  fun `biometric credential preserves unknown platform and status values`() {
    val json =
      """
      {
        "id": "td_123",
        "platform": "vision_pro",
        "app_identifier": "com.example.app",
        "algorithm": "ES256",
        "status": "paused",
        "created_at": 1,
        "updated_at": 2
      }
      """
        .trimIndent()

    val biometricCredential = ClerkApi.json.decodeFromString<BiometricCredential>(json)

    assertEquals(BiometricCredential.Platform.UNKNOWN, biometricCredential.platform)
    assertEquals(BiometricCredential.Status.UNKNOWN, biometricCredential.status)
    assertEquals("vision_pro", biometricCredential.platformRawValue)
    assertEquals("paused", biometricCredential.statusRawValue)

    val copied = biometricCredential.copy(name = "Updated device")
    val encodedCopy = ClerkApi.json.encodeToString(BiometricCredential.serializer(), copied)
    val decodedCopy = ClerkApi.json.decodeFromString<BiometricCredential>(encodedCopy)

    assertEquals("Updated device", decodedCopy.name)
    assertEquals("vision_pro", decodedCopy.platformRawValue)
    assertEquals("paused", decodedCopy.statusRawValue)

    val otherPlatform =
      ClerkApi.json.decodeFromString<BiometricCredential>(json.replace("vision_pro", "visionos"))
    val otherStatus =
      ClerkApi.json.decodeFromString<BiometricCredential>(json.replace("paused", "suspended"))
    assertNotEquals(biometricCredential, otherPlatform)
    assertNotEquals(biometricCredential, otherStatus)
  }

  @Test
  fun `biometric credential challenge decodes from snake_case json`() {
    val json =
      """
      {
        "object": "trusted_device_challenge",
        "challenge": "challenge-value",
        "challenge_id": "tdc_123",
        "trusted_device_id": "td_123",
        "client_data": "client-data-to-sign",
        "expires_at": 1720000005000,
        "algorithm": "ES256"
      }
      """
        .trimIndent()

    val challenge = ClerkApi.json.decodeFromString<BiometricCredentialChallenge>(json)

    assertEquals("challenge-value", challenge.challenge)
    assertEquals("tdc_123", challenge.challengeId)
    assertEquals("td_123", challenge.biometricCredentialId)
    assertEquals("client-data-to-sign", challenge.clientData)
    assertEquals(1720000005000, challenge.expiresAt)
    assertEquals("ES256", challenge.algorithm)
  }

  @Test
  fun `verification decodes biometric credential challenge`() {
    val json =
      """
      {
        "status": "unverified",
        "strategy": "trusted_device",
        "trusted_device_challenge": {
          "object": "trusted_device_challenge",
          "challenge": "challenge-value",
          "challenge_id": "tdc_123",
          "client_data": "client-data-to-sign",
          "expires_at": 1720000005000,
          "algorithm": "ES256"
        }
      }
      """
        .trimIndent()

    val verification = ClerkApi.json.decodeFromString<Verification>(json)

    assertEquals("trusted_device", verification.strategy)
    assertNotNull(verification.biometricCredentialChallenge)
    assertEquals("client-data-to-sign", verification.biometricCredentialChallenge?.clientData)
  }

  @Test
  fun `auth config decodes native settings`() {
    val json =
      """
      {
        "single_session_mode": true,
        "native_settings": {
          "api_enabled": true,
          "trusted_device_sign_in_enabled": true,
          "trusted_device_enrollment_prompt_after_sign_in_enabled": true,
          "trusted_device_enrollment_prompt_after_sign_up_enabled": false
        }
      }
      """
        .trimIndent()

    val authConfig = ClerkApi.json.decodeFromString<AuthConfig>(json)

    assertTrue(authConfig.nativeSettings.apiEnabled)
    assertTrue(authConfig.nativeSettings.biometricSignInEnabled)
    assertTrue(authConfig.nativeSettings.biometricCredentialPromptAfterSignInEnabled)
    assertFalse(authConfig.nativeSettings.biometricCredentialPromptAfterSignUpEnabled)
  }

  @Test
  fun `auth config defaults native settings when absent`() {
    val json = """{"single_session_mode": false}"""

    val authConfig = ClerkApi.json.decodeFromString<AuthConfig>(json)

    assertFalse(authConfig.nativeSettings.apiEnabled)
    assertFalse(authConfig.nativeSettings.biometricSignInEnabled)
    assertFalse(authConfig.nativeSettings.biometricCredentialPromptAfterSignInEnabled)
    assertFalse(authConfig.nativeSettings.biometricCredentialPromptAfterSignUpEnabled)
  }
}
