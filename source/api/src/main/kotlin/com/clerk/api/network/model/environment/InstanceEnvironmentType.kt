package com.clerk.api.network.model.environment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * An enumeration representing the type of environment for an instance.
 *
 * This is used to distinguish between production and development environments, allowing for
 * environment-specific configurations and behaviors.
 */
@Serializable
enum class InstanceEnvironmentType {

  /** Represents a production environment. */
  @SerialName("production") PRODUCTION,

  /** Represents a development environment. */
  @SerialName("development") DEVELOPMENT,

  /** Used as a fallback in case of decoding error. */
  @SerialName("unknown") UNKNOWN,
}
