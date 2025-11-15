package com.clerk.telemetry

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

data class TelemetryCollectorOptions(
  val samplingRate: Double = 1.0,
  val maxBufferSize: Int = 5,
  val flushIntervalSeconds: Long = 30L,
  val disableThrottling: Boolean = false,
) {
  init {
    require(samplingRate in 0.0..1.0) { "samplingRate must be in [0,1]" }
  }

  val normalizedMaxBufferSize: Int = maxOf(1, maxBufferSize)
  val normalizedFlushIntervalSeconds: Long = maxOf(1L, flushIntervalSeconds)
}

/**
 * A telemetry event as sent to the backend.
 *
 * Note: Android version also does not include `cv` / `sk` here; that’s handled elsewhere if needed.
 */
@Serializable
data class TelemetryEvent(
  @SerialName("event") val event: String,
  @SerialName("it") val instanceType: String,
  @SerialName("sdk") val sdkName: String,
  @SerialName("sdkv") val sdkVersion: String,
  @SerialName("pk") val publishableKey: String? = null,
  @SerialName("payload") val payload: Map<String, JsonElement>,
)

/** Raw input describing an event before environment enrichment. */
data class TelemetryEventRaw(
  val event: String,
  val payload: Map<String, JsonElement>,
  /** Optional per-event sampling override in [0, 1]. */
  val eventSamplingRate: Double? = null,
)
