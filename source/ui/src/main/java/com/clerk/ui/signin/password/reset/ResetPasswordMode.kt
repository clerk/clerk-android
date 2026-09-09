package com.clerk.ui.signin.password.reset

import com.clerk.api.Clerk

internal enum class ResetPasswordMode {
  SIGN_IN,
  SESSION_TASK,
}

internal fun ResetPasswordMode.viewModelKey(clerk: Clerk): String {
  return when (this) {
    ResetPasswordMode.SIGN_IN -> {
      val signInId = clerk.signIn?.id ?: "no-sign-in"
      "reset-password-$signInId"
    }
    ResetPasswordMode.SESSION_TASK -> {
      val sessionId = clerk.session?.id ?: "no-session"
      "session-task-reset-password-$sessionId"
    }
  }
}
