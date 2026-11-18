package com.clerk.api.network.serialization

import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.client.Client
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class DecodeFailureSummaryTest {

  @Test
  fun `summary keeps the class and path and drops the JSON input excerpt`() {
    val exception =
      SerializationException(
        "Unexpected JSON token at offset 42: Expected numeric literal at path: " +
          "\$.client.sessions[0].expire_at\n" +
          "JSON input: {\"last_active_token\":{\"jwt\":\"eyJhbGciOiJSUzI1NiJ9.secret.sig\"}}"
      )

    val summary = exception.decodeFailureSummary()

    assertEquals("SerializationException at path: \$.client.sessions[0].expire_at", summary)
    assertFalse(summary.contains("jwt"))
    assertFalse(summary.contains("secret"))
  }

  @Test
  fun `summary takes the locator kotlinx adds last, not marker text inside a quoted literal`() {
    val exception =
      SerializationException(
        "Failed to parse literal 'at path: eyJhbGciOiJSUzI1NiJ9.secret.sig' as a long value " +
          "at element: \$.client.sessions[0].expire_at"
      )

    val summary = exception.decodeFailureSummary()

    assertEquals("SerializationException at path: \$.client.sessions[0].expire_at", summary)
    assertFalse(summary.contains("eyJ"))
  }

  @Test
  fun `summary ignores marker text inside the JSON input excerpt`() {
    val exception =
      SerializationException(
        "Unexpected JSON token at offset 42 at path: \$.client\n" +
          "JSON input: {\"note\":\"at path: eyJhbGciOiJSUzI1NiJ9.secret.sig\"}"
      )

    val summary = exception.decodeFailureSummary()

    assertEquals("SerializationException at path: \$.client", summary)
  }

  @Test
  fun `a real decode failure on a long field keeps marker text out of the summary`() {
    val token = "eyJhbGciOiJSUzI1NiJ9.c2Vzc2lvbl9zZWNyZXQ.signature"
    val failure =
      runCatching {
          ClerkApi.json.decodeFromString<Client>(
            """
            {
              "id": "client_123",
              "sessions": [{
                "id": "sess_123",
                "status": "active",
                "expire_at": "at path: $token",
                "last_active_at": 1000,
                "created_at": 1000,
                "updated_at": 1000
              }]
            }
            """
              .trimIndent()
          )
        }
        .exceptionOrNull()

    assertNotNull(failure)
    val summary = failure!!.decodeFailureSummary()
    assertFalse(summary.contains(token))
    assertFalse(summary.contains("eyJ"))
  }

  @Test
  fun `summary without a path is the class name alone`() {
    val summary = SerializationException("JSON input: {\"jwt\":\"secret\"}").decodeFailureSummary()

    assertEquals("SerializationException", summary)
  }
}
