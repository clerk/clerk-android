package com.clerk.linearclone.ui.chooseloginmethod

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.signin.SignIn
import com.clerk.sso.OAuthProvider
import kotlinx.coroutines.launch

class ChooseLoginViewModel : ViewModel() {

  fun authWithGoogle() {
    viewModelScope.launch {
      SignIn.authenticateWithRedirect(
        SignIn.AuthenticateWithRedirectParams.OAuth(provider = OAuthProvider.GOOGLE)
      )
    }
  }
}
