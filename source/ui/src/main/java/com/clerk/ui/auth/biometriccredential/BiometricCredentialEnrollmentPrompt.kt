package com.clerk.ui.auth.biometriccredential

import android.content.SharedPreferences
import androidx.core.content.edit
import com.clerk.api.BiometricCredentialAvailability
import com.clerk.api.BiometricCredentialUnavailableReason
import com.clerk.api.Clerk
import com.clerk.api.User

/** Decides whether the post-auth biometric credential enrollment prompt should be offered. */
internal object BiometricCredentialEnrollmentPrompt {

  /**
   * Returns whether the enrollment prompt should be offered after a completed auth flow.
   *
   * The prompt is offered when the biometric sign-in feature and the matching prompt setting are
   * enabled, the device supports biometric authentication, the session allows enrollment, and this
   * device doesn't already hold a usable credential for the user.
   */
  suspend fun shouldOffer(
    clerk: Clerk,
    afterSignUp: Boolean,
    sharedPreferences: SharedPreferences,
  ): Boolean {
    val userId = clerk.user?.id ?: return false
    val sessionId = clerk.session?.id ?: return false
    if (!clerk.biometricCredentials.canEnroll) return false
    val settings = clerk.environment.authConfig.nativeSettings ?: return false
    val enabled =
      if (afterSignUp) settings.trustedDeviceEnrollmentPromptAfterSignUpEnabled
      else
        settings.trustedDeviceEnrollmentPromptAfterSignInEnabled &&
          !hasSeenPrompt(sharedPreferences, userId)
    if (!enabled) return false
    val availability =
      clerk.biometricCredentials.localAvailability(
        com.clerk.api.BiometricCredentialSelectionParams(currentUser = true)
      )
    return clerk.user?.id == userId &&
      clerk.session?.id == sessionId &&
      !availability.isAvailable &&
      availability.canPromptForEnrollment
  }

  fun markPromptSeen(clerk: Clerk, sharedPreferences: SharedPreferences) {
    val userId = clerk.user?.id ?: return
    sharedPreferences.edit(commit = true) { putBoolean(storageKey(userId), true) }
  }

  private fun hasSeenPrompt(sharedPreferences: SharedPreferences, userId: String): Boolean {
    return sharedPreferences.getBoolean(storageKey(userId), false)
  }

  private fun storageKey(userId: String): String = "$STORAGE_KEY_PREFIX.$userId"

  private const val STORAGE_KEY_PREFIX = "clerk_trusted_device_enrollment_prompt_seen"
}

/** Whether this availability state allows offering the enrollment prompt. */
internal val BiometricCredentialAvailability.canPromptForEnrollment: Boolean
  get() =
    when (unavailableReason) {
      BiometricCredentialUnavailableReason.NoLocalCredential,
      BiometricCredentialUnavailableReason.LocalKeyMissing,
      BiometricCredentialUnavailableReason.ServerCredentialMissing,
      BiometricCredentialUnavailableReason.ServerCredentialRevoked -> true
      else -> false
    }

/** A local-only user identifier hint used to select the credential during later sign-ins. */
internal val User.biometricCredentialIdentifierHint: String?
  get() =
    primaryEmailAddress?.emailAddress?.takeIf { it.isNotBlank() }
      ?: primaryPhoneNumber?.phoneNumber?.takeIf { it.isNotBlank() }
      ?: username?.takeIf { it.isNotBlank() }
