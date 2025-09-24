package com.clerk.ui.signup.code

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.longErrorMessageOrNull
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signup.SignUp
import com.clerk.api.signup.attemptVerification
import com.clerk.api.signup.prepareVerification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class SignUpCodeViewModel : ViewModel() {

  private val _state = MutableStateFlow<AuthenticationState>(AuthenticationState.Idle)
  val state = _state.asStateFlow()

  fun prepare(field: SignUpCodeField) {
    _state.value = AuthenticationState.Loading
    val signUp = Clerk.client.signUp ?: return
    viewModelScope.launch {
      val signUp =
        when (field) {
          is SignUpCodeField.Email -> {
            signUp.prepareVerification(SignUp.PrepareVerificationParams.Strategy.EmailCode())
          }
          is SignUpCodeField.Phone -> {
            signUp.prepareVerification(SignUp.PrepareVerificationParams.Strategy.PhoneCode())
          }
        }
      signUp
        .onSuccess { _state.value = AuthenticationState.CodeSent }
        .onFailure { _state.value = AuthenticationState.Error(it.longErrorMessageOrNull) }
    }
  }

  fun attempt(code: String, field: SignUpCodeField) {
    val signUp = Clerk.client.signUp ?: return
    viewModelScope.launch {
      val signUp =
        when (field) {
          is SignUpCodeField.Email ->
            signUp.attemptVerification(SignUp.AttemptVerificationParams.EmailCode(code))
          is SignUpCodeField.Phone ->
            signUp.attemptVerification(SignUp.AttemptVerificationParams.PhoneCode(code))
        }
      signUp
        .onSuccess { _state.value = AuthenticationState.CodeSent }
        .onFailure { _state.value = AuthenticationState.Error(it.longErrorMessageOrNull) }
    }
  }

  internal sealed interface AuthenticationState {
    object Idle : AuthenticationState

    object Loading : AuthenticationState

    object CodeSent : AuthenticationState

    data class Error(val message: String?) : AuthenticationState
  }
}
