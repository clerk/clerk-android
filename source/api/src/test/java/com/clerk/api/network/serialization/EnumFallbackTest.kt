package com.clerk.api.network.serialization

import com.clerk.api.network.model.environment.DisplayConfig
import com.clerk.api.network.model.environment.InstanceEnvironmentType
import com.clerk.api.network.model.environment.PreferredSignInStrategy
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.session.Session
import com.clerk.api.signin.SignIn
import com.clerk.api.signup.SignUp
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EnumFallbackTest {

  private val json = Json {
    isLenient = true
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = true
  }

  @Test
  fun `DisplayConfig deserializes with unknown instanceEnvironmentType to UNKNOWN`() {
    // Given
    val jsonString = """
      {
        "instance_environment_type": "super_production",
        "application_name": "Test App",
        "preferred_sign_in_strategy": "unknown_strategy",
        "branded": true,
        "logo_image_url": "https://example.com/logo.png",
        "home_url": "https://example.com"
      }
    """.trimIndent()

    // When
    val displayConfig = json.decodeFromString<DisplayConfig>(jsonString)

    // Then
    assertEquals(InstanceEnvironmentType.UNKNOWN, displayConfig.instanceEnvironmentType)
    assertEquals(PreferredSignInStrategy.UNKNOWN, displayConfig.preferredSignInStrategy)
    assertEquals("Test App", displayConfig.applicationName)
  }

  @Test
  fun `DisplayConfig deserializes with missing enum properties to UNKNOWN defaults`() {
    // Given - JSON missing the enum properties
    val jsonString = """
      {
        "application_name": "Test App",
        "branded": true,
        "logo_image_url": "https://example.com/logo.png",
        "home_url": "https://example.com"
      }
    """.trimIndent()

    // When
    val displayConfig = json.decodeFromString<DisplayConfig>(jsonString)

    // Then
    assertEquals(InstanceEnvironmentType.UNKNOWN, displayConfig.instanceEnvironmentType)
    assertEquals(PreferredSignInStrategy.UNKNOWN, displayConfig.preferredSignInStrategy)
    assertEquals("Test App", displayConfig.applicationName)
  }

  @Test
  fun `Verification deserializes with unknown status to UNKNOWN`() {
    // Given
    val jsonString = """
      {
        "status": "super_verified",
        "strategy": "email_code"
      }
    """.trimIndent()

    // When
    val verification = json.decodeFromString<Verification>(jsonString)

    // Then
    assertEquals(Verification.Status.UNKNOWN, verification.status)
    assertEquals("email_code", verification.strategy)
  }

  @Test
  fun `Verification deserializes with missing status to UNKNOWN default`() {
    // Given - JSON missing status
    val jsonString = """
      {
        "strategy": "email_code"
      }
    """.trimIndent()

    // When
    val verification = json.decodeFromString<Verification>(jsonString)

    // Then
    assertEquals(Verification.Status.UNKNOWN, verification.status)
    assertEquals("email_code", verification.strategy)
  }

  @Test
  fun `SignUp deserializes with unknown status to UNKNOWN`() {
    // Given
    val jsonString = """
      {
        "id": "signup_123",
        "status": "super_complete",
        "required_fields": [],
        "optional_fields": [],
        "missing_fields": [],
        "unverified_fields": [],
        "verifications": {},
        "password_enabled": false
      }
    """.trimIndent()

    // When
    val signUp = json.decodeFromString<SignUp>(jsonString)

    // Then
    assertEquals(SignUp.Status.UNKNOWN, signUp.status)
    assertEquals("signup_123", signUp.id)
  }

  @Test
  fun `SignIn deserializes with unknown status to UNKNOWN`() {
    // Given
    val jsonString = """
      {
        "id": "signin_123",
        "status": "super_complete"
      }
    """.trimIndent()

    // When
    val signIn = json.decodeFromString<SignIn>(jsonString)

    // Then
    assertEquals(SignIn.Status.UNKNOWN, signIn.status)
    assertEquals("signin_123", signIn.id)
  }

  @Test
  fun `Session deserializes with unknown status to UNKNOWN`() {
    // Given
    val jsonString = """
      {
        "id": "session_123",
        "status": "super_active",
        "expire_at": 1234567890,
        "abandon_at": 1234567890,
        "last_active_at": 1234567890,
        "created_at": 1234567890,
        "updated_at": 1234567890
      }
    """.trimIndent()

    // When
    val session = json.decodeFromString<Session>(jsonString)

    // Then
    assertEquals(Session.SessionStatus.UNKNOWN, session.status)
    assertEquals("session_123", session.id)
  }

  @Test
  fun `known enum values still deserialize correctly`() {
    // Given
    val jsonString = """
      {
        "instance_environment_type": "production",
        "application_name": "Test App",
        "preferred_sign_in_strategy": "password",
        "branded": true,
        "logo_image_url": "https://example.com/logo.png",
        "home_url": "https://example.com"
      }
    """.trimIndent()

    // When
    val displayConfig = json.decodeFromString<DisplayConfig>(jsonString)

    // Then
    assertEquals(InstanceEnvironmentType.PRODUCTION, displayConfig.instanceEnvironmentType)
    assertEquals(PreferredSignInStrategy.PASSWORD, displayConfig.preferredSignInStrategy)
    assertEquals("Test App", displayConfig.applicationName)
  }
}