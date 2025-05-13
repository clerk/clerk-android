package com.clerk.sdk.model.environment

import kotlinx.serialization.Serializable

@Serializable
data class Environment(
  val authConfig: AuthConfig,
  val userSettings: UserSettings,
  val displayConfig: DisplayConfig,
  val fraudSettings: FraudSettings,
)
