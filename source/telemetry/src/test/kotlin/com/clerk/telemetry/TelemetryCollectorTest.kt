package com.clerk.telemetry

import com.clerk.api.Clerk
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.engine.mock.toByteArray
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer

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

  @Test
  fun postsJsonToTheEventEndpoint() {
    val requests = mutableListOf<okhttp3.Request>()
    val client =
      OkHttpClient.Builder()
        .addInterceptor { chain ->
          requests += chain.request()
          okResponse(chain.request())
        }
        .build()

    runBlocking { recordAndFlush(collector(environment("development"), client)) }

    val request = requests.single()
    assertEquals("POST", request.method)
    assertEquals("https://clerk-telemetry.com/v1/event", request.url.toString())
    assertEquals("application/json; charset=utf-8", request.body?.contentType().toString())
  }

  @Suppress("DEPRECATION")
  @Test
  fun deprecatedKtorConstructorStillPostsEvents() {
    val bodies = mutableListOf<String>()
    val engine = MockEngine { request ->
      bodies += request.body.toByteArray().decodeToString()
      respondOk()
    }
    val collector =
      TelemetryCollector(
        options = TelemetryCollectorOptions(disableThrottling = true),
        client = HttpClient(engine),
        environment = environment("development"),
        throttler = NeverThrottled,
      )

    runBlocking { recordAndFlush(collector) }

    assertEquals(listOf("development"), instanceTypes(bodies))
  }

  private fun sentInstanceTypes(environment: TelemetryEnvironment): List<String> = runBlocking {
    val bodies = mutableListOf<String>()
    val client =
      OkHttpClient.Builder()
        .addInterceptor { chain ->
          val buffer = Buffer()
          chain.request().body?.writeTo(buffer)
          bodies += buffer.readUtf8()
          okResponse(chain.request())
        }
        .build()

    recordAndFlush(collector(environment, client))

    instanceTypes(bodies)
  }

  private fun collector(environment: TelemetryEnvironment, client: OkHttpClient) =
    TelemetryCollector(
      options = TelemetryCollectorOptions(disableThrottling = true),
      environment = environment,
      throttler = NeverThrottled,
      client = client,
    )

  private suspend fun recordAndFlush(collector: TelemetryCollector) {
    collector.record(TelemetryEventRaw(event = "test_event", payload = emptyMap()))
    collector.flush()
  }

  private fun instanceTypes(bodies: List<String>): List<String> = bodies.flatMap { body ->
    Json.parseToJsonElement(body).jsonObject.getValue("events").jsonArray.map {
      it.jsonObject.getValue("it").jsonPrimitive.content
    }
  }

  private fun okResponse(request: okhttp3.Request): Response =
    Response.Builder()
      .request(request)
      .protocol(Protocol.HTTP_1_1)
      .code(200)
      .message("OK")
      .body("".toResponseBody())
      .build()

  private object NeverThrottled : TelemetryEventThrottler {
    override suspend fun isEventThrottled(event: TelemetryEvent) = false
  }

  private companion object {
    const val TEST_PUBLISHABLE_KEY = "pk_test_ZXhhbXBsZS5jbGVyay5hY2NvdW50cy5kZXYk"
    const val LIVE_PUBLISHABLE_KEY = "pk_live_Y2xlcmsuZXhhbXBsZS5jb20k"
  }
}
