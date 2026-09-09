package com.clerk.ui.userprofile.connectedaccount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.*
import com.clerk.ui.core.common.displayMessage
import com.clerk.ui.core.common.runUiOperation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive

internal class AddConnectedAccountViewModel(private val clerk: Clerk) : ViewModel() {

  private val _state = MutableStateFlow<State>(State.Idle)
  val state = _state.asStateFlow()

  fun connectExternalAccount(provider: OAuthProvider) {
    _state.value = State.Loading
    viewModelScope.launch {
      runUiOperation {
          val user = clerk.user ?: throw CoreException("no_user", "User does not exist")
          val strategy =
            CreateExternalAccountParamsStrategy.fromJson(
              JsonPrimitive("oauth_${provider.rawValue}"),
              clerk.context.requireRuntime(),
            )
          user.createExternalAccount(CreateExternalAccountParams(strategy = strategy))
        }
        .onSuccess { _state.value = State.Success }
        .onFailure { _state.value = State.Error(it.displayMessage) }
    }
  }

  fun removeConnectedAccount(externalAccount: ExternalAccount) {
    _state.value = State.Loading
    viewModelScope.launch {
      runUiOperation { externalAccount.destroy() }
        .onSuccess { _state.value = State.ConnectedAccountRemoved }
        .onFailure { _state.value = State.Error(it.displayMessage) }
    }
  }

  fun resetState() {
    _state.value = State.Idle
  }

  sealed interface State {
    data object Idle : State

    data object Loading : State

    data object Success : State

    data class Error(val message: String?) : State

    data object ConnectedAccountRemoved : State
  }
}
