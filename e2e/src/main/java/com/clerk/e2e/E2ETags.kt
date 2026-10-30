package com.clerk.e2e

internal object E2ETags {
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
    const val RESET_PASSWORD_REVERIFICATION = "e2e.home.resetPasswordReverification"
  }

  object ResetPasswordReverification {
    const val SEND_CODE = "verify.resetPasswordReverification.sendCode"
    const val SENT_TO = "verify.resetPasswordReverification.sentTo"
    const val CODE = "verify.resetPasswordReverification.code"
    const val NEW_PASSWORD = "verify.resetPasswordReverification.newPassword"
    const val VERIFY = "verify.resetPasswordReverification.verify"
    const val ERROR = "verify.resetPasswordReverification.error"
  }

  object Launch {
    const val ERROR = "e2e.launch.error"
  }
}
