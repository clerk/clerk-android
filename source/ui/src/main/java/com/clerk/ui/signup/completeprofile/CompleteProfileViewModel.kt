package com.clerk.ui.signup.completeprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.serialization.longErrorMessageOrNull
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signup.SignUp
import com.clerk.api.signup.update
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class CompleteProfileViewModel : ViewModel() {

  private val _state = MutableStateFlow<State>(State.Idle)
  val state = _state.asStateFlow()

  fun updateSignUp(fistName: String, lastName: String) {
    val inProgressSignUp = Clerk.signUp ?: return
    _state.value = State.Loading

    viewModelScope.launch {
      inProgressSignUp
        .update(SignUp.UpdateParams(firstName = fistName, lastName = lastName))
        .onSuccess { _state.value = State.Success }
        .onFailure {
          ClerkLog.e("Failed to update sign up: ${it.longErrorMessageOrNull}")
          _state.value = State.Error(it.longErrorMessageOrNull)
        }
    }
  }

  sealed interface State {
    data object Idle : State

    data object Loading : State

    data object Success : State

    data class Error(val message: String?) : State
  }
}
