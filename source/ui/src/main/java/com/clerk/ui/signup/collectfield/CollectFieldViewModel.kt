package com.clerk.ui.signup.collectfield

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.*
import com.clerk.ui.auth.*
import com.clerk.ui.core.common.displayMessage
import com.clerk.ui.core.common.runUiOperation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class CollectFieldViewModel(private val clerk: Clerk) : ViewModel() {
  private val _state = MutableStateFlow<AuthenticationViewState>(AuthenticationViewState.Idle)
  val state = _state.asStateFlow()

  fun updateSignUp(
    collectField: CollectField,
    email: String,
    password: String,
    username: String,
    phone: String,
  ) =
    guardSignUp(clerk, _state) { signUp ->
      _state.value = AuthenticationViewState.Loading
      viewModelScope.launch {
        runUiOperation {
            when (collectField) {
              CollectField.Email -> signUp.update(SignUpUpdateParams(emailAddress = email))
              CollectField.Phone -> signUp.update(SignUpUpdateParams(phoneNumber = phone))
              CollectField.Username -> signUp.update(SignUpUpdateParams(username = username))
              CollectField.Password ->
                signUp.password(
                  SignUpPasswordParams.Case4(SignUpPasswordParamsCase4(password = password))
                )
            }
          }
          .onSuccess { _state.value = AuthenticationViewState.Success.SignUp(signUp) }
          .onFailure { _state.value = AuthenticationViewState.Error(it.displayMessage) }
      }
    }

  fun resetState() {
    _state.value = AuthenticationViewState.Idle
  }
}
