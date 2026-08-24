package com.clerk.ui.auth.biometriccredential

import android.content.SharedPreferences
import androidx.core.content.edit
import com.clerk.api.Clerk
import com.clerk.api.biometriccredential.BiometricCredentialAvailability
import com.clerk.api.log.ClerkLog
import com.clerk.api.session.Session
import com.clerk.api.user.User

/** Decides whether the post-auth biometric-credential enrollment prompt should be offered. */
internal object BiometricCredentialEnrollmentPrompt {

  /**
   * Returns whether the enrollment prompt should be offered after a completed auth flow.
   *
   * The prompt is offered when the biometric-credential feature and the matching prompt setting are
   * enabled, the device supports biometric authentication, the session allows enrollment, and this
   * device doesn't already hold a usable credential for the user.
   */
  @Suppress("ReturnCount")
  fun shouldOffer(afterSignUp: Boolean, sharedPreferences: SharedPreferences): Boolean {
    val userId = Clerk.user?.id ?: return skip("no signed-in user")
    val sessionStatus = Clerk.session?.status ?: return skip("no session")
    if (
      sessionStatus != Session.SessionStatus.ACTIVE &&
        sessionStatus != Session.SessionStatus.PENDING
    ) {
      return skip("session status is $sessionStatus")
    }

    val promptSettingIsEnabled =
      if (afterSignUp) {
        Clerk.biometricCredentialPromptAfterSignUpIsEnabled
      } else {
        Clerk.biometricCredentialPromptAfterSignInIsEnabled &&
          !hasSeenPrompt(sharedPreferences, userId)
      }
    if (!promptSettingIsEnabled) {
      return skip(
        "prompt setting disabled or already seen " +
          "(afterSignUp=$afterSignUp, " +
          "promptAfterSignIn=${Clerk.biometricCredentialPromptAfterSignInIsEnabled}, " +
          "promptAfterSignUp=${Clerk.biometricCredentialPromptAfterSignUpIsEnabled}, " +
          "seen=${hasSeenPrompt(sharedPreferences, userId)})"
      )
    }

    if (!Clerk.biometricCredentials.deviceSupportsBiometricAuthentication) {
      return skip("device does not support biometric authentication")
    }

    val availability = Clerk.biometricCredentials.currentUserLocalAvailability()
    if (availability.isAvailable) {
      return skip("device already enrolled")
    }
    if (!availability.canPromptForEnrollment) {
      return skip("unavailable for reason ${availability.unavailableReason}")
    }
    return true
  }

  private fun skip(reason: String): Boolean {
    ClerkLog.d("Biometric-credential enrollment prompt not offered: $reason")
    return false
  }

  fun markPromptSeen(sharedPreferences: SharedPreferences) {
    val userId = Clerk.user?.id ?: return
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
      BiometricCredentialAvailability.UnavailableReason.NO_LOCAL_CREDENTIAL,
      BiometricCredentialAvailability.UnavailableReason.LOCAL_KEY_MISSING,
      BiometricCredentialAvailability.UnavailableReason.SERVER_CREDENTIAL_MISSING,
      BiometricCredentialAvailability.UnavailableReason.SERVER_CREDENTIAL_REVOKED -> true
      else -> false
    }

/** A local-only user identifier hint used to select the credential during later sign-ins. */
internal val User.biometricCredentialIdentifierHint: String?
  get() =
    primaryEmailAddress?.emailAddress?.takeIf { it.isNotBlank() }
      ?: primaryPhoneNumber?.phoneNumber?.takeIf { it.isNotBlank() }
      ?: username?.takeIf { it.isNotBlank() }
