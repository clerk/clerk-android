package com.clerk.sdk.model.environment

import com.clerk.sdk.model.error.ClerkAPIError
import com.clerk.sdk.network.ClerkApi
import com.slack.eithernet.ApiResult
import kotlinx.serialization.Serializable

@Serializable
data class Environment(
  val authConfig: AuthConfig,
  val userSettings: UserSettings,
  val displayConfig: DisplayConfig,
  val fraudSettings: FraudSettings,
) {
  companion object {
    suspend fun get(): ApiResult<Environment, ClerkAPIError> = ClerkApi.apiService.environment()
  }
}
