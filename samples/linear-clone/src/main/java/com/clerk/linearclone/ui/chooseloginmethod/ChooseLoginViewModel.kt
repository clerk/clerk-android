package com.clerk.linearclone.ui.chooseloginmethod

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.signin.SignIn
import com.clerk.sso.OAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChooseLoginViewModel : ViewModel() {

  fun authWithGoogle() {
    viewModelScope.launch(Dispatchers.IO) {
      SignIn.authenticateWithRedirect(
        SignIn.AuthenticateWithRedirectParams.OAuth(provider = OAuthProvider.GOOGLE)
      )
    }
  }

  fun authenticateWithPasskey() {
    viewModelScope.launch(Dispatchers.IO) {
      SignIn.authenticateWithGoogleCredential(listOf(SignIn.CredentialType.PASSKEY))
    }
  }
}
