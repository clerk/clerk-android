package com.clerk.exampleapp.ui.screens.signin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.Clerk
import com.clerk.network.model.environment.UserSettings
import com.clerk.network.serialization.flatMap
import com.clerk.network.serialization.longErrorMessageOrNull
import com.clerk.network.serialization.onFailure
import com.clerk.network.serialization.onSuccess
import com.clerk.signin.SignIn
import com.clerk.signin.attemptFirstFactor
import com.clerk.signin.prepareFirstFactor
import com.clerk.signup.SignUp
import com.clerk.signup.attemptVerification
import com.clerk.signup.prepareVerification
import com.clerk.sso.OAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class SignInOrUpViewModel @Inject constructor() : ViewModel() {

  private val _state = MutableStateFlow<SignInOrUpState>(SignInOrUpState.SignedOut)
  val state = _state.asStateFlow()

  var isSignUp: Boolean = false

  fun handlePhoneNumber(phoneNumber: String, isSignUp: Boolean) {
    Log.d("SignInOrUpViewModel", "handlePhoneNumber: $phoneNumber, isSignUp: $isSignUp")
    this.isSignUp = isSignUp
    if (isSignUp) {
      createSignUp(phoneNumber)
    } else {
      createSignIn(phoneNumber)
    }
  }

  private fun createSignUp(phoneNumber: String) {
    viewModelScope.launch(Dispatchers.IO) {
      SignUp.Companion.create(SignUp.CreateParams.Standard(phoneNumber = phoneNumber)).flatMap {
        it
          .prepareVerification(SignUp.PrepareVerificationParams.Strategy.PhoneCode())
          .onSuccess {
            withContext(Dispatchers.Main) { _state.value = SignInOrUpState.NeedsFirstFactor }
          }
          .onFailure { error ->
            withContext(Dispatchers.Main) {
              Log.e(
                "SignUpViewModel",
                "Failed to prepare verification: ${error.longErrorMessageOrNull}",
                error.throwable,
              )
              _state.value = SignInOrUpState.Error
            }
          }
      }
    }
  }

  private fun createSignIn(phoneNumber: String) {
    viewModelScope.launch(Dispatchers.IO) {
      SignIn.create(SignIn.CreateParams.Strategy.PhoneCode(phoneNumber))
        .flatMap { it.prepareFirstFactor(SignIn.PrepareFirstFactorParams.PhoneCode()) }
        .onSuccess {
          withContext(Dispatchers.Main) { _state.value = SignInOrUpState.NeedsFirstFactor }
        }
        .onFailure {
          Log.e(
            "SignInViewModel",
            "Failed to create sign in: ${it.longErrorMessageOrNull}",
            it.throwable,
          )
          withContext(Dispatchers.Main) { _state.value = SignInOrUpState.Error }
        }
    }
  }

  fun verify(code: String) {
    if (isSignUp) {
      verifySignUp(code)
    } else {
      verifySignIn(code)
    }
  }

  private fun verifySignIn(code: String) {
    val inProgressSignIn = Clerk.signIn ?: return
    viewModelScope.launch(Dispatchers.IO) {
      inProgressSignIn
        .attemptFirstFactor(SignIn.AttemptFirstFactorParams.PhoneCode(code))
        .onSuccess { withContext(Dispatchers.Main) { _state.value = SignInOrUpState.Success } }
        .onFailure {
          Log.e("SignInViewModel", "${it.longErrorMessageOrNull}", it.throwable)
          withContext(Dispatchers.Main) { _state.value = SignInOrUpState.Error }
        }
    }
  }

  private fun verifySignUp(code: String) {
    // Grab in progress sign up
    val inProgressSignUp = Clerk.signUp ?: return
    viewModelScope.launch(Dispatchers.IO) {
      inProgressSignUp
        .attemptVerification(SignUp.AttemptVerificationParams.PhoneCode(code))
        .onSuccess {
          withContext(Dispatchers.Main) {
            if (it.status == SignUp.Status.COMPLETE) {
              _state.value = SignInOrUpState.Success
            }
          }
        }
        .onFailure {
          withContext(Dispatchers.Main) {
            Log.e("SignUpViewModel", "${it.longErrorMessageOrNull}", it.throwable)
            _state.value = SignInOrUpState.Error
          }
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
        .onSuccess { withContext(Dispatchers.Main) { _state.value = SignInOrUpState.Success } }
        .onFailure {
          Log.e("SignInViewModel", "Failed to authenticate with redirect", it.throwable)
          withContext(Dispatchers.Main) { _state.value = SignInOrUpState.Error }
        }
    }
  }
}

sealed interface SignInOrUpState {
  data object SignedOut : SignInOrUpState

  data object Success : SignInOrUpState

  data object NeedsFirstFactor : SignInOrUpState

  data object Error : SignInOrUpState
}
