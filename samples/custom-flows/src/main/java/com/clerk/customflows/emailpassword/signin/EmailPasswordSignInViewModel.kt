package com.clerk.customflows.emailpassword.signin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

class EmailPasswordSignInViewModel : ViewModel() {
  private val _uiState =
    MutableStateFlow<EmailPasswordSignInUiState>(EmailPasswordSignInUiState.Loading)
  val uiState = _uiState.asStateFlow()

  init {
    combine(Clerk.userFlow, Clerk.isInitialized) { user, isInitialized ->
        Log.e("EmailPasswordSignInViewModel", "combine: $user, $isInitialized")
        _uiState.value =
          when {
            !isInitialized -> EmailPasswordSignInUiState.Loading
            user == null -> EmailPasswordSignInUiState.SignedOut
            else -> EmailPasswordSignInUiState.SignedIn
          }
      }
      .launchIn(viewModelScope)
  }

  fun submit(email: String, password: String) {
    viewModelScope.launch {
      Clerk.auth
        .signInWithPassword {
          identifier = email
          this.password = password
        }
        .onSuccess { _uiState.value = EmailPasswordSignInUiState.SignedIn }
        .onFailure {
          // See https://clerk.com/docs/custom-flows/error-handling
          // for more info on error handling
        }
    }
  }

  sealed interface EmailPasswordSignInUiState {
    data object Loading : EmailPasswordSignInUiState

    data object SignedOut : EmailPasswordSignInUiState

    data object SignedIn : EmailPasswordSignInUiState
  }
}
