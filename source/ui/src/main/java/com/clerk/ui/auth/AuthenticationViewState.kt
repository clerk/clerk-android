package com.clerk.ui.auth

import com.clerk.api.Clerk
import com.clerk.api.signin.SignIn
import com.clerk.api.signup.SignUp
import com.clerk.ui.signin.code.VerificationState
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Represents the various states of an authentication process.
 *
 * This sealed interface is used to model the different stages that an authentication flow can be
 * in, such as not started, idle, loading, successful, or encountering an error.
 */
internal typealias ClerkSignIn = SignIn

internal typealias ClerkSignUp = SignUp

internal sealed interface AuthenticationViewState {
  data object NotStarted : AuthenticationViewState

  data object Idle : AuthenticationViewState

  data object Loading : AuthenticationViewState

  sealed interface Success : AuthenticationViewState {
    data class SignIn(val signIn: ClerkSignIn) : Success

    data class SignUp(val signUp: ClerkSignUp) : Success
  }

  data class Error(val message: String?) : AuthenticationViewState
}

/** States for the code verification text, since it's not 1:1 with the view model states. */
internal sealed interface VerificationUiState {
  data object Idle : VerificationUiState

  data object Verifying : VerificationUiState

  data object Verified : VerificationUiState

  data class Error(val message: String?) : VerificationUiState
}

/**
 * Extension function that converts a [AuthenticationViewState] to a [VerificationUiState].
 *
 * This mapping provides a UI-focused state representation that can be used by input components to
 * determine their visual appearance and behavior.
 *
 * @return The corresponding [VerificationUiState] for the current view model state
 */
internal fun VerificationUiState.verificationState(): VerificationState {
  return when (this) {
    is VerificationUiState.Error -> VerificationState.Error
    VerificationUiState.Idle -> VerificationState.Default
    is VerificationUiState.Verified -> VerificationState.Success
    VerificationUiState.Verifying -> VerificationState.Verifying
  }
}

/**
 * Ensures that a sign-in process is active before executing a given block of code.
 *
 * This function checks if a [ClerkSignIn] object exists. If not, it sets the provided [state] to
 * [AuthenticationViewState.NotStarted] and does nothing further. Otherwise, it executes the
 * provided [block] with the active [ClerkSignIn] object.
 *
 * This is useful for guarding operations that require an active sign-in flow, preventing errors or
 * unexpected behavior if a sign-in hasn't been initiated.
 *
 * @param state The [MutableStateFlow] representing the current authentication view state.
 * @param block A function that will be executed if a [ClerkSignIn] is available. It receives the
 *   active [ClerkSignIn] object as a parameter.
 */
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

/**
 * Ensures that a sign-up process is active before executing a given block of code.
 *
 * This function checks if a [ClerkSignUp] object exists. If not, it sets the provided [state] to
 * [AuthenticationViewState.NotStarted] and does nothing further. Otherwise, it executes the
 * provided [block] with the active [ClerkSignUp] object.
 *
 * This is useful for guarding operations that require an active sign-up flow, preventing errors or
 * unexpected behavior if a sign-up hasn't been initiated.
 *
 * @param state The [MutableStateFlow] representing the current authentication view state.
 * @param block A function that will be executed if a [ClerkSignUp] is available. It receives the
 *   active [ClerkSignUp] object as a parameter.
 */
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
