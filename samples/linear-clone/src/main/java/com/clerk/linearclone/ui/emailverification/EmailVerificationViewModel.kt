package com.clerk.linearclone.ui.emailverification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.Clerk
import com.clerk.signin.SignIn
import com.clerk.signin.attemptFirstFactor
import kotlinx.coroutines.launch

class EmailVerificationViewModel : ViewModel() {

  fun verify(code: String) {
    val inProgressSignIn = Clerk.signIn
    viewModelScope.launch {
      inProgressSignIn?.attemptFirstFactor(SignIn.AttemptFirstFactorParams.EmailCode(code = code))
    }
  }
}
