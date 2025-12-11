package com.clerk.quickstart

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
  private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
  val uiState = _uiState.asStateFlow()

  init {
    // Combine initialization state with user state to determine UI state
    combine(Clerk.isInitialized, Clerk.userFlow) { isInitialized, user ->
        when {
          !isInitialized -> MainUiState.Loading
          user != null -> MainUiState.SignedIn
          else -> MainUiState.SignedOut
        }
      }
      .onEach { state -> _uiState.value = state }
      .launchIn(viewModelScope)
  }

  fun signOut() {
    viewModelScope.launch {
      Clerk.signOut()
        .onSuccess { _uiState.value = MainUiState.SignedOut }
        .onFailure { Log.e("MainViewModel", "${it.errorMessage}", it.throwable) }
    }
  }
}

sealed interface MainUiState {
  data object Loading : MainUiState

  data object SignedIn : MainUiState

  data object SignedOut : MainUiState
}
