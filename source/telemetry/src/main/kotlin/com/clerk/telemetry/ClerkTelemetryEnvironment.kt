package com.clerk.telemetry

import com.clerk.api.Clerk
import com.clerk.api.network.model.environment.InstanceEnvironmentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive

private const val CLERK_ANDROID = "clerk-android"

/** A [TelemetryEnvironment] implementation that reads values from injected providers. */
public class ClerkTelemetryEnvironment(
  override val sdkVersion: String,
  private val instanceTypeProvider: suspend () -> String,
  private val telemetryEnabledProvider: suspend () -> Boolean,
  private val debugModeEnabledProvider: suspend () -> Boolean,
  private val publishableKeyProvider: suspend () -> String?,
) : TelemetryEnvironment {

  public constructor() :
    this(
      sdkVersion = Clerk.version,
      instanceTypeProvider = {
        Json.encodeToJsonElement(
            InstanceEnvironmentType.serializer(),
            Clerk.instanceEnvironmentType,
          )
          .jsonPrimitive
          .content
      },
      telemetryEnabledProvider = { Clerk.telemetryEnabled },
      debugModeEnabledProvider = { Clerk.debugMode },
      publishableKeyProvider = { Clerk.publishableKey },
    )

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
