package com.clerk.exampleapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.Clerk
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {

  private val _authenticationState =
    MutableStateFlow<AuthenticationState>(AuthenticationState.Loading)
  val uiState = _authenticationState.asStateFlow()

  init {
    Clerk.isInitialized
      .onEach { isInitialized ->
        if (isInitialized) {
          if (Clerk.session != null) {
            _authenticationState.value = AuthenticationState.SignedIn
          } else {
            _authenticationState.value = AuthenticationState.SignedOut
          }
        }
      }
      .launchIn(viewModelScope)
  }
}

sealed interface AuthenticationState {
  object Loading : AuthenticationState

  object SignedIn : AuthenticationState

  object SignedOut : AuthenticationState
}
