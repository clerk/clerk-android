package com.clerk.linearclone.ui.chooseloginmethod

import com.clerk.api.Clerk
import com.clerk.api.MobileAuthenticationResult
import com.clerk.api.MobileSSOParams
import com.clerk.api.MobileSSOParamsStart
import com.clerk.api.SignInPasskeyParams
import com.clerk.api.SignInPasskeyParamsFlow
import com.clerk.api.SignInSSOParamsStrategy
import com.clerk.api.SignInStatus
import com.clerk.api.SignUpStatus
import com.clerk.linearclone.LinearFeedback
import com.clerk.linearclone.LinearViewModel
import com.clerk.ui.auth.AuthMode

class ChooseLoginViewModel(clerk: Clerk, feedback: LinearFeedback) :
  LinearViewModel(clerk, feedback) {
  fun authWithGoogle() = runOperation {
    when (
      val result =
        clerk.authenticateWithSSO(
          MobileSSOParams(
            strategy = SignInSSOParamsStrategy.OauthGoogle,
            start = MobileSSOParamsStart.SignIn,
            transferable = true,
          )
        )
    ) {
      is MobileAuthenticationResult.Case1 -> {
        val attempt = result.value.signIn
        if (attempt.status == SignInStatus.Complete) attempt.finalize()
        else feedback.continuation.value = AuthMode.SignIn
      }
      is MobileAuthenticationResult.Case2 -> {
        val attempt = result.value.signUp
        if (attempt.status == SignUpStatus.Complete) attempt.finalize()
        else feedback.continuation.value = AuthMode.SignUp
      }
    }
  }

  fun authenticateWithPasskey() = runOperation {
    val attempt = clerk.signIn
    attempt.passkey(SignInPasskeyParams(flow = SignInPasskeyParamsFlow.Discoverable))
    if (attempt.status == SignInStatus.Complete) attempt.finalize()
    else feedback.continuation.value = AuthMode.SignIn
  }
}
