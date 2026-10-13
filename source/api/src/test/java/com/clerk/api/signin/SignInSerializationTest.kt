package com.clerk.api.signin

import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.client.Client
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SignInSerializationTest {

  @Test
  fun `serializer preserves the public descriptor name`() {
    assertEquals("com.clerk.api.signin.SignIn", SignIn.serializer().descriptor.serialName)
  }

  @Test
  fun `sign in decodes known status values`() {
    val signIn =
      ClerkApi.json.decodeFromString<SignIn>("""{"id":"sia_123","status":"needs_client_trust"}""")

    assertEquals(SignIn.Status.NEEDS_CLIENT_TRUST, signIn.status)
    assertEquals("needs_client_trust", signIn.statusRawValue)
  }

  @Test
  fun `sign in decodes protect status and preserves the opaque challenge`() {
    val signIn =
      ClerkApi.json.decodeFromString<SignIn>(
        """
        {
          "id": "sia_123",
          "status": "needs_protect_check",
          "protect_check": {
            "status": "pending",
            "token": "challenge-token",
            "sdk_url": "https://example.com/protect.js",
            "future_field": {"nested": true}
          }
        }
        """
          .trimIndent()
      )

    assertEquals(SignIn.Status.NEEDS_PROTECT_CHECK, signIn.status)
    assertEquals("needs_protect_check", signIn.statusRawValue)

    val copied = signIn.copy(identifier = "user@example.com")
    val encoded = ClerkApi.json.encodeToString(SignIn.serializer(), copied)
    val decoded = ClerkApi.json.decodeFromString<SignIn>(encoded)

    assertEquals(signIn.protectCheck, decoded.protectCheck)
    assertEquals("user@example.com", decoded.identifier)
  }

  @Test
  fun `sign in preserves unknown status values`() {
    val json =
      """
      {
        "id": "sia_123",
        "status": "future_sign_in_status",
        "supported_identifiers": ["email_address"],
        "identifier": "user@example.com",
        "created_session_id": null
      }
      """
        .trimIndent()

    val signIn = ClerkApi.json.decodeFromString<SignIn>(json)

    assertEquals(SignIn.Status.UNKNOWN, signIn.status)
    assertEquals("future_sign_in_status", signIn.statusRawValue)
    assertEquals(listOf("email_address"), signIn.supportedIdentifiers)
    assertEquals("user@example.com", signIn.identifier)

    val copied = signIn.copy(identifier = "updated@example.com")
    val encodedCopy = ClerkApi.json.encodeToString(SignIn.serializer(), copied)
    val decodedCopy = ClerkApi.json.decodeFromString<SignIn>(encodedCopy)

    assertEquals("updated@example.com", decodedCopy.identifier)
    assertEquals("future_sign_in_status", decodedCopy.statusRawValue)

    val otherStatus =
      ClerkApi.json.decodeFromString<SignIn>(
        json.replace("future_sign_in_status", "another_future_status")
      )
    assertNotEquals(signIn, otherStatus)
  }

  @Test
  fun `client preserves an unknown nested sign in status`() {
    val client =
      ClerkApi.json.decodeFromString<Client>(
        """
        {
          "id": "client_123",
          "sign_in": {
            "id": "sia_123",
            "status": "future_sign_in_status"
          },
          "sessions": []
        }
        """
          .trimIndent()
      )

    assertEquals(SignIn.Status.UNKNOWN, client.signIn?.status)
    assertEquals("future_sign_in_status", client.signIn?.statusRawValue)
  }

  @Test
  fun `copying with a different status uses its serialized value`() {
    val signIn =
      ClerkApi.json.decodeFromString<SignIn>(
        """{"id":"sia_123","status":"future_sign_in_status"}"""
      )

    val completed = signIn.copy(status = SignIn.Status.COMPLETE)

    assertEquals(SignIn.Status.COMPLETE, completed.status)
    assertEquals("complete", completed.statusRawValue)
  }
}
