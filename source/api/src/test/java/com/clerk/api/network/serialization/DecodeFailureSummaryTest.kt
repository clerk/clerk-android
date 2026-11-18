package com.clerk.api.network.serialization

import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.client.Client
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DecodeFailureSummaryTest {

  private val token = "eyJhbGciOiJSUzI1NiJ9.c2Vzc2lvbl9zZWNyZXQ.signature"
  private val locatorImitation = "at path: \$$token JSON input:"

  private fun clientJsonWithExpireAt(expireAt: String): String =
    """
    {
      "id": "client_123",
      "sessions": [{
        "id": "sess_123",
        "status": "active",
        "expire_at": "$expireAt",
        "last_active_at": 1000,
        "created_at": 1000,
        "updated_at": 1000,
        "last_active_token": { "jwt": "$token" }
      }]
    }
    """
      .trimIndent()

  private fun failureOf(decode: () -> Client): Throwable =
    runCatching(decode).exceptionOrNull() ?: error("The client decoded")

  private fun assertSummaryIsTheClassAlone(failure: Throwable) {
    val summary = failure.decodeFailureSummary()

    assertEquals(failure::class.simpleName, summary)
    assertFalse(summary.contains(token))
    assertFalse(summary.contains("eyJ"))
    assertFalse(summary.contains("$"))
  }

  @Test
  fun `summary is the exception class and drops the message`() {
    val exception =
      SerializationException(
        "Unexpected JSON token at offset 42: Expected numeric literal at path: " +
          "\$.client.sessions[0].expire_at\n" +
          "JSON input: {\"last_active_token\":{\"jwt\":\"$token\"}}"
      )

    assertEquals("SerializationException", exception.decodeFailureSummary())
  }

  @Test
  fun `a string decode failure on a field that imitates the locator leaks nothing`() {
    val failure =
      failureOf { ClerkApi.json.decodeFromString(clientJsonWithExpireAt(locatorImitation)) }

    assertSummaryIsTheClassAlone(failure)
  }

  @Test
  fun `a tree decode failure on a field that imitates the locator leaks nothing`() {
    val element = Json.parseToJsonElement(clientJsonWithExpireAt(locatorImitation))

    val failure = failureOf { ClerkApi.json.decodeFromJsonElement(element) }

    assertSummaryIsTheClassAlone(failure)
  }

  @Test
  fun `summary of an exception without a message is the class name`() {
    assertEquals("SerializationException", SerializationException().decodeFailureSummary())
  }
}
