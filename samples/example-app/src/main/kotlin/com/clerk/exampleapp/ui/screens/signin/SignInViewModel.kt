package com.clerk.exampleapp.ui.screens.signin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.Clerk
import com.clerk.network.model.environment.UserSettings
import com.clerk.network.serialization.longErrorMessageOrNull
import com.clerk.network.serialization.onFailure
import com.clerk.network.serialization.onSuccess
import com.clerk.signin.SignIn
import com.clerk.signin.attemptFirstFactor
import com.clerk.sso.OAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class SignInViewModel @Inject constructor() : ViewModel() {

  private val _state = MutableStateFlow<SignInState>(SignInState.SignedOut)
  val state = _state.asStateFlow()

  fun createSignIn(phoneNumber: String) {
    viewModelScope.launch(Dispatchers.IO) {
      SignIn.create(SignIn.CreateParams.Strategy.PhoneCode(phoneNumber))
        .onSuccess { withContext(Dispatchers.Main) { _state.value = SignInState.NeedsFirstFactor } }
        .onFailure {
          Log.e(
            "SignInViewModel",
            "Failed to create sign in: ${it.longErrorMessageOrNull}",
            it.throwable,
          )
          withContext(Dispatchers.Main) { _state.value = SignInState.Error }
        }
    }
  }

  fun verify(code: String) {
    val inProgressSignIn = Clerk.signIn ?: return
    viewModelScope.launch(Dispatchers.IO) {
      inProgressSignIn
        .attemptFirstFactor(SignIn.AttemptFirstFactorParams.PhoneCode(code))
        .onSuccess { withContext(Dispatchers.Main) { _state.value = SignInState.SignInSuccess } }
        .onFailure {
          Log.e(
            "SignInViewModel",
            "Failed to verify code: ${it.longErrorMessageOrNull}",
            it.throwable,
          )
          withContext(Dispatchers.Main) { _state.value = SignInState.Error }
        }
    }
  }

  fun authenticateWithRedirect(socialConfig: UserSettings.SocialConfig) {
    viewModelScope.launch(Dispatchers.IO) {
      SignIn.authenticateWithRedirect(
          SignIn.AuthenticateWithRedirectParams.OAuth(
            OAuthProvider.fromStrategy(socialConfig.strategy)
          )
        )
        .onSuccess { withContext(Dispatchers.Main) { _state.value = SignInState.SignInSuccess } }
        .onFailure {
          Log.e("SignInViewModel", "Failed to authenticate with redirect", it.throwable)
          withContext(Dispatchers.Main) { _state.value = SignInState.Error }
        }
    }
  }
}

sealed interface SignInState {
  data object SignedOut : SignInState

  data object SignInSuccess : SignInState

  data object NeedsFirstFactor : SignInState

  data object Error : SignInState
}
