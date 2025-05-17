package com.clerk.sdk.model.environment

import com.clerk.sdk.model.error.ClerkErrorResponse
import com.clerk.sdk.network.ClerkApi
import com.slack.eithernet.ApiResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Environment(
  @SerialName("auth_config") val authConfig: AuthConfig,
  @SerialName("display_config") val displayConfig: DisplayConfig,
  @SerialName("user_settings") val userSettings: UserSettings,
  @SerialName("fraud_settings") val fraudSettings: FraudSettings,
) {
  companion object {

    /** Fetches the environment configuration from the Clerk API. */
    suspend fun get(): ApiResult<Environment, ClerkErrorResponse> = ClerkApi.instance.environment()
  }
}
