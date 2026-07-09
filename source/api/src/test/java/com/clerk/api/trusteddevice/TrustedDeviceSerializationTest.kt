package com.clerk.api.trusteddevice

import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.environment.AuthConfig
import com.clerk.api.network.model.verification.Verification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrustedDeviceSerializationTest {

  @Test
  fun `trusted device decodes from snake_case json`() {
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

    val trustedDevice = ClerkApi.json.decodeFromString<TrustedDevice>(json)

    assertEquals("td_123", trustedDevice.id)
    assertEquals(TrustedDevice.Platform.ANDROID, trustedDevice.platform)
    assertEquals("com.example.app", trustedDevice.appIdentifier)
    assertEquals("Pixel 9", trustedDevice.name)
    assertEquals("ES256", trustedDevice.algorithm)
    assertEquals(TrustedDevice.Status.ACTIVE, trustedDevice.status)
    assertEquals(1720000000000, trustedDevice.createdAt)
    assertEquals(1720000001000, trustedDevice.updatedAt)
    assertEquals(1720000002000L, trustedDevice.lastUsedAt)
    assertNull(trustedDevice.revokedAt)
  }

  @Test
  fun `trusted device coerces unknown platform and status`() {
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

    val trustedDevice = ClerkApi.json.decodeFromString<TrustedDevice>(json)

    assertEquals(TrustedDevice.Platform.UNKNOWN, trustedDevice.platform)
    assertEquals(TrustedDevice.Status.UNKNOWN, trustedDevice.status)
  }

  @Test
  fun `trusted device challenge decodes from snake_case json`() {
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

    val challenge = ClerkApi.json.decodeFromString<TrustedDeviceChallenge>(json)

    assertEquals("challenge-value", challenge.challenge)
    assertEquals("tdc_123", challenge.challengeId)
    assertEquals("td_123", challenge.trustedDeviceId)
    assertEquals("client-data-to-sign", challenge.clientData)
    assertEquals(1720000005000, challenge.expiresAt)
    assertEquals("ES256", challenge.algorithm)
  }

  @Test
  fun `verification decodes trusted device challenge`() {
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
    assertNotNull(verification.trustedDeviceChallenge)
    assertEquals("client-data-to-sign", verification.trustedDeviceChallenge?.clientData)
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
    assertTrue(authConfig.nativeSettings.trustedDeviceSignInEnabled)
    assertTrue(authConfig.nativeSettings.trustedDevicePromptAfterSignInEnabled)
    assertFalse(authConfig.nativeSettings.trustedDevicePromptAfterSignUpEnabled)
  }

  @Test
  fun `auth config defaults native settings when absent`() {
    val json = """{"single_session_mode": false}"""

    val authConfig = ClerkApi.json.decodeFromString<AuthConfig>(json)

    assertFalse(authConfig.nativeSettings.apiEnabled)
    assertFalse(authConfig.nativeSettings.trustedDeviceSignInEnabled)
    assertFalse(authConfig.nativeSettings.trustedDevicePromptAfterSignInEnabled)
    assertFalse(authConfig.nativeSettings.trustedDevicePromptAfterSignUpEnabled)
  }
}
