package com.clerk.ui.signin.password.reset

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.R
import com.clerk.ui.auth.AuthState
import com.clerk.ui.auth.AuthStateEffects
import com.clerk.ui.auth.AuthenticationViewState
import com.clerk.ui.auth.PreviewAuthStateProvider
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.composition.LocalAuthState
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.core.input.ClerkTextField
import com.clerk.ui.core.scaffold.ClerkThemedAuthScaffold
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.ClerkThemeOverrideProvider

/**
 * A view that allows the user to set a new password during the sign-in reset password flow.
 *
 * @param modifier The [Modifier] to be applied to the view.
 */
@Composable
fun SignInSetNewPasswordView(
  modifier: Modifier = Modifier,
  clerkTheme: ClerkTheme? = null,
  onAuthComplete: () -> Unit,
) {
  ClerkThemeOverrideProvider(clerkTheme) {
    SignInSetNewPasswordViewImpl(
      modifier = modifier,
      mode = ResetPasswordMode.SIGN_IN,
      onAuthComplete = onAuthComplete,
    )
  }
}

@Composable
internal fun SessionTaskResetPasswordView(
  modifier: Modifier = Modifier,
  clerkTheme: ClerkTheme? = null,
  onAuthComplete: () -> Unit,
) {
  ClerkThemeOverrideProvider(clerkTheme) {
    SignInSetNewPasswordViewImpl(
      modifier = modifier,
      mode = ResetPasswordMode.SESSION_TASK,
      onAuthComplete = onAuthComplete,
    )
  }
}

/**
 * The internal implementation of the [SignInSetNewPasswordView].
 *
 * @param modifier The [Modifier] to be applied to the view.
 */
@Composable
private fun SignInSetNewPasswordViewImpl(
  mode: ResetPasswordMode,
  modifier: Modifier = Modifier,
  viewModel: ResetPasswordViewModel = viewModel(key = mode.viewModelKey()),
  onAuthComplete: () -> Unit,
) {
  val authState = LocalAuthState.current
  val snackbarHostState = remember { SnackbarHostState() }
  var signOutOtherDevices by remember { mutableStateOf(false) }
  val state by viewModel.state.collectAsStateWithLifecycle()
  val passwordsMatch by
    remember(authState.signInNewPassword, authState.signInConfirmNewPassword) {
      derivedStateOf {
        authState.signInNewPassword == authState.signInConfirmNewPassword ||
          authState.signInConfirmNewPassword.isBlank()
      }
    }

  AuthStateEffects(
    state = state,
    authState = authState,
    snackbarHostState = snackbarHostState,
    onAuthComplete = onAuthComplete,
  ) {
    viewModel.resetState()
  }
  val isButtonEnabled =
    authState.signInNewPassword.isNotBlank() &&
      authState.signInConfirmNewPassword.isNotBlank() &&
      passwordsMatch
  val onResetPassword = {
    if (passwordsMatch) {
      when (mode) {
        ResetPasswordMode.SIGN_IN ->
          viewModel.setNewPassword(authState.signInNewPassword, signOutOtherDevices)
        ResetPasswordMode.SESSION_TASK ->
          viewModel.completeSessionTask(authState.signInNewPassword, signOutOtherDevices)
      }
    }
  }

  ClerkThemedAuthScaffold(
    modifier = modifier,
    onBackPressed = authState::navigateBack,
    hasLogo = false,
    title = stringResource(R.string.set_new_password),
    subtitle = mode.subtitle(),
    hasBackButton = mode == ResetPasswordMode.SIGN_IN,
    snackbarHostState = snackbarHostState,
  ) {
    PasswordInputs(authState, passwordsMatch)
    Spacers.Vertical.Spacer24()
    SignOutOfOtherDevicesRow(signOutOtherDevices, onCheckChange = { signOutOtherDevices = it })
    Spacers.Vertical.Spacer24()
    ClerkButton(
      onClick = onResetPassword,
      isLoading = state is AuthenticationViewState.Loading,
      text = stringResource(R.string.reset_password),
      modifier = Modifier.fillMaxWidth(),
      isEnabled = isButtonEnabled,
    )
  }
}

@Composable
private fun ResetPasswordMode.subtitle(): String? {
  return if (this == ResetPasswordMode.SESSION_TASK) {
    stringResource(R.string.account_requires_new_password_before_continue)
  } else {
    null
  }
}

@Composable
private fun PasswordInputs(authState: AuthState, passwordsMatch: Boolean) {
  val showPasswordMismatchError = authState.signInConfirmNewPassword.isNotBlank() && !passwordsMatch
  ClerkTextField(
    value = authState.signInNewPassword,
    onValueChange = { authState.signInNewPassword = it },
    visualTransformation = PasswordVisualTransformation(),
    label = stringResource(R.string.new_password),
    inputContentType = ContentType.Password,
  )
  Spacers.Vertical.Spacer24()
  ClerkTextField(
    value = authState.signInConfirmNewPassword,
    onValueChange = { authState.signInConfirmNewPassword = it },
    label = stringResource(R.string.confirm_password),
    visualTransformation = PasswordVisualTransformation(),
    inputContentType = ContentType.Password,
    isError = showPasswordMismatchError,
    supportingText = if (showPasswordMismatchError) "Passwords don't match" else null,
  )
}

/**
 * A row containing a label and a [Switch] to toggle signing out of other devices.
 *
 * @param signOutOtherDevices The current state of the switch.
 * @param onCheckChange A callback to be invoked when the switch is toggled.
 */
@Composable
private fun SignOutOfOtherDevicesRow(
  signOutOtherDevices: Boolean,
  onCheckChange: (Boolean) -> Unit,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .background(color = ClerkMaterialTheme.colors.muted)
        .padding(horizontal = dp16, vertical = dp8),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = stringResource(R.string.sign_out_of_all_other_devices),
      style = ClerkMaterialTheme.typography.bodyLarge,
    )
    Spacer(modifier = Modifier.weight(1f))
    Switch(
      checked = signOutOtherDevices,
      onCheckedChange = onCheckChange,
      colors =
        SwitchDefaults.colors(
          uncheckedBorderColor = ClerkMaterialTheme.computedColors.inputBorder,
          uncheckedThumbColor = ClerkMaterialTheme.colors.mutedForeground,
          uncheckedTrackColor = ClerkMaterialTheme.colors.muted,
        ),
    )
  }
}

@PreviewLightDark
@Composable
private fun PreviewSignInSetNewPasswordView() {
  PreviewAuthStateProvider { ClerkMaterialTheme { SignInSetNewPasswordView(onAuthComplete = {}) } }
}
