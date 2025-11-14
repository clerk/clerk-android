package com.clerk.telemetry

interface TelemetryHttpClient {
  suspend fun postEvents(
    url: String,
    batch: TelemetryEventBatch,
    headers: Map<String, String> = emptyMap(),
  )
}

fun interface EnvProvider {
  operator fun invoke(name: String): String?
}

fun interface TelemetryLogger {
  fun logDebug(message: String)
}
