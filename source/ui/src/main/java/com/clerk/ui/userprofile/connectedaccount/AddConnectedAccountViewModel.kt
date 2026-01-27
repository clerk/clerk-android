package com.clerk.ui.userprofile.connectedaccount

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.externalaccount.ExternalAccount
import com.clerk.api.externalaccount.delete
import com.clerk.api.externalaccount.reauthorize
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.flatMap
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signin.SignIn
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.sso.ResultType
import com.clerk.api.user.User
import com.clerk.api.user.createExternalAccount
import com.clerk.ui.core.common.guardUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class AddConnectedAccountViewModel : ViewModel() {

  private val _state = MutableStateFlow<State>(State.Idle)
  val state = _state.asStateFlow()

  fun connectExternalAccount(provider: OAuthProvider, activity: Activity) {
    _state.value = State.Loading
    guardUser(userDoesNotExist = { _state.value = State.Error("User does not exist") }) { user ->
      viewModelScope.launch {
        if (provider == OAuthProvider.GOOGLE && Clerk.isGoogleOneTapEnabled) {
          handleGoogleOneTap(activity)
        } else {

          user
            .createExternalAccount(User.CreateExternalAccountParams(provider = provider))
            .flatMap { it.reauthorize() }
            .onSuccess { _state.value = State.Success }
            .onFailure { _state.value = State.Error(it.errorMessage) }
        }
      }
    }
  }

  private suspend fun handleGoogleOneTap(activity: Activity) {
    SignIn.authenticateWithGoogleOneTap(activity)
      .onSuccess {
        withContext(Dispatchers.Main) {
          when (it.resultType) {
            ResultType.SIGN_IN -> _state.value = State.Success
            ResultType.SIGN_UP -> _state.value = State.Success
            ResultType.UNKNOWN -> _state.value = State.Error("Unknown result type")
          }
        }
      }
      .onFailure { withContext(Dispatchers.Main) { _state.value = State.Error(it.errorMessage) } }
  }

  fun removeConnectedAccount(externalAccount: ExternalAccount) {
    viewModelScope.launch {
      externalAccount
        .delete()
        .onSuccess { _state.value = State.ConnectedAccountRemoved }
        .onFailure {
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
