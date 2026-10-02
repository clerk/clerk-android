package com.clerk.ui.userprofile.security.delete

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.user.delete
import com.clerk.ui.core.common.guardUser
import com.clerk.ui.core.log.ClerkLog
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class DeleteAccountViewModel(
  private val workDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

  private val _state = MutableStateFlow<State>(State.Idle)
  val state = _state.asStateFlow()

  fun deleteAccount() {
    guardUser({}) { user ->
      _state.value = State.Loading
      viewModelScope.launch {
        val deletedUserId = user.id
        user
          .delete()
          .onFailure {
            ClerkLog.e("Failed to delete account: ${it.errorMessage}")
            _state.value = State.Error(it.errorMessage)
          }
          .onSuccess {
            forgetBiometricLocalCredentials(deletedUserId)
            _state.value = State.Success
          }
      }
    }
  }

  private suspend fun forgetBiometricLocalCredentials(deletedUserId: String) {
    withContext(workDispatcher) {
      runCatching {
        Clerk.biometricCredentials.forgetLocalCredentialsAfterAccountDeletion(deletedUserId)
      }
        .onFailure {
          ClerkLog.e(
            "Failed to delete biometric local credentials after account deletion. " +
              "This is non-critical."
          )
        }
    }
  }

  sealed interface State {
    object Idle : State

    object Loading : State

    object Success : State

    data class Error(val message: String) : State
  }
}
