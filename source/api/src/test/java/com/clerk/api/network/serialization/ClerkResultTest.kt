package com.clerk.api.network.serialization

import com.clerk.api.configuration.connectivity.NetworkConnectivityMonitor
import com.clerk.api.network.model.error.ClerkErrorResponse
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import java.io.IOException
import org.junit.After
import org.junit.Assert.assertEquals
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
}
