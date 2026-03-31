package com.clerk.ui.signin.password.reset

import com.clerk.api.Clerk

internal enum class ResetPasswordMode {
  SIGN_IN,
  SESSION_TASK,
}

internal fun ResetPasswordMode.viewModelKey(): String {
  return when (this) {
    ResetPasswordMode.SIGN_IN -> {
      val signInId = Clerk.auth.currentSignIn?.id ?: "no-sign-in"
      "reset-password-$signInId"
    }
    ResetPasswordMode.SESSION_TASK -> {
      val sessionId = Clerk.session?.id ?: "no-session"
      "session-task-reset-password-$sessionId"
    }
  }
}
