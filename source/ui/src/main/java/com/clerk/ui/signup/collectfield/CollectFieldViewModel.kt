package com.clerk.ui.signup.collectfield

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.serialization.longErrorMessageOrNull
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

internal class CollectFieldViewModel : ViewModel() {

  private val _state = MutableStateFlow<AuthenticationViewState>(AuthenticationViewState.Idle)
  val state = _state.asStateFlow()

  fun updateSignUp(
    collectField: CollectField,
    email: String,
    password: String,
    username: String,
    phone: String,
  ) {
    guardSignUp(_state) { inProgressSignUp ->
      _state.value = AuthenticationViewState.Loading

      viewModelScope.launch(Dispatchers.IO) {
        val signUp =
          when (collectField) {
            CollectField.Email -> inProgressSignUp.update(SignUp.UpdateParams(emailAddress = email))
            CollectField.Password ->
              inProgressSignUp.update(SignUp.UpdateParams(password = password))
            CollectField.Phone -> inProgressSignUp.update(SignUp.UpdateParams(phoneNumber = phone))
            CollectField.Username ->
              inProgressSignUp.update(SignUp.UpdateParams(username = username))
          }

        signUp
          .onSuccess {
            withContext(Dispatchers.Main) {
              _state.value = AuthenticationViewState.Success.SignUp(it)
            }
          }
          .onFailure {
            ClerkLog.e(
              "CollectFieldViewModel - updateSignUp - failed to update sign up: ${it.longErrorMessageOrNull}"
            )
            withContext(Dispatchers.Main) {
              _state.value = AuthenticationViewState.Error(it.longErrorMessageOrNull)
            }
          }
      }
    }
  }

  fun resetState() {
    _state.value = AuthenticationViewState.Idle
  }
}
