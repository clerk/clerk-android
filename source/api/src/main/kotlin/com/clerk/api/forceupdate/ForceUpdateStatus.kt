package com.clerk.api.forceupdate

/**
 * Represents the force-update viability status for the currently running app build.
 */
data class ForceUpdateStatus(
  val isSupported: Boolean,
  val minimumVersion: String?,
  val updateUrl: String?,
) {
  companion object {
    val SupportedDefault =
      ForceUpdateStatus(
        isSupported = true,
        minimumVersion = null,
        updateUrl = null,
      )
  }
}
