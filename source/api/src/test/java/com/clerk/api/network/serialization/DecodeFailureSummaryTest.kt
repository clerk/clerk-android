package com.clerk.api.network.serialization

import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
  fun `summary without a path is the class name alone`() {
    val summary = SerializationException("JSON input: {\"jwt\":\"secret\"}").decodeFailureSummary()

    assertEquals("SerializationException", summary)
  }
}
