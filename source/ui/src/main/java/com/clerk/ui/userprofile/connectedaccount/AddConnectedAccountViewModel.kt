package com.clerk.ui.userprofile.connectedaccount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.externalaccount.ExternalAccount
import com.clerk.api.externalaccount.delete
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.user.User
import com.clerk.api.user.createExternalAccount
import com.clerk.ui.core.common.guardUser
import com.clerk.ui.core.log.ClerkLog
import com.clerk.ui.reverification.isReverificationCancelled
import com.clerk.ui.reverification.reverify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class AddConnectedAccountViewModel : ViewModel() {

  private val _state = MutableStateFlow<State>(State.Idle)
  val state = _state.asStateFlow()

  fun connectExternalAccount(provider: OAuthProvider) {
    _state.value = State.Loading
    guardUser(userDoesNotExist = { _state.value = State.Error("User does not exist") }) { user ->
      viewModelScope.launch {
        Clerk.reverify {
            user.createExternalAccount(User.CreateExternalAccountParams(provider = provider))
          }
          .onSuccess { _state.value = State.Success }
          .onFailure {
            _state.value =
              if (it.isReverificationCancelled) State.Idle else State.Error(it.errorMessage)
          }
      }
    }
  }

  fun removeConnectedAccount(externalAccount: ExternalAccount) {
    viewModelScope.launch {
      Clerk.reverify { externalAccount.delete() }
        .onSuccess { _state.value = State.ConnectedAccountRemoved }
        .onFailure {
          if (it.isReverificationCancelled) return@onFailure
          ClerkLog.e("Failed to remove connected account: ${it.errorMessage}")
          _state.value = State.Error(it.errorMessage)
        }
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
