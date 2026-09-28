package com.clerk.customflows.oauth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signin.SignIn
import com.clerk.api.signup.SignUp
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.sso.ResultType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

class OAuthViewModel : ViewModel() {
  private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
  val uiState = _uiState.asStateFlow()

  init {
    combine(Clerk.isInitialized, Clerk.userFlow) { isInitialized, user ->
        _uiState.value =
          when {
            !isInitialized -> UiState.Loading
            user != null -> UiState.Authenticated
            else -> UiState.SignedOut
          }
      }
      .launchIn(viewModelScope)
  }

  fun signInWithOAuth(provider: OAuthProvider) {
    viewModelScope.launch {
      Clerk.auth
        .signInWithOAuth(provider)
        .onSuccess {
          when (it.resultType) {
            ResultType.SIGN_IN -> {
              if (it.signIn?.status == SignIn.Status.COMPLETE) {
                _uiState.value = UiState.Authenticated
              }
            }
            ResultType.SIGN_UP -> {
              if (it.signUp?.status == SignUp.Status.COMPLETE) {
                _uiState.value = UiState.Authenticated
              }
            }

            ResultType.UNKNOWN -> {
              ClerkLog.e("Unknown result type after OAuth redirect")
            }
          }
        }
        .onFailure {
          Log.e("OAuthViewModel", it.errorMessage, it.throwable)
        }
    }
  }

  sealed interface UiState {
    data object Loading : UiState

    data object SignedOut : UiState

    data object Authenticated : UiState
  }
}
