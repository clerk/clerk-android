package com.clerk.api.network.model.environment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Internal data class representing authentication configuration from the Clerk environment.
 *
 * This class contains authentication-related settings that control how the SDK handles user
 * sessions and authentication behavior.
 *
 * @property singleSessionMode Whether the application is configured for single session mode. When
 *   true, only one active session is allowed per user at a time.
 * @property sessionMinter Whether session token minting at the edge is enabled.
 * @property nativeSettings Native-app specific settings, such as biometric sign-in.
 */
@Serializable
internal data class AuthConfig(
  /**
   * Whether the application is configured for single session mode. When true, only one active
   * session is allowed per user at a time.
   */
  @SerialName("single_session_mode") val singleSessionMode: Boolean,

  /** Whether session token minting at the edge is enabled. */
  @SerialName("session_minter") val sessionMinter: Boolean = false,

  /** Native-app specific settings, such as biometric sign-in. */
  @SerialName("native_settings") val nativeSettings: NativeSettings = NativeSettings(),
) {

  /**
   * Native-app specific settings from the Clerk environment.
   *
   * @property apiEnabled Whether the Clerk Native API is enabled for this instance.
   * @property biometricSignInEnabled Whether biometric sign-in is enabled.
   * @property biometricCredentialPromptAfterSignInEnabled Whether the biometric-credential
   *   enrollment prompt should be offered after sign-in.
   * @property biometricCredentialPromptAfterSignUpEnabled Whether the biometric-credential
   *   enrollment prompt should be offered after sign-up.
   */
  @Serializable
  internal data class NativeSettings(
    /** Whether the Clerk Native API is enabled for this instance. */
    @SerialName("api_enabled") val apiEnabled: Boolean = false,

    /** Whether biometric sign-in is enabled. */
    @SerialName("trusted_device_sign_in_enabled") val biometricSignInEnabled: Boolean = false,

    /** Whether the biometric credential enrollment prompt should be offered after sign-in. */
    @SerialName("trusted_device_enrollment_prompt_after_sign_in_enabled")
    val biometricCredentialPromptAfterSignInEnabled: Boolean = false,

    /** Whether the biometric credential enrollment prompt should be offered after sign-up. */
    @SerialName("trusted_device_enrollment_prompt_after_sign_up_enabled")
    val biometricCredentialPromptAfterSignUpEnabled: Boolean = false,
  )
}
