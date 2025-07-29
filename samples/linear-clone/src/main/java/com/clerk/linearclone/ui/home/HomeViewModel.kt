package com.clerk.linearclone.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.Clerk
import com.clerk.network.serialization.onFailure
import com.clerk.network.serialization.onSuccess
import com.clerk.user.createPasskey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel : ViewModel() {

  private val _uiState = MutableStateFlow<UiState>(UiState.SignedIn)

  val uiState = _uiState.asStateFlow()

  fun signOut() {
    viewModelScope.launch(Dispatchers.IO) { Clerk.signOut() }
  }

  fun createPasskey() {
    val currentUser = Clerk.user.value ?: return
    viewModelScope.launch(Dispatchers.IO) {
      currentUser
        .createPasskey()
        .onSuccess { withContext(Dispatchers.Main) { _uiState.value = UiState.PasskeySuccess } }
        .onFailure { withContext(Dispatchers.Main) { _uiState.value = UiState.PasskeyFailure } }
    }
  }

  sealed interface UiState {
    data object SignedIn : UiState

    data object PasskeySuccess : UiState

    data object PasskeyFailure : UiState
  }
}
