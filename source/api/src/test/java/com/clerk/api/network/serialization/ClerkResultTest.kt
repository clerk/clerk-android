package com.clerk.api.network.serialization

import com.clerk.api.configuration.connectivity.NetworkConnectivityMonitor
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import java.io.IOException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class ClerkResultTest {

  @Before
  fun setup() {
    mockkObject(NetworkConnectivityMonitor)
    every { NetworkConnectivityMonitor.isCurrentlyConnected() } returns true
  }

  @After
  fun tearDown() {
    unmockkObject(NetworkConnectivityMonitor)
  }

  @Test
  fun `errorMessage describes IO failures while offline`() {
    every { NetworkConnectivityMonitor.isCurrentlyConnected() } returns false
    val result: ClerkResult.Failure<ClerkErrorResponse> =
      ClerkResult.unknownFailure(IOException("Unable to resolve host api.clerk.com"))

    assertEquals(
      "No internet connection detected. Please check your network connection and try again.",
      result.errorMessage,
    )
  }

  @Test
  fun `errorMessage keeps generic fallback for IO failures while connected`() {
    val result: ClerkResult.Failure<ClerkErrorResponse> =
      ClerkResult.unknownFailure(IOException("Connection reset"))

    assertEquals("Error occurred with unknown message.", result.errorMessage)
  }

  @Test
  fun `errorMessage keeps generic fallback for non-IO failures while offline`() {
    every { NetworkConnectivityMonitor.isCurrentlyConnected() } returns false
    val result: ClerkResult.Failure<ClerkErrorResponse> =
      ClerkResult.unknownFailure(IllegalStateException("Unexpected failure"))

    assertEquals("Error occurred with unknown message.", result.errorMessage)
  }

  @Test
  fun `httpFailure accepts redirect status codes`() {
    val failure = ClerkResult.httpFailure<ClerkErrorResponse>(code = 300)

    assertEquals(300, failure.code)
    assertEquals(ClerkResult.Failure.ErrorType.HTTP, failure.errorType)
  }

  @Test
  fun `httpFailure accepts any non-2xx status code`() {
    listOf(101, 304, 404, 503, 600, 999).forEach { code ->
      assertEquals(code, ClerkResult.httpFailure<ClerkErrorResponse>(code = code).code)
    }
  }

  @Test
  fun `httpFailure rejects codes outside the HTTP status range`() {
    listOf(-1, 99, 1000).forEach { code ->
      assertThrows(IllegalArgumentException::class.java) {
        ClerkResult.httpFailure<ClerkErrorResponse>(code = code)
      }
    }
  }

  @Test
  fun `httpFailure rejects successful status codes`() {
    assertThrows(IllegalArgumentException::class.java) {
      ClerkResult.httpFailure<ClerkErrorResponse>(code = 200)
    }
  }

  @Test
  fun `logSummary keeps error codes and messages and drops the trace id and meta`() {
    val errorResponse =
      ClerkErrorResponse(
        errors =
          listOf(
            Error(
              code = "form_identifier_not_found",
              message = "Couldn't find your account.",
              meta = JsonObject(mapOf("session_id" to JsonPrimitive("sess_in_error_meta"))),
            )
          ),
        meta = JsonObject(mapOf("client_id" to JsonPrimitive("client_in_meta"))),
        clerkTraceId = "trace_secret",
      )

    val summary = ClerkResult.httpFailure(code = 422, error = errorResponse).logSummary()

    assertEquals("HTTP 422 form_identifier_not_found: Couldn't find your account.", summary)
  }

  @Test
  fun `logSummary names the throwable type without its message`() {
    val summary = ClerkResult.unknownFailure(IOException("host api.clerk.com")).logSummary()

    assertEquals("UNKNOWN IOException", summary)
  }
}
