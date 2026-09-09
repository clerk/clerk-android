package com.clerk.workbench

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MainViewModel(private val clerk: Clerk) : ViewModel() {

  private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
  val uiState = _uiState.asStateFlow()

  init {
    clerk.changes
      .onEach {
        val session = clerk.session
        _uiState.value =
          when {
            !clerk.loaded -> UiState.Loading
            session == null -> UiState.SignedOut
            session.currentTask != null ||
              session.status != SessionStatus.Active ||
              clerk.user == null -> UiState.SignedOut
            else -> UiState.SignedIn
          }
      }
      .launchIn(viewModelScope)
  }

  sealed interface UiState {
    data object SignedIn : UiState

    data object SignedOut : UiState

    data object Loading : UiState
  }
}
