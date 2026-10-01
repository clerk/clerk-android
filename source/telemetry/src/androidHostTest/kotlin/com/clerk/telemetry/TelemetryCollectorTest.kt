package com.clerk.telemetry

import com.clerk.api.Clerk
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class TelemetryCollectorTest {

  @AfterTest
  fun resetClerk() {
    Clerk.publishableKey = null
  }

  @Test
  fun sendsEventsFromDevelopmentInstances() {
    assertEquals(listOf("development"), sentInstanceTypes(environment("development")))
  }

  @Test
  fun skipsProductionInstances() {
    assertEquals(emptyList(), sentInstanceTypes(environment("production")))
  }

  @Test
  fun skipsUnknownInstances() {
    assertEquals(emptyList(), sentInstanceTypes(environment("unknown")))
  }

  @Test
  fun skipsWhenTelemetryIsDisabled() {
    assertEquals(
      emptyList(),
      sentInstanceTypes(environment("development", telemetryEnabled = false)),
    )
  }

  @Test
  fun skipsWhenDebugModeIsEnabled() {
    assertEquals(emptyList(), sentInstanceTypes(environment("development", debugMode = true)))
  }

  @Test
  fun clerkEnvironmentSendsTestKeyEventsAsDevelopment() {
    Clerk.publishableKey = TEST_PUBLISHABLE_KEY

    assertEquals(listOf("development"), sentInstanceTypes(ClerkTelemetryEnvironment()))
  }

  @Test
  fun clerkEnvironmentSkipsLiveKeyEvents() {
    Clerk.publishableKey = LIVE_PUBLISHABLE_KEY

    assertEquals(emptyList(), sentInstanceTypes(ClerkTelemetryEnvironment()))
  }

  private fun environment(
    instanceType: String,
    telemetryEnabled: Boolean = true,
    debugMode: Boolean = false,
  ) =
    ClerkTelemetryEnvironment(
      sdkVersion = "0.0.0",
      instanceTypeProvider = { instanceType },
      telemetryEnabledProvider = { telemetryEnabled },
      debugModeEnabledProvider = { debugMode },
      publishableKeyProvider = { null },
    )

  private fun sentInstanceTypes(environment: TelemetryEnvironment): List<String> = runBlocking {
    val bodies = mutableListOf<String>()
    val engine = MockEngine { request ->
      bodies += request.body.toByteArray().decodeToString()
      respondOk()
    }
    val collector =
      TelemetryCollector(
        options = TelemetryCollectorOptions(disableThrottling = true),
        client = HttpClient(engine) { install(ContentNegotiation) { json() } },
        environment = environment,
        throttler = NeverThrottled,
      )

    collector.record(TelemetryEventRaw(event = "test_event", payload = emptyMap()))
    collector.flush()

    bodies.flatMap { body ->
      Json.parseToJsonElement(body).jsonObject.getValue("events").jsonArray.map {
        it.jsonObject.getValue("it").jsonPrimitive.content
      }
    }
  }

  private object NeverThrottled : TelemetryEventThrottler {
    override suspend fun isEventThrottled(event: TelemetryEvent) = false
  }

  private companion object {
    const val TEST_PUBLISHABLE_KEY = "pk_test_ZXhhbXBsZS5jbGVyay5hY2NvdW50cy5kZXYk"
    const val LIVE_PUBLISHABLE_KEY = "pk_live_Y2xlcmsuZXhhbXBsZS5jb20k"
  }
}
