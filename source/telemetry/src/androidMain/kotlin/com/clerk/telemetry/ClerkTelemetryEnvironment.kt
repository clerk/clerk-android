package com.clerk.telemetry

import com.clerk.api.Clerk

private const val CLERK_ANDROID = "clerk-android"

/**
 * An implementation of [TelemetryEnvironment] that retrieves configuration and state directly from
 * the main [Clerk] singleton. This class acts as a bridge between the telemetry system and the core
 * Clerk SDK's settings.
 */
class ClerkTelemetryEnvironment : TelemetryEnvironment {

  override val sdkName: String = CLERK_ANDROID
  override val sdkVersion: String = Clerk.version

  override suspend fun instanceTypeString(): String {
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
