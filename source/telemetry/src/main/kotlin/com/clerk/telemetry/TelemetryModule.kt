package com.clerk.telemetry

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object TelemetryModule {

  private val json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
  }

  /**
   * Creates a Ktor client configured for telemetry.
   *
   * The SDK no longer ships Ktor, so callers must declare `ktor-client-okhttp`,
   * `ktor-client-content-negotiation`, and `ktor-serialization-kotlinx-json` themselves.
   */
  @Deprecated(
    "The SDK posts telemetry with OkHttp and no longer ships Ktor. Use createCollector, or pass " +
      "an OkHttpClient to TelemetryCollector."
  )
  fun httpClient(): HttpClient = HttpClient(OkHttp) { install(ContentNegotiation) { json(json) } }

  fun createCollector(
    context: Context,
    environment: TelemetryEnvironment,
    options: TelemetryCollectorOptions = TelemetryCollectorOptions(),
  ): TelemetryCollector {
    val throttler = AndroidTelemetryEventThrottler(context, json)
    return TelemetryCollector(
      options = options,
      environment = environment,
      throttler = throttler,
      json = json,
    )
  }
}
