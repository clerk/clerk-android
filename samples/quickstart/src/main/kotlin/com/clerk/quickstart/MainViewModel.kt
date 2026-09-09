package com.clerk.quickstart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.CoreException
import com.clerk.api.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class MainViewModel(private val clerk: Clerk) : ViewModel() {
  private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
  val uiState = _uiState.asStateFlow()
  private val _error = MutableStateFlow<String?>(null)
  val error = _error.asStateFlow()

  init {
    clerk.changes
      .onEach { state ->
        val session = state.session
        _uiState.value =
          when {
            session == null -> MainUiState.SignedOut
            session.currentTask != null ->
              MainUiState.PendingTask(session.currentTask!!.key.rawValue)
            session.status == SessionStatus.Active -> MainUiState.SignedIn
            else -> MainUiState.PendingTask("Session is not active")
          }
      }
      .launchIn(viewModelScope)
  }

  fun signOut() {
    viewModelScope.launch {
      try {
        clerk.signOut()
        _error.value = null
      } catch (error: CoreException) {
        _error.value = error.message
      }
    }
  }
}

sealed interface MainUiState {
  data object Loading : MainUiState

  data object SignedIn : MainUiState

  data object SignedOut : MainUiState

  data class PendingTask(val task: String) : MainUiState
}
