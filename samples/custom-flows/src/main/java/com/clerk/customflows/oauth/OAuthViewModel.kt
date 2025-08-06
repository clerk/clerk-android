package com.clerk.customflows.oauth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.longErrorMessageOrNull
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
      SignIn.authenticateWithRedirect(SignIn.AuthenticateWithRedirectParams.OAuth(provider))
        .onSuccess {
          when (it.resultType) {
            ResultType.SIGN_IN -> {
              // The OAuth flow resulted in a sign in
              if (it.signIn?.status == SignIn.Status.COMPLETE) {
                _uiState.value = UiState.Authenticated
              } else {
                // If the status is not complete, check why. User may need to
                // complete further steps.
              }
            }
            ResultType.SIGN_UP -> {
              // The OAuth flow resulted in a sign up
              if (it.signUp?.status == SignUp.Status.COMPLETE) {
                _uiState.value = UiState.Authenticated
              } else {
                // If the status is not complete, check why. User may need to
                // complete further steps.
              }
            }
          }
        }
        .onFailure {
          // See https://clerk.com/docs/custom-flows/error-handling
          // for more info on error handling
          Log.e("OAuthViewModel", it.longErrorMessageOrNull, it.throwable)
        }
    }
  }

  sealed interface UiState {
    data object Loading : UiState

    data object SignedOut : UiState

    data object Authenticated : UiState
  }
}
