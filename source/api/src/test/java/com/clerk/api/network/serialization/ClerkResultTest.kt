package com.clerk.api.network.serialization

import com.clerk.api.network.model.error.ClerkErrorResponse
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Test

class ClerkResultTest {

  @Test
  fun `errorMessage describes network failures`() {
    val result: ClerkResult.Failure<ClerkErrorResponse> =
      ClerkResult.unknownFailure(IOException("Unable to resolve host api.clerk.com"))

    assertEquals(
      "No internet connection detected. Please check your network connection and try again.",
      result.errorMessage,
    )
  }

  @Test
  fun `errorMessage keeps generic fallback for non-network failures`() {
    val result: ClerkResult.Failure<ClerkErrorResponse> =
      ClerkResult.unknownFailure(IllegalStateException("Unexpected failure"))

    assertEquals("Error occurred with unknown message.", result.errorMessage)
  }
}
