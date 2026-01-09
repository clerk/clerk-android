package com.clerk.quickstart.signin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SignInViewModel : ViewModel() {
  private val _uiState = MutableStateFlow<SignInUiState>(SignInUiState.SignedOut)
  val uiState = _uiState.asStateFlow()

  fun signIn(email: String, password: String) {
    viewModelScope.launch {
      Clerk.auth
        .signInWithPassword {
          identifier = email
          this.password = password
        }
        .onSuccess { _uiState.value = SignInUiState.SignedIn }
        .onFailure { Log.e("SignInViewModel", "${it.errorMessage}", it.throwable) }
    }
  }
}

sealed interface SignInUiState {
  data object SignedOut : SignInUiState

  data object SignedIn : SignInUiState
}
