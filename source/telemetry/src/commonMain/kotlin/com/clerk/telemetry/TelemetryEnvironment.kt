package com.clerk.telemetry

interface TelemetryEnvironment {
  val sdkName: String
  val sdkVersion: String

  suspend fun instanceTypeString(): String

  suspend fun isTelemetryEnabled(): Boolean

  suspend fun isDebugModeEnabled(): Boolean

  suspend fun publishableKey(): String?
}
