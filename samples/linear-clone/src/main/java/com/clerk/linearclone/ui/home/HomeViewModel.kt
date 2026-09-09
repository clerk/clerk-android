package com.clerk.linearclone.ui.home

import com.clerk.api.Clerk
import com.clerk.linearclone.LinearFeedback
import com.clerk.linearclone.LinearViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel(clerk: Clerk, feedback: LinearFeedback) : LinearViewModel(clerk, feedback) {
  private val _uiState = MutableStateFlow<UiState>(UiState.SignedIn())
  val uiState = _uiState.asStateFlow()

  fun signOut() = runOperation { clerk.signOut() }

  fun createPasskey() = runOperation {
    val user = clerk.user ?: error("Sign in before creating a passkey")
    user.createPasskey()
    _uiState.value = UiState.SignedIn(PasskeyResult.Success)
  }

  sealed interface UiState {
    data class SignedIn(val passkeyResult: PasskeyResult? = null) : UiState
  }
}

sealed interface PasskeyResult {
  data object Success : PasskeyResult

  data object Failure : PasskeyResult
}
