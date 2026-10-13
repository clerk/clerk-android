package com.clerk.e2e

internal object E2ETags {
  const val SIGN_IN = "e2e.auth.signIn"
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
  const val TOTP_TYPE_CODE = "e2e.totp.typeCode"
  const val TOTP_TYPED = "e2e.totp.typed"
  const val TOTP_FAILED = "e2e.totp.failed"

  object Auth {
    const val SIGN_IN = "e2e.auth.signIn"
    const val SIGN_IN_FULL_SCREEN = "e2e.auth.signInFullScreen"
    const val SIGNED_IN = "e2e.auth.signedIn"
    const val SIGNED_OUT = "e2e.auth.signedOut"
    const val SIGN_OUT = "e2e.auth.signOut"
    const val USER_ID = "e2e.auth.userId"
    const val SESSION_ID = "e2e.auth.sessionId"
  }

  object CustomSignIn {
    const val PHONE_NUMBER = "verify.customSignIn.phoneNumber"
    const val SEND_CODE = "verify.customSignIn.sendCode"
    const val CODE = "verify.customSignIn.code"
    const val VERIFY_CODE = "verify.customSignIn.verifyCode"
    const val ERROR = "verify.customSignIn.error"
  }

  object Home {
    const val CUSTOM_SIGN_IN = "e2e.home.customSignIn"
  }

  object Launch {
    const val ERROR = "e2e.launch.error"
  }
}
