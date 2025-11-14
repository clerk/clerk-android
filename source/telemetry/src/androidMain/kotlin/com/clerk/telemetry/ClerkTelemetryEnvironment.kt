package com.clerk.telemetry

import com.clerk.api.Clerk

class ClerkTelemetryEnvironment : TelemetryEnvironment {

  override val sdkName: String = "clerk-android"
  override val sdkVersion: String = Clerk.version // adjust to your static version access

  override suspend fun instanceTypeString(): String {
    // adjust to your actual model
    return Clerk.instanceEnvironmentType.name
  }

  override suspend fun isTelemetryEnabled(): Boolean {
    return Clerk.telemetryEnabled
  }

  override suspend fun isDebugModeEnabled(): Boolean {
    return Clerk.debugMode
  }

  override suspend fun publishableKey(): String? {
    val key = Clerk.publishableKey
    return key.takeIf { !it.isNullOrEmpty() }
  }
}
