package com.clerk.telemetry

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object TelemetryEvents {
  // Sampling rates matching JS/iOS
  private const val METHOD_INVOKED_SAMPLING_RATE = 0.1
  private const val VIEW_DID_APPEAR_SAMPLING_RATE = 0.1

  fun methodInvoked(
    method: String,
    payload: Map<String, JsonElement> = emptyMap(),
    samplingRate: Double? = null,
  ): TelemetryEventRaw {
    val base = buildJsonObject { put("method", method) }
    val merged = base.toMutableMap().apply { putAll(payload) }
    return TelemetryEventRaw(
      event = "METHOD_INVOKED",
      payload = merged,
      eventSamplingRate = samplingRate ?: METHOD_INVOKED_SAMPLING_RATE,
    )
  }

  fun viewDidAppear(
    viewName: String,
    payload: Map<String, JsonElement> = emptyMap(),
    samplingRate: Double? = null,
  ): TelemetryEventRaw {
    val base = buildJsonObject { put("view", viewName) }
    val merged = base.toMutableMap().apply { putAll(payload) }
    return TelemetryEventRaw(
      event = "VIEW_DID_APPEAR",
      payload = merged,
      eventSamplingRate = samplingRate ?: VIEW_DID_APPEAR_SAMPLING_RATE,
    )
  }
}

// tiny helper if you want quick payloads
fun telemetryPayload(vararg pairs: Pair<String, Any?>): Map<String, JsonElement> = buildJsonObject {
  pairs.forEach { (k, v) ->
    when (v) {
      null -> put(k, JsonPrimitive(null as String?))
      is String -> put(k, v)
      is Boolean -> put(k, v)
      is Int -> put(k, v)
      is Long -> put(k, v)
      is Double -> put(k, v)
      else -> put(k, v.toString())
    }
  }
}
