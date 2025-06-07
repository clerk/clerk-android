package com.clerk.network.model.environment

import com.clerk.network.ClerkApi
import com.clerk.network.model.error.ClerkErrorResponse
import com.clerk.network.serialization.ClerkResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class Environment(
  @SerialName("auth_config") val authConfig: AuthConfig,
  @SerialName("display_config") val displayConfig: DisplayConfig,
  @SerialName("user_settings") val userSettings: UserSettings,
  @SerialName("fraud_settings") val fraudSettings: FraudSettings,
) {
  companion object {

    /** Fetches the environment configuration from the Clerk API. */
    suspend fun get(): ClerkResult<Environment, ClerkErrorResponse> =
      ClerkApi.instance.environment()
  }
}
