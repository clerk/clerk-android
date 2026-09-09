package com.clerk.telemetry

private const val CLERK_ANDROID = "clerk-android"

/** A [TelemetryEnvironment] implementation that reads values from injected providers. */
class ClerkTelemetryEnvironment(
  override val sdkVersion: String,
  private val instanceTypeProvider: suspend () -> String,
  private val telemetryEnabledProvider: suspend () -> Boolean,
  private val debugModeEnabledProvider: suspend () -> Boolean,
  private val publishableKeyProvider: suspend () -> String?,
) : TelemetryEnvironment {

  override val sdkName: String = CLERK_ANDROID

  override suspend fun instanceTypeString(): String {
    return instanceTypeProvider()
  }

  override suspend fun isTelemetryEnabled(): Boolean {
    return telemetryEnabledProvider()
  }

  override suspend fun isDebugModeEnabled(): Boolean {
    return debugModeEnabledProvider()
  }

  override suspend fun publishableKey(): String? {
    val key = publishableKeyProvider()
    return key.takeIf { !it.isNullOrEmpty() }
  }
}
