package com.clerk.api.appversion

/**
 * Whether this build of your app meets the minimum version set for it in the Clerk Dashboard.
 *
 * When the version is unsupported, Clerk rejects your app's requests until the user updates. The
 * Clerk UI library shows its update-required screen over your app automatically; call
 * [com.clerk.api.ClerkConfigurationOptions.withUpdateRequiredViewDisabled] to show your own UI
 * instead.
 *
 * @property isSupported Whether this app version is supported.
 * @property currentVersion This build's `versionCode`, or `null` before
 *   [com.clerk.api.Clerk.initialize] reads it.
 * @property minimumVersion The oldest supported `versionCode`, when a minimum is set for this app.
 * @property updateUrl The store page to send users to so they can update.
 */
public data class AppVersionSupportStatus(
  val isSupported: Boolean,
  val currentVersion: Long?,
  val minimumVersion: Long? = null,
  val updateUrl: String? = null,
) {
  internal companion object {
    fun supported(currentVersion: Long?): AppVersionSupportStatus =
      AppVersionSupportStatus(isSupported = true, currentVersion = currentVersion)
  }
}
