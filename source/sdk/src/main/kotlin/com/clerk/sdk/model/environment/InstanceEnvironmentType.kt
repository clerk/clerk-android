package com.clerk.sdk.model.environment

/**
 * An enumeration representing the type of environment for an instance.
 *
 * This is used to distinguish between production and developmetn environments, allowing for
 * environment-specific configurations and behaviors.
 */
enum class InstanceEnvironmentType {

  /** Represents a production environment. */
  PRODUCTION,

  /** Represents a development environment. */
  DEVELOPMENT,

  /** Used as a fallback in case of decoding error. */
  UNKNOWN,
}
