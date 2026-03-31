package com.clerk.ui.auth

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.clerk.ui.R

@Composable
internal fun AuthStateEffects(
  authState: AuthState,
  state: AuthenticationViewState,
  snackbarHostState: SnackbarHostState,
  onAuthComplete: () -> Unit,
  onReset: () -> Unit = {},
) {
  val context = LocalContext.current
  LaunchedEffect(state) {
    when (state) {
      is AuthenticationViewState.Error -> {
        val msg = state.message ?: context.getString(R.string.something_went_wrong_please_try_again)
        snackbarHostState.showSnackbar(msg)
      }
      AuthenticationViewState.NotStarted -> authState.clearBackStack()
      is AuthenticationViewState.Success.SignIn -> {
        val resolvedSession = state.signIn.resolvePostAuthSession()
        authState.setToStepForStatus(state.signIn, resolvedSession, onAuthComplete)
        onReset()
      }
      is AuthenticationViewState.Success.SignUp -> {
        val resolvedSession = state.signUp.resolvePostAuthSession()
        authState.setToStepForStatus(state.signUp, resolvedSession, onAuthComplete)
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
