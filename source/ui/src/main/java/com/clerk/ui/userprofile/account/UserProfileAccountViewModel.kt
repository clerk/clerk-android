package com.clerk.ui.userprofile.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.*
import com.clerk.ui.core.common.displayMessage
import com.clerk.ui.core.common.runUiOperation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class UserProfileAccountViewModel(private val clerk: Clerk) : ViewModel() {

  private val _deleteAccountStateFlow =
    MutableStateFlow<DeleteAccountState>(DeleteAccountState.Idle)
  val deleteAccountStateFlow = _deleteAccountStateFlow.asStateFlow()

  fun signOut() {
    val sessionId = clerk.session?.id ?: return
    viewModelScope.launch {
      runUiOperation { clerk.signOut(MobileSignOutOptions(sessionId)) }
        .onFailure { _deleteAccountStateFlow.value = DeleteAccountState.Error(it.displayMessage) }
    }
  }

  fun deleteAccount() {
    _deleteAccountStateFlow.value = DeleteAccountState.Loading
    viewModelScope.launch {
      runUiOperation {
          val user = clerk.user ?: throw CoreException("no_user", "User not authenticated")
          user.delete()
        }
        .onSuccess { _deleteAccountStateFlow.value = DeleteAccountState.Success }
        .onFailure { _deleteAccountStateFlow.value = DeleteAccountState.Error(it.displayMessage) }
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
