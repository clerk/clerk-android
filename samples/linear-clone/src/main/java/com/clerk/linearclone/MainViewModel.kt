package com.clerk.linearclone

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.Clerk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn

class MainViewModel : ViewModel() {

  private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
  val uiState = _uiState.asStateFlow()

  init {
    combine(Clerk.isInitialized, Clerk.sessionFlow) { isInitialized, session ->
        Log.d("MainViewModel", "isInitialized: $isInitialized")
        Log.d("MainViewModel", "session: $session")
        Log.d("MainViewModel", "user: ${Clerk.user.value}")
        _uiState.value =
          when {
            !isInitialized -> UiState.Loading
            session == null -> UiState.SignedOut
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
