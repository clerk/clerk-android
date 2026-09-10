package com.clerk.api

import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class AndroidTimerCapabilityTest {
  private fun host() =
    AndroidCapabilities(
      publishableKey = "fixture",
      frontendAPI = "https://example.com",
      storage =
        object : CredentialStorage {
          override suspend fun read(): String? = error("Unexpected storage")

          override suspend fun write(value: String) {
            error("Unexpected storage")
          }

          override suspend fun remove() {
            error("Unexpected storage")
          }
        },
    )

  @Test
  fun fractionalTimerCompletes() = runBlocking {
    val result =
      withTimeout(1000) {
        host().perform("timer", buildJsonObject { put("milliseconds", 1.75) })
      }
    assertEquals(JsonNull, result)
  }

  @Test
  fun fractionalTimerCanBeCancelled() = runBlocking {
    val pending =
      launch(start = CoroutineStart.UNDISPATCHED) {
        host().perform("timer", buildJsonObject { put("milliseconds", 10000.75) })
        fail("Cancelled timer must not finish")
      }
    pending.cancelAndJoin()
    assertTrue(pending.isCancelled)
  }
}
