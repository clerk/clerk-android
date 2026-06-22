package com.clerk.api.passkeys

import com.clerk.api.Clerk
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class PasskeyWebAuthnRequestTest {

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `createWebAuthnRequest uses nested server relying party id`() {
    val request =
      GoogleCredentialAuthenticationService.createWebAuthnRequest(
        nonce = """{"challenge":"test-challenge","rp":{"id":"passkeys.example.com"}}"""
      )

    assertEquals("passkeys.example.com", request.rpId)
  }

  @Test
  fun `createWebAuthnRequest uses top level server relying party id`() {
    val request =
      GoogleCredentialAuthenticationService.createWebAuthnRequest(
        nonce = """{"challenge":"test-challenge","rpId":"passkeys.example.com"}"""
      )

    assertEquals("passkeys.example.com", request.rpId)
  }

  @Test
  fun `createWebAuthnRequest uses server allowed credentials`() {
    val request =
      GoogleCredentialAuthenticationService.createWebAuthnRequest(
        nonce =
          """
          {
            "challenge": "test-challenge",
            "rpId": "passkeys.example.com",
            "allowCredentials": [
              {
                "type": "public-key",
                "id": "credential-1",
                "transports": ["internal", "hybrid"]
              },
              {
                "type": "public-key",
                "id": "credential-2"
              },
              {
                "type": "public-key"
              },
              {
                "type": "public-key",
                "id": ""
              }
            ]
          }
          """
            .trimIndent()
      )

    assertEquals(
      listOf(
        mapOf("type" to "public-key", "id" to "credential-1"),
        mapOf("type" to "public-key", "id" to "credential-2"),
      ),
      request.allowCredentials,
    )
  }

  @Test
  fun `createWebAuthnRequest prefers explicit allowed credential ids over server list`() {
    val request =
      GoogleCredentialAuthenticationService.createWebAuthnRequest(
        nonce =
          """
          {
            "challenge": "test-challenge",
            "rpId": "passkeys.example.com",
            "allowCredentials": [
              {
                "type": "public-key",
                "id": "server-credential"
              }
            ]
          }
          """
            .trimIndent(),
        allowedCredentialIds = listOf("explicit-credential"),
      )

    assertEquals(
      listOf(mapOf("type" to "public-key", "id" to "explicit-credential")),
      request.allowCredentials,
    )
  }

  @Test
  fun `createWebAuthnRequest falls back to Clerk domain without server relying party id`() {
    mockkObject(Clerk)
    every { Clerk.baseUrl } returns "https://www.fapi.example.com/v1"

    val request =
      GoogleCredentialAuthenticationService.createWebAuthnRequest(
        nonce = """{"challenge":"test-challenge"}"""
      )

    assertEquals("fapi.example.com", request.rpId)
  }
}
