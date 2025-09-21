package com.clerk.ui.signup.collectfield

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.serialization.longErrorMessageOrNull
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signup.SignUp
import com.clerk.api.signup.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class CollectFieldViewModel : ViewModel() {

  private val _state = MutableStateFlow<State>(State.Idle)
  val state = _state.asStateFlow()

  fun updateSignUp(
    collectField: CollectField,
    email: String,
    password: String,
    username: String,
    phone: String,
  ) {
    _state.value = State.Loading
    val inProgressSignUp = Clerk.signUp ?: return

    viewModelScope.launch(Dispatchers.IO) {
      val signUp =
        when (collectField) {
          CollectField.Email -> inProgressSignUp.update(SignUp.UpdateParams(emailAddress = email))
          CollectField.Password -> inProgressSignUp.update(SignUp.UpdateParams(password = password))
          CollectField.Phone -> inProgressSignUp.update(SignUp.UpdateParams(phoneNumber = phone))
          CollectField.Username -> inProgressSignUp.update(SignUp.UpdateParams(username = username))
        }

      signUp
        .onSuccess { _state.value = State.Success }
        .onFailure {
          ClerkLog.e(
            "CollectFieldViewModel - updateSignUp - failed to update sign up: ${it.longErrorMessageOrNull}"
          )
          _state.value = State.Error(it.longErrorMessageOrNull)
        }
    }
  }

  internal sealed interface State {
    data object Idle : State

    data object Loading : State

    data object Success : State

    data class Error(val message: String?) : State
  }
}
