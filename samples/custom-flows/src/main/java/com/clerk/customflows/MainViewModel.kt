package com.clerk.customflows

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.Clerk
import com.clerk.network.serialization.onFailure
import com.clerk.network.serialization.onSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

  private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
  val uiState = _uiState.asStateFlow()

  init {
    combine(Clerk.isInitialized, Clerk.user) { isInitialized, user ->
        Log.e("QQQ", "isInitialized: $isInitialized, user: $user")
        _uiState.value =
          when {
            !isInitialized -> UiState.Loading
            user != null -> UiState.SignedIn
            else -> UiState.SignedOut
          }
      }
      .launchIn(viewModelScope)
  }

  fun signOut() {
    viewModelScope.launch {
      Clerk.signOut()
        .onSuccess { _uiState.value = UiState.SignedOut }
        .onFailure {
          // See https://clerk.com/docs/custom-flows/error-handling
          // for more info on error handling
        }
    }
  }

  sealed interface UiState {
    data object SignedIn : UiState

    data object SignedOut : UiState

    data object Loading : UiState
  }
}
