package com.clerk.telemetry

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Options used to configure the telemetry collector.
 *
 * Use this to control sampling, buffering, and environment metadata for development-only telemetry
 * in the Clerk KMM SDK.
 */
data class TelemetryCollectorOptions(
  /** Sampling rate in the range [0, 1]. */
  val samplingRate: Double = 1.0,
  /** Maximum number of events to buffer before forcing a flush. */
  val maxBufferSize: Int = 5,
  /** Time interval (in seconds) between periodic flushes. */
  val flushIntervalSeconds: Long = 30L,
  /**
   * If true, disables all filtering (throttling and sampling) for debugging purposes. When enabled,
   * ALL events will be recorded including duplicates and regardless of sampling rates. Buffering
   * and flushing still apply normally.
   */
  val disableThrottling: Boolean = false,
) {
  init {
    require(samplingRate in 0.0..1.0) { "samplingRate must be in [0, 1]" }
    require(maxBufferSize >= 1) { "maxBufferSize must be >= 1" }
    require(flushIntervalSeconds >= 1) { "flushIntervalSeconds must be >= 1" }
  }
}

/**
 * A telemetry event as sent to the Clerk telemetry backend.
 *
 * KMM version matches iOS shape: no `cv` or `sk`.
 */
@Serializable
data class TelemetryEvent(
  /** The event name (e.g. "method_invoked"). */
  val event: String,
  /** The instance type string (e.g. "development", "production"). */
  val it: String,
  /** The SDK name (e.g. "clerk-ios" / "clerk-android"). */
  val sdk: String,
  /** The SDK version string. */
  val sdkv: String,
  /** The publishable key, if available. */
  val pk: String? = null,
  /** Arbitrary JSON payload for the event. */
  val payload: JsonObject,
)

/** Raw input describing a telemetry event to be recorded by the collector. */
data class TelemetryEventRaw(
  /** The event name. */
  val event: String,
  /** Arbitrary JSON payload. */
  val payload: JsonObject,
  /**
   * Optional per-event sampling rate in [0, 1]. If omitted, defaults to the collector
   * `samplingRate`.
   */
  val eventSamplingRate: Double? = null,
)

@Serializable internal data class TelemetryRequestBody(val events: List<TelemetryEvent>)
