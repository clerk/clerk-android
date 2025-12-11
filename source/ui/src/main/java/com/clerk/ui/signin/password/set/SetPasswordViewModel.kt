package com.clerk.ui.signin.password.set

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.attemptFirstFactor
import com.clerk.ui.auth.AuthenticationViewState
import com.clerk.ui.auth.guardSignIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class SetPasswordViewModel : ViewModel() {

  private val _state = MutableStateFlow<AuthenticationViewState>(AuthenticationViewState.Idle)
  val state = _state.asStateFlow()

  fun submitPassword(password: String) =
    guardSignIn(state = _state) { signIn ->
      _state.value = AuthenticationViewState.Loading
      viewModelScope.launch(Dispatchers.IO) {
        signIn
          .attemptFirstFactor(SignIn.AttemptFirstFactorParams.Password(password))
          .onSuccess {
            withContext(Dispatchers.Main) {
              _state.value = AuthenticationViewState.Success.SignIn(it)
            }
          }
          .onFailure {
            withContext(Dispatchers.Main) {
              _state.value = AuthenticationViewState.Error(it.errorMessage)
            }
          }
      }
    }

  fun resetState() {
    _state.value = AuthenticationViewState.Idle
  }
}
