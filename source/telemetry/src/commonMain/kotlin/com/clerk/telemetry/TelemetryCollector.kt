package com.clerk.telemetry

import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

enum class InstanceType {
  Production,
  Development,
}

data class PublishableKey(val instanceType: InstanceType, val frontendApi: String)

@Serializable
data class TelemetryEvent(
  val event: String,
  val cv: String,
  val it: String,
  val sdk: String,
  val sdkv: String,
  val pk: String? = null,
  val sk: String? = null,
  val payload: JsonObject,
)

@Serializable data class TelemetryEventBatch(val events: List<TelemetryEvent>)

data class TelemetryEventRaw(
  val event: String,
  val payload: JsonObject,
  val eventSamplingRate: Double? = null,
)

data class TelemetryCollectorOptions(
  val samplingRate: Double? = null,
  val disabled: Boolean = false,
  val debug: Boolean = false,
  val maxBufferSize: Int? = null,
  val clerkVersion: String? = null,
  val sdk: String,
  val sdkVersion: String,
  val publishableKey: String? = null,
  val secretKey: String? = null,
  val endpoint: String? = null,
  val scope: CoroutineScope? = null,
  val httpClient: TelemetryHttpClient,
  val env: EnvProvider = EnvProvider { null },
  val logger: TelemetryLogger? = null,
)

interface TelemetryCollector {
  val isEnabled: Boolean
  val isDebug: Boolean

  fun record(event: TelemetryEventRaw)
}

internal fun isTruthyEnv(env: EnvProvider, name: String): Boolean {
  val value = env(name) ?: return false
  return value.equals("1", true) || value.equals("true", true) || value.equals("yes", true)
}
