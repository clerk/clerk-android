package com.clerk.ui.userprofile.email

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.*
import com.clerk.ui.core.common.displayMessage
import com.clerk.ui.core.common.runUiOperation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val ADD_EMAIL_IMMUTABLE_MESSAGE =
  "Email addresses cannot be changed for this application."

internal class AddEmailViewModel(private val clerk: Clerk) : ViewModel() {

  private val _state = MutableStateFlow<State>(State.Idle)
  val state = _state.asStateFlow()

  fun addEmail(email: String) {
    if (clerk.environment.userSettings.attributes["email_address"]?.immutable == true) {
      _state.value = State.Error(ADD_EMAIL_IMMUTABLE_MESSAGE)
      return
    }
    _state.value = State.Loading
    viewModelScope.launch {
      runUiOperation {
          val user = clerk.user ?: throw CoreException("no_user", "No current user found")
          user.createEmailAddress(CreateEmailAddressParams(email))
        }
        .onSuccess { _state.value = State.Success(it) }
        .onFailure { _state.value = State.Error(it.displayMessage) }
    }
  }

  fun resetState() {
    _state.value = State.Idle
  }

  sealed interface State {
    data object Idle : State

    data object Loading : State

    data class Success(val emailAddress: EmailAddress) : State

    data class Error(val message: String?) : State
  }
}
