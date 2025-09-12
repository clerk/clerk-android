package com.clerk.workbench

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.serialization.longErrorMessageOrNull
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signin.SignIn
import com.clerk.api.user.createPasskey
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

  fun createPasskey() {
    viewModelScope.launch {
      Clerk.userFlow.value!!.createPasskey()
        .onSuccess { ClerkLog.d("Passkey created: $it") }
        .onFailure { ClerkLog.e("Failed to create passkey: ${it.longErrorMessageOrNull}") }
    }
  }

  fun authenticate() {
    viewModelScope.launch {
      SignIn.create(
          SignIn.CreateParams.Strategy.Password(identifier = "sam@clerk.dev", password = "Broyce77")
        )
        .onSuccess { ClerkLog.d("Signed in: ${Clerk.userFlow.value}") }
        .onFailure { ClerkLog.e("Failed to sign in: ${it.longErrorMessageOrNull}") }
    }
  }
}
