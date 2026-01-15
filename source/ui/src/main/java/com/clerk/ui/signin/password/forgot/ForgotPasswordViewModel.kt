package com.clerk.ui.signin.password.forgot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.resetPasswordFactor
import com.clerk.api.signin.SignIn
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.sso.ResultType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class ForgotPasswordViewModel : ViewModel() {

  private val _state = MutableStateFlow<ResetPasswordViewState>(ResetPasswordViewState.Idle)
  val state = _state.asStateFlow()

  fun signInWithProvider(provider: OAuthProvider) {
    _state.value = ResetPasswordViewState.Loading
    viewModelScope.launch(Dispatchers.IO) {
      SignIn.authenticateWithRedirect(SignIn.AuthenticateWithRedirectParams.OAuth(provider))
        .onSuccess {
          withContext(Dispatchers.Main) {
            if (it.resultType == ResultType.SIGN_IN) {
              _state.value = ResetPasswordViewState.Success.SignIn(it.signIn!!)
            } else {
              _state.value = ResetPasswordViewState.Success.SignUp(it.signUp!!)
            }
          }
        }
        .onFailure {
          withContext(Dispatchers.Main) {
            _state.value = ResetPasswordViewState.Error(it.errorMessage)
          }
        }
    }
  }

  fun resetPassword() {
    _state.value = ResetPasswordViewState.Loading
    val signIn = Clerk.auth.currentSignIn
    val resetPasswordFactor = Clerk.auth.currentSignIn?.resetPasswordFactor
    if (signIn == null || resetPasswordFactor == null) {
      _state.value = ResetPasswordViewState.NotStarted
      return
    } else {
      _state.value = ResetPasswordViewState.ResetFactor(resetPasswordFactor)
    }
  }

  fun resetState() {
    _state.value = ResetPasswordViewState.Idle
  }
}

/**
 * Represents the various states of an authentication process.
 *
 * This sealed interface is used to model the different stages that an authentication flow can be
 * in, such as not started, idle, loading, successful, or encountering an error.
 */
internal sealed interface ResetPasswordViewState {
  data object NotStarted : ResetPasswordViewState

  data object Idle : ResetPasswordViewState

  data object Loading : ResetPasswordViewState

  sealed interface Success : ResetPasswordViewState {
    data class SignIn(val signIn: com.clerk.api.signin.SignIn) : Success

    data class SignUp(val signUp: com.clerk.api.signup.SignUp) : Success
  }

  data class Error(val message: String?) : ResetPasswordViewState

  data class ResetFactor(val factor: Factor) : ResetPasswordViewState
}
