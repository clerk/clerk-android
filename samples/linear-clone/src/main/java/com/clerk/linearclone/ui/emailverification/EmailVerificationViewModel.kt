package com.clerk.linearclone.ui.emailverification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.signin.verifyCode
import kotlinx.coroutines.launch

class EmailVerificationViewModel : ViewModel() {

  fun verify(code: String) {
    val inProgressSignIn = Clerk.auth.signIn
    viewModelScope.launch { inProgressSignIn?.verifyCode(code) }
  }
}
