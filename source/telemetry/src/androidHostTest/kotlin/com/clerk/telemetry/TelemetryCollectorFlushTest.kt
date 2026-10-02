package com.clerk.telemetry

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertIs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking

class TelemetryCollectorFlushTest {

  @Test
  fun flushRethrowsCancellation() {
    val collector = collector(MockEngine { throw CancellationException("cancelled") })

    val thrown = runBlocking {
      collector.record(TelemetryEventRaw(event = "test_event", payload = emptyMap()))
      runCatching { collector.flush() }.exceptionOrNull()
    }

    assertIs<CancellationException>(thrown)
  }

  @Test
  fun flushSwallowsDeliveryFailures() {
    val collector = collector(MockEngine { error("offline") })

    runBlocking {
      collector.record(TelemetryEventRaw(event = "test_event", payload = emptyMap()))
      collector.flush()
    }
  }

  private fun collector(engine: MockEngine) =
    TelemetryCollector(
      options = TelemetryCollectorOptions(disableThrottling = true),
      client = HttpClient(engine) { install(ContentNegotiation) { json() } },
      environment =
        ClerkTelemetryEnvironment(
          sdkVersion = "0.0.0",
          instanceTypeProvider = { "development" },
          telemetryEnabledProvider = { true },
          debugModeEnabledProvider = { false },
          publishableKeyProvider = { null },
        ),
      throttler = NeverThrottled,
    )

  private object NeverThrottled : TelemetryEventThrottler {
    override suspend fun isEventThrottled(event: TelemetryEvent) = false
  }
}
