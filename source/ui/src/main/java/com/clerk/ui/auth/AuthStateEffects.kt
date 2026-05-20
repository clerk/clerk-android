package com.clerk.ui.auth

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import com.clerk.ui.R

@Composable
internal fun AuthStateEffects(
  authState: AuthState,
  state: AuthenticationViewState,
  snackbarHostState: SnackbarHostState,
  onAuthComplete: () -> Unit,
  onReset: () -> Unit = {},
) {
  val defaultErrorMessage = stringResource(R.string.something_went_wrong_please_try_again)
  LaunchedEffect(state, defaultErrorMessage) {
    when (state) {
      is AuthenticationViewState.Error -> {
        val msg = state.message ?: defaultErrorMessage
        snackbarHostState.showSnackbar(msg)
      }
      AuthenticationViewState.NotStarted -> authState.clearBackStack()
      is AuthenticationViewState.Success.SignIn -> {
        authState.setToStepForStatus(state.signIn, onAuthComplete = onAuthComplete)
        onReset()
      }
      is AuthenticationViewState.Success.SignUp -> {
        authState.setToStepForStatus(state.signUp, onAuthComplete = onAuthComplete)
        onReset()
      }
      is AuthenticationViewState.Success.SessionTaskComplete -> {
        authState.handleSessionTaskCompletion(state.session, onAuthComplete)
        onReset()
      }
      else -> Unit
    }
  }
}
