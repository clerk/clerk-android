package com.clerk.telemetry

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.core.readBytes
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TelemetryCollectorTest {

  private val json =
    Json {
      encodeDefaults = true
      ignoreUnknownKeys = true
    }

  @Test
  fun `record flush sends enriched telemetry payload`() = runTest {
    val recordedBodies = mutableListOf<String>()
    val collector =
      createCollector(
        recordedBodies = recordedBodies,
        environment =
          FakeTelemetryEnvironment(
            sdkName = "test-sdk",
            sdkVersion = "1.2.3",
            instanceType = "production",
            publishableKeyValue = "pk_test_123",
          ),
        options = TelemetryCollectorOptions(disableThrottling = true),
      )

    collector.record(
      TelemetryEventRaw(
        event = "CUSTOM_EVENT",
        payload = telemetryPayload("payload_key" to "payload_value"),
      ),
    )
    collector.flush()

    assertEquals(1, recordedBodies.size)
    val envelope = json.parseToJsonElement(recordedBodies.single()).jsonObject
    val event = envelope.getValue("events").jsonArray.single().jsonObject

    assertEquals("CUSTOM_EVENT", event.getValue("event").jsonPrimitive.content)
    assertEquals("production", event.getValue("it").jsonPrimitive.content)
    assertEquals("test-sdk", event.getValue("sdk").jsonPrimitive.content)
    assertEquals("1.2.3", event.getValue("sdkv").jsonPrimitive.content)
    assertEquals("pk_test_123", event.getValue("pk").jsonPrimitive.content)
    assertEquals(
      "payload_value",
      event.getValue("payload").jsonObject.getValue("payload_key").jsonPrimitive.content,
    )
  }

  @Test
  fun `record skips when telemetry disabled`() = runTest {
    val recordedBodies = mutableListOf<String>()
    val collector =
      createCollector(
        recordedBodies = recordedBodies,
        environment = FakeTelemetryEnvironment(telemetryEnabled = false),
        options = TelemetryCollectorOptions(disableThrottling = true),
      )

    collector.record(
      TelemetryEventRaw(event = "SKIPPED_EVENT", payload = telemetryPayload("key" to "value")),
    )
    collector.flush()

    assertTrue(recordedBodies.isEmpty())
  }

  @Test
  fun `record skips when throttler denies event`() = runTest {
    val recordedBodies = mutableListOf<String>()
    val throttler = FakeTelemetryEventThrottler(shouldThrottle = true)
    val collector =
      createCollector(
        recordedBodies = recordedBodies,
        environment = FakeTelemetryEnvironment(),
        throttler = throttler,
        options = TelemetryCollectorOptions(samplingRate = 1.0),
      )

    collector.record(
      TelemetryEventRaw(
        event = "THROTTLED_EVENT",
        payload = telemetryPayload("key" to "value"),
        eventSamplingRate = 1.0,
      ),
    )
    collector.flush()

    assertTrue(recordedBodies.isEmpty())
    assertEquals(1, throttler.invocationCount)
  }

  private fun createCollector(
    recordedBodies: MutableList<String>,
    environment: TelemetryEnvironment,
    throttler: TelemetryEventThrottler = FakeTelemetryEventThrottler(shouldThrottle = false),
    options: TelemetryCollectorOptions,
  ): TelemetryCollector {
    val client =
        HttpClient(MockEngine { request: HttpRequestData ->
          val bodyText = request.body.readText()
          recordedBodies += bodyText
          respond(
            content = TextContent("{}", ContentType.Application.Json),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
          )
        }) {
        install(ContentNegotiation) { json(json) }
      }

    return TelemetryCollector(
      options = options,
      client = client,
      environment = environment,
      throttler = throttler,
      json = json,
      endpointBaseUrl = "https://example.com",
    )
  }

  private suspend fun Any.readText(): String =
    when (this) {
      is TextContent -> text
      is OutgoingContent.ByteArrayContent -> bytes().decodeToString()
      is OutgoingContent.ReadChannelContent -> readFrom().readRemaining().readBytes().decodeToString()
      else -> error("Unsupported body type: ${this::class}")
    }

  private class FakeTelemetryEnvironment(
    override val sdkName: String = "test-sdk",
    override val sdkVersion: String = "1.0.0",
    private val instanceType: String = "production",
    private val telemetryEnabled: Boolean = true,
    private val debugMode: Boolean = false,
    private val publishableKeyValue: String? = "pk_test",
  ) : TelemetryEnvironment {
    override suspend fun instanceTypeString(): String = instanceType

    override suspend fun isTelemetryEnabled(): Boolean = telemetryEnabled

    override suspend fun isDebugModeEnabled(): Boolean = debugMode

    override suspend fun publishableKey(): String? = publishableKeyValue
  }

  private class FakeTelemetryEventThrottler(private val shouldThrottle: Boolean) :
    TelemetryEventThrottler {

    var invocationCount: Int = 0
      private set

    override suspend fun isEventThrottled(event: TelemetryEvent): Boolean {
      invocationCount++
      return shouldThrottle
    }
  }
}
