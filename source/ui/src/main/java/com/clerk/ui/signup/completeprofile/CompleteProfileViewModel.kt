package com.clerk.ui.signup.completeprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signup.SignUp
import com.clerk.api.signup.update
import com.clerk.ui.auth.AuthenticationViewState
import com.clerk.ui.auth.guardSignUp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class CompleteProfileViewModel : ViewModel() {

  private val _state = MutableStateFlow<AuthenticationViewState>(AuthenticationViewState.Idle)
  val state = _state.asStateFlow()

  fun updateSignUp(fistName: String, lastName: String) =
    guardSignUp(_state) { signUp ->
      _state.value = AuthenticationViewState.Loading
      viewModelScope.launch(Dispatchers.IO) {
        signUp
          .update(SignUp.SignUpUpdateParams.Standard(firstName = fistName, lastName = lastName))
          .onSuccess {
            withContext(Dispatchers.Main) {
              _state.value = AuthenticationViewState.Success.SignUp(it)
            }
          }
          .onFailure {
            ClerkLog.e("Failed to update sign up: ${it.errorMessage}")
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
