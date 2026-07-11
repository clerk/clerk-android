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
 * @property nativeSettings Native-app specific settings, such as trusted-device sign-in.
 */
@Serializable
internal data class AuthConfig(
  /**
   * Whether the application is configured for single session mode. When true, only one active
   * session is allowed per user at a time.
   */
  @SerialName("single_session_mode") val singleSessionMode: Boolean,

  /** Native-app specific settings, such as trusted-device sign-in. */
  @SerialName("native_settings") val nativeSettings: NativeSettings = NativeSettings(),
) {

  /**
   * Native-app specific settings from the Clerk environment.
   *
   * @property apiEnabled Whether the Clerk Native API is enabled for this instance.
   * @property trustedDeviceSignInEnabled Whether trusted-device (biometric) sign-in is enabled.
   * @property trustedDevicePromptAfterSignInEnabled Whether the trusted-device enrollment prompt
   *   should be offered after sign-in.
   * @property trustedDevicePromptAfterSignUpEnabled Whether the trusted-device enrollment prompt
   *   should be offered after sign-up.
   */
  @Serializable
  internal data class NativeSettings(
    /** Whether the Clerk Native API is enabled for this instance. */
    @SerialName("api_enabled") val apiEnabled: Boolean = false,

    /** Whether trusted-device (biometric) sign-in is enabled. */
    @SerialName("trusted_device_sign_in_enabled") val trustedDeviceSignInEnabled: Boolean = false,

    /** Whether the trusted-device enrollment prompt should be offered after sign-in. */
    @SerialName("trusted_device_enrollment_prompt_after_sign_in_enabled")
    val trustedDevicePromptAfterSignInEnabled: Boolean = false,

    /** Whether the trusted-device enrollment prompt should be offered after sign-up. */
    @SerialName("trusted_device_enrollment_prompt_after_sign_up_enabled")
    val trustedDevicePromptAfterSignUpEnabled: Boolean = false,
  )
}
