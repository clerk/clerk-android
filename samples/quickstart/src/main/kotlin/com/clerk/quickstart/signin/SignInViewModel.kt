package com.clerk.quickstart.signin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.network.serialization.longErrorMessageOrNull
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signin.SignIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SignInViewModel : ViewModel() {
  private val _uiState = MutableStateFlow<SignInUiState>(SignInUiState.SignedOut)
  val uiState = _uiState.asStateFlow()

  fun signIn(email: String, password: String) {
    viewModelScope.launch {
      SignIn.create(SignIn.CreateParams.Strategy.Password(identifier = email, password = password))
        .onSuccess { _uiState.value = SignInUiState.SignedIn }
        .onFailure { Log.e("SignInViewModel", "${it.longErrorMessageOrNull}", it.throwable) }
    }
  }
}

sealed interface SignInUiState {
  data object SignedOut : SignInUiState

  data object SignedIn : SignInUiState
}
