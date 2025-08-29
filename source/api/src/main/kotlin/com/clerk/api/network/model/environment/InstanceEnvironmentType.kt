package com.clerk.api.network.model.environment

import com.clerk.api.network.serialization.FallbackEnumSerializer
import com.clerk.api.network.serialization.createFallbackEnumSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * An enumeration representing the type of environment for an instance.
 *
 * This is used to distinguish between production and development environments, allowing for
 * environment-specific configurations and behaviors.
 */
@Serializable(with = InstanceEnvironmentTypeSerializer::class)
enum class InstanceEnvironmentType {

  /** Represents a production environment. */
  @SerialName("production") PRODUCTION,

  /** Represents a development environment. */
  @SerialName("development") DEVELOPMENT,

  /** Used as a fallback in case of decoding error. */
  @SerialName("unknown") UNKNOWN,
}

/**
 * Custom serializer for InstanceEnvironmentType that provides fallback to UNKNOWN.
 */
object InstanceEnvironmentTypeSerializer : FallbackEnumSerializer<InstanceEnvironmentType>(
  "InstanceEnvironmentType",
  InstanceEnvironmentType.UNKNOWN,
  InstanceEnvironmentType.entries.toTypedArray()
)
