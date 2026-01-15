package com.clerk.linearclone.ui.chooseloginmethod

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.sso.OAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChooseLoginViewModel : ViewModel() {

  fun authWithGoogle() {
    viewModelScope.launch(Dispatchers.IO) { Clerk.auth.signInWithOAuth(OAuthProvider.GOOGLE) }
  }

  fun authenticateWithPasskey() {
    viewModelScope.launch(Dispatchers.IO) { Clerk.auth.signInWithPasskey() }
  }
}
