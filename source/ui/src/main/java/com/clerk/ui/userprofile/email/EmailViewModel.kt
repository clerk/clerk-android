package com.clerk.ui.userprofile.email

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.*
import com.clerk.ui.core.common.displayMessage
import com.clerk.ui.core.common.runUiOperation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val EMAIL_IMMUTABLE_MESSAGE =
  "Email addresses cannot be changed for this application."

internal class EmailViewModel(private val clerk: Clerk) : ViewModel() {

  private val _state = MutableStateFlow<State>(State.Idle)
  val state = _state.asStateFlow()

  fun setAsPrimary(emailAddress: EmailAddress) {
    if (clerk.environment.userSettings.attributes["email_address"]?.immutable == true) {
      _state.value = State.Failure(EMAIL_IMMUTABLE_MESSAGE)
      return
    }
    _state.value = State.Loading
    viewModelScope.launch {
      runUiOperation {
          val user = clerk.user ?: throw CoreException("no_user", "No current user found")
          user.update(UpdateUserParams(primaryEmailAddressId = Field.Value(emailAddress.id)))
        }
        .onSuccess { _state.value = State.SetAsPrimary.Success }
        .onFailure { _state.value = State.Failure(it.displayMessage) }
    }
  }

  fun remove(emailAddress: EmailAddress) {
    if (clerk.environment.userSettings.attributes["email_address"]?.immutable == true) {
      _state.value = State.Failure(EMAIL_IMMUTABLE_MESSAGE)
      return
    }
    _state.value = State.Loading
    viewModelScope.launch {
      runUiOperation { emailAddress.destroy() }
        .onFailure { _state.value = State.Failure(it.displayMessage) }
        .onSuccess { _state.value = State.Remove.Success }
    }
  }

  sealed interface State {
    data object Idle : State

    data object Loading : State

    data class Failure(val message: String) : State

    sealed interface SetAsPrimary : State {
      data object Success : SetAsPrimary
    }

    sealed interface Remove : State {
      data object Success : Remove
    }
  }
}
