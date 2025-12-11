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

  fun httpClient(): HttpClient = HttpClient(OkHttp) { install(ContentNegotiation) { json(json) } }

  fun createCollector(
    context: Context,
    environment: TelemetryEnvironment,
    options: TelemetryCollectorOptions = TelemetryCollectorOptions(),
  ): TelemetryCollector {
    val throttler = AndroidTelemetryEventThrottler(context, json)
    return TelemetryCollector(
      options = options,
      client = httpClient(),
      environment = environment,
      throttler = throttler,
      json = json,
    )
  }
}
