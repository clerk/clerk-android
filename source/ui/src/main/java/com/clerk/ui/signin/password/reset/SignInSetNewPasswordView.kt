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
import com.clerk.ui.R
import com.clerk.ui.auth.AuthState
import com.clerk.ui.auth.LocalAuthState
import com.clerk.ui.auth.PreviewAuthStateProvider
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.common.AuthStateEffects
import com.clerk.ui.core.common.AuthenticationViewState
import com.clerk.ui.core.common.ClerkThemedAuthScaffold
import com.clerk.ui.core.common.Spacers
import com.clerk.ui.core.common.dimens.dp16
import com.clerk.ui.core.common.dimens.dp8
import com.clerk.ui.core.input.ClerkTextField
import com.clerk.ui.theme.ClerkMaterialTheme

/**
 * A view that allows the user to set a new password during the sign-in reset password flow.
 *
 * @param modifier The [Modifier] to be applied to the view.
 */
@Composable
fun SignInSetNewPasswordView(modifier: Modifier = Modifier, onAuthComplete: () -> Unit) {
  SignInSetNewPasswordViewImpl(modifier = modifier, onAuthComplete = onAuthComplete)
}

/**
 * The internal implementation of the [SignInSetNewPasswordView].
 *
 * @param modifier The [Modifier] to be applied to the view.
 * @param viewModel The [ResetPasswordViewModel] used to manage the state and actions of the view.
 */
@Composable
private fun SignInSetNewPasswordViewImpl(
  modifier: Modifier = Modifier,
  viewModel: ResetPasswordViewModel = viewModel(),
  onAuthComplete: () -> Unit,
) {
  val authState = LocalAuthState.current
  val snackbarHostState = remember { SnackbarHostState() }
  var passwordsMatch by remember { mutableStateOf(true) }
  var signOutOtherDevices by remember { mutableStateOf(false) }
  val state by viewModel.state.collectAsStateWithLifecycle()

  val isButtonEnabled by remember {
    derivedStateOf {
      authState.signInNewPassword.isNotBlank() ||
        authState.signInConfirmNewPassword.isNotBlank() ||
        !passwordsMatch
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

  ClerkThemedAuthScaffold(
    modifier = modifier,
    onBackPressed = authState::navigateBack,
    hasLogo = false,
    title = stringResource(R.string.set_new_password),
    snackbarHostState = snackbarHostState,
  ) {
    PasswordInputs(authState, passwordsMatch)
    Spacers.Vertical.Spacer24()
    SignOutOfOtherDevicesRow(signOutOtherDevices, onCheckChange = { signOutOtherDevices = it })
    Spacers.Vertical.Spacer24()
    ClerkButton(
      onClick = {
        if (authState.signInNewPassword == authState.signInConfirmNewPassword) {
          viewModel.setNewPassword(authState.signInNewPassword, signOutOtherDevices)
        } else {
          passwordsMatch = false
        }
      },
      isLoading = state is AuthenticationViewState.Loading,
      text = stringResource(R.string.reset_password),
      modifier = Modifier.fillMaxWidth(),
      isEnabled = isButtonEnabled,
    )
  }
}

@Composable
private fun PasswordInputs(authState: AuthState, passwordsMatch: Boolean) {
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
    isError = !passwordsMatch,
    supportingText = if (!passwordsMatch) "Passwords don't match" else null,
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
      style = ClerkMaterialTheme.typography.titleMedium,
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
