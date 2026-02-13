package com.clerk.api.forceupdate

/**
 * Represents the force-update viability status for the currently running app build.
 */
data class ForceUpdateStatus(
  val isSupported: Boolean,
  val currentVersion: String?,
  val minimumVersion: String?,
  val updateUrl: String?,
  val reason: Reason,
) {
  enum class Reason {
    SUPPORTED,
    NO_POLICY,
    MISSING_CURRENT_VERSION,
    INVALID_CURRENT_VERSION,
    INVALID_MINIMUM_VERSION,
    BELOW_MINIMUM,
    SERVER_REJECTED,
  }

  companion object {
    val SupportedDefault =
      ForceUpdateStatus(
        isSupported = true,
        currentVersion = null,
        minimumVersion = null,
        updateUrl = null,
        reason = Reason.NO_POLICY,
      )
  }
}
