package com.clerk.linearclone.ui.emailverification

import com.clerk.api.Clerk
import com.clerk.api.SignInEmailCodeVerifyParams
import com.clerk.api.SignInStatus
import com.clerk.linearclone.LinearFeedback
import com.clerk.linearclone.LinearViewModel
import com.clerk.ui.auth.AuthMode

class EmailVerificationViewModel(clerk: Clerk, feedback: LinearFeedback) :
  LinearViewModel(clerk, feedback) {
  fun verify(code: String) = runOperation {
    val attempt = clerk.signIn
    attempt.emailCode.verifyCode(SignInEmailCodeVerifyParams(code))
    if (attempt.status == SignInStatus.Complete) attempt.finalize()
    else feedback.continuation.value = AuthMode.SignIn
  }
}
