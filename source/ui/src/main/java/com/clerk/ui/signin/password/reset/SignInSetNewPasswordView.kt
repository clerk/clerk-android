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
import androidx.compose.runtime.LaunchedEffect
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
import com.clerk.ui.core.button.standard.ClerkButton
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
 * @param onBackPressed A callback to be invoked when the user presses the back button.
 */
@Composable
fun SignInSetNewPasswordView(modifier: Modifier = Modifier, onBackPressed: () -> Unit) {
  SignInSetNewPasswordViewImpl(modifier = modifier, onBackPressed = onBackPressed)
}

/**
 * The internal implementation of the [SignInSetNewPasswordView].
 *
 * @param onBackPressed A callback to be invoked when the user presses the back button.
 * @param modifier The [Modifier] to be applied to the view.
 * @param viewModel The [ResetPasswordViewModel] used to manage the state and actions of the view.
 */
@Composable
private fun SignInSetNewPasswordViewImpl(
  onBackPressed: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: ResetPasswordViewModel = viewModel(),
) {
  val snackbarHostState = remember { SnackbarHostState() }
  val state by viewModel.state.collectAsStateWithLifecycle()
  var passwordsMatch by remember { mutableStateOf(true) }
  var password by remember { mutableStateOf("") }
  var confirmPassword by remember { mutableStateOf("") }
  var signOutOtherDevices by remember { mutableStateOf(false) }

  if (state is ResetPasswordViewModel.State.Error) {
    val errorMessage = (state as ResetPasswordViewModel.State.Error).message
    LaunchedEffect(Unit) { snackbarHostState.showSnackbar(errorMessage) }
  }

  ClerkThemedAuthScaffold(
    modifier = modifier,
    onBackPressed = onBackPressed,
    hasLogo = false,
    title = stringResource(R.string.set_new_password),
    snackbarHostState = snackbarHostState,
  ) {
    ClerkTextField(
      value = password,
      onValueChange = { password = it },
      visualTransformation = PasswordVisualTransformation(),
      label = stringResource(R.string.new_password),
      inputContentType = ContentType.Password,
    )
    Spacers.Vertical.Spacer24()
    ClerkTextField(
      value = confirmPassword,
      onValueChange = { confirmPassword = it },
      label = stringResource(R.string.confirm_password),
      visualTransformation = PasswordVisualTransformation(),
      inputContentType = ContentType.Password,
      isError = !passwordsMatch,
      supportingText = if (!passwordsMatch) "Passwords don't match" else null,
    )
    Spacers.Vertical.Spacer24()
    SignOutOfOtherDevicesRow(signOutOtherDevices, onCheckChange = { signOutOtherDevices = it })
    Spacers.Vertical.Spacer24()
    ClerkButton(
      onClick = {
        if (password == confirmPassword) {
          viewModel.setNewPassword(password, signOutOtherDevices)
        } else {
          passwordsMatch = false
        }
      },
      isLoading = state is ResetPasswordViewModel.State.Loading,
      text = stringResource(R.string.reset_password),
      modifier = Modifier.fillMaxWidth(),
    )
  }
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
  ClerkMaterialTheme { SignInSetNewPasswordView(onBackPressed = {}) }
}
