package com.clerk.e2e

internal object E2ETags {
  const val SIGN_UP_WITH_EMAIL = "e2e.auth.signUpWithEmail"
  const val SIGN_UP_WITH_PHONE = "e2e.auth.signUpWithPhone"
  const val SIGNED_IN = "e2e.auth.signedIn"
  const val SIGNED_OUT = "e2e.auth.signedOut"
  const val SESSION_ACTIVE = "e2e.auth.sessionActive"
  const val SESSION_PENDING = "e2e.auth.sessionPending"
  const val SESSION_STATUS = "e2e.auth.sessionStatus"
  const val PENDING_TASKS = "e2e.auth.pendingTasks"
  const val USER_ID = "e2e.auth.userID"
  const val USER_BUTTON = "e2e.auth.userButton"
  const val SIGN_OUT = "e2e.auth.signOut"
  const val DELETE_ACCOUNT = "e2e.auth.deleteAccount"
  const val CLEANUP_IN_PROGRESS = "e2e.auth.cleanupInProgress"
  const val CLEANUP_COMPLETE = "e2e.auth.cleanupComplete"
  const val CLEANUP_FAILED = "e2e.auth.cleanupFailed"
  const val TOTP_FROM_CLIPBOARD = "e2e.totp.fromClipboard"
  const val TOTP_READY = "e2e.totp.ready"
  const val TOTP_FAILED = "e2e.totp.failed"
}
