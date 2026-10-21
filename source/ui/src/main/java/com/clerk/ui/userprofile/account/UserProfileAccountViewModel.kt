package com.clerk.ui.userprofile.account

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

internal class UserProfileAccountViewModel(
  private val workDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

  private val _deleteAccountStateFlow =
    MutableStateFlow<DeleteAccountState>(DeleteAccountState.Idle)
  val deleteAccountStateFlow = _deleteAccountStateFlow.asStateFlow()

  fun signOut() {
    val sessionId = Clerk.session?.id ?: return
    viewModelScope.launch { Clerk.auth.signOut(sessionId = sessionId) }
  }

  fun deleteAccount() {
    _deleteAccountStateFlow.value = DeleteAccountState.Loading
    viewModelScope.launch {
      guardUser({
        ClerkLog.e("User not authenticated, cannot delete account.")
        _deleteAccountStateFlow.value = DeleteAccountState.Error("User not authenticated")
      }) { user ->
        viewModelScope.launch {
          val deletedUserId = user.id
          user
            .delete()
            .onSuccess {
              forgetBiometricLocalCredentials(deletedUserId)
              _deleteAccountStateFlow.value = DeleteAccountState.Success
            }
            .onFailure {
              ClerkLog.e("Failed to delete user account: ${it.errorMessage}")
              _deleteAccountStateFlow.value = DeleteAccountState.Error(it.errorMessage)
            }
        }
      }
    }
  }

  private suspend fun forgetBiometricLocalCredentials(deletedUserId: String) {
    withContext(workDispatcher) { forgetLocalCredentialsIgnoringFailure(deletedUserId) }
  }

  private fun forgetLocalCredentialsIgnoringFailure(deletedUserId: String) {
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

  fun resetState() {
    _deleteAccountStateFlow.value = DeleteAccountState.Idle
  }

  sealed interface DeleteAccountState {
    data object Idle : DeleteAccountState

    data object Loading : DeleteAccountState

    data object Success : DeleteAccountState

    data class Error(val message: String?) : DeleteAccountState
  }
}
