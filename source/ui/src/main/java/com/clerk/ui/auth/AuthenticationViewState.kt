package com.clerk.ui.auth

import com.clerk.api.Clerk
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.session.Session
import com.clerk.api.signin.SignIn
import com.clerk.api.signup.SignUp
import com.clerk.api.sso.SSOCancellationException
import com.clerk.ui.signin.code.VerificationState
import kotlinx.coroutines.flow.MutableStateFlow

internal typealias ClerkSignIn = SignIn

internal typealias ClerkSignUp = SignUp

internal sealed interface AuthenticationViewState {
  data object NotStarted : AuthenticationViewState

  data object Idle : AuthenticationViewState

  data object Loading : AuthenticationViewState

  sealed interface Success : AuthenticationViewState {
    data class SignIn(val signIn: ClerkSignIn) : Success

    data class SignUp(val signUp: ClerkSignUp) : Success

    data class SessionTaskComplete(val session: Session?) : Success
  }

  data class Error(val message: String?) : AuthenticationViewState
}

internal val ClerkResult.Failure<*>.isSSOCancellation: Boolean
  get() = throwable is SSOCancellationException

internal sealed interface VerificationUiState {
  data object Idle : VerificationUiState

  data object Verifying : VerificationUiState

  data object Verified : VerificationUiState

  data object IncorrectCode : VerificationUiState
}

internal fun VerificationUiState.verificationState(): VerificationState {
  return when (this) {
    VerificationUiState.IncorrectCode -> VerificationState.Error
    VerificationUiState.Idle -> VerificationState.Default
    is VerificationUiState.Verified -> VerificationState.Success
    VerificationUiState.Verifying -> VerificationState.Verifying
  }
}

private const val FORM_CODE_INCORRECT = "form_code_incorrect"

internal fun reportFailedCodeAttempt(
  failure: ClerkResult.Failure<ClerkErrorResponse>,
  verificationState: MutableStateFlow<VerificationUiState>,
  state: MutableStateFlow<AuthenticationViewState>,
) {
  if (failure.error?.errors.orEmpty().any { it.code == FORM_CODE_INCORRECT }) {
    verificationState.value = VerificationUiState.IncorrectCode
    state.value = AuthenticationViewState.Idle
  } else {
    verificationState.value = VerificationUiState.Idle
    state.value = AuthenticationViewState.Error(failure.errorMessage)
  }
}

internal fun guardSignIn(
  state: MutableStateFlow<AuthenticationViewState>,
  block: (ClerkSignIn) -> Unit,
) {
  val s = Clerk.auth.currentSignIn
  if (s == null) {
    state.value = AuthenticationViewState.NotStarted
    null
  } else {
    block(s)
  }
}

internal fun guardSignUp(
  state: MutableStateFlow<AuthenticationViewState>,
  block: (ClerkSignUp) -> Unit,
) {
  val s = Clerk.auth.currentSignUp
  if (s == null) {
    state.value = AuthenticationViewState.NotStarted
    null
  } else {
    block(s)
  }
}
