package com.clerk.ui.userprofile.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.user.delete
import com.clerk.ui.core.common.guardUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class UserProfileAccountViewModel : ViewModel() {

  private val _deleteAccountStateFlow =
    MutableStateFlow<DeleteAccountState>(DeleteAccountState.Idle)
  val deleteAccountStateFlow = _deleteAccountStateFlow.asStateFlow()

  fun signOut() {
    viewModelScope.launch { Clerk.signOut() }
  }

  fun deleteAccount() {
    _deleteAccountStateFlow.value = DeleteAccountState.Loading
    viewModelScope.launch {
      guardUser({
        ClerkLog.e("User not authenticated, cannot delete account.")
        _deleteAccountStateFlow.value = DeleteAccountState.Error("User not authenticated")
      }) { user ->
        viewModelScope.launch {
          user
            .delete()
            .onSuccess { _deleteAccountStateFlow.value = DeleteAccountState.Success }
            .onFailure {
              ClerkLog.e("Failed to delete user account: ${it.errorMessage}")
              _deleteAccountStateFlow.value = DeleteAccountState.Error(it.errorMessage)
            }
        }
      }
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
