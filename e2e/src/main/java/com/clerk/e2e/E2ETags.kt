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
  }

  object Launch {
    const val ERROR = "e2e.launch.error"
  }
}
