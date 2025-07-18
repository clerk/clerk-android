package com.clerk.quickstart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.Clerk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MainViewModel : ViewModel() {
  private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
  val uiState = _uiState.asStateFlow()

  init {
    Clerk.isInitialized
      .onEach { isInitialized ->
        if (isInitialized) {
          if (Clerk.user != null) {
            _uiState.value = MainUiState.SignedIn
          } else {
            _uiState.value = MainUiState.SignedOut
          }
        }
      }
      .launchIn(viewModelScope)
  }
}

sealed interface MainUiState {
  data object Loading : MainUiState

  data object SignedIn : MainUiState

  data object SignedOut : MainUiState
}
