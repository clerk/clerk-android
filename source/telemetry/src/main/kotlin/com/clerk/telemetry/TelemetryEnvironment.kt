package com.clerk.telemetry

public interface TelemetryEnvironment {
  public val sdkName: String
  public val sdkVersion: String

  public suspend fun instanceTypeString(): String

  public suspend fun isTelemetryEnabled(): Boolean

  public suspend fun isDebugModeEnabled(): Boolean

  public suspend fun publishableKey(): String?
}
