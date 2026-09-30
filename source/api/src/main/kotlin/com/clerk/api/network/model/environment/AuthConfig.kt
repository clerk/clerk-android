package com.clerk.api.network.model.environment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AuthConfig(
  @SerialName("single_session_mode") val singleSessionMode: Boolean,

  /** Whether session token minting at the edge is enabled. */
  @SerialName("session_minter") val sessionMinter: Boolean = false,

  @SerialName("native_settings") val nativeSettings: NativeSettings = NativeSettings(),
) {
  @Serializable
  internal data class NativeSettings(
    @SerialName("api_enabled") val apiEnabled: Boolean = false,

    @SerialName("trusted_device_sign_in_enabled") val biometricSignInEnabled: Boolean = false,

    @SerialName("trusted_device_enrollment_prompt_after_sign_in_enabled")
    val biometricCredentialPromptAfterSignInEnabled: Boolean = false,

    @SerialName("trusted_device_enrollment_prompt_after_sign_up_enabled")
    val biometricCredentialPromptAfterSignUpEnabled: Boolean = false,
  )
}
