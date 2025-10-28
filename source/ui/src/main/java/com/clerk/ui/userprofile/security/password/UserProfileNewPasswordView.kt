package com.clerk.ui.userprofile.security.password

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.ui.R
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.core.input.ClerkTextField
import com.clerk.ui.core.input.PasswordKeyboardOptions
import com.clerk.ui.core.scaffold.ClerkThemedProfileScaffold
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
internal fun UserProfileNewPasswordView(
  currentPassword: String?,
  passwordAction: PasswordAction,
  onSuccess: () -> Unit,
  modifier: Modifier = Modifier,
) {
  UserProfileNewPasswordViewImpl(
    modifier = modifier,
    passwordAction = passwordAction,
    currentPassword = currentPassword,
    onSuccess = onSuccess,
  )
}

@Composable
private fun UserProfileNewPasswordViewImpl(
  passwordAction: PasswordAction,
  currentPassword: String?,
  onSuccess: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: UserProfileChangePasswordViewModel = viewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  LaunchedEffect(state) {
    if (state is UserProfileChangePasswordViewModel.State.Success) {
      onSuccess()
    }
  }
  val errorMessage = (state as? UserProfileChangePasswordViewModel.State.Error)?.message
  ClerkThemedProfileScaffold(
    modifier = modifier,
    errorMessage = errorMessage,
    hasBackButton = true,
    contentTopPadding = dp12,
    title =
      if (passwordAction == PasswordAction.Reset) stringResource(R.string.update_password)
      else stringResource(R.string.add_password),
    content = {
      UpdatePasswordContent(
        isLoading = state is UserProfileChangePasswordViewModel.State.Loading,
        onClick = { newPassword, signOutOtherSessions ->
          viewModel.resetPassword(
            currentPassword = currentPassword,
            newPassword = newPassword,
            signOutOfOtherSessions = signOutOtherSessions,
          )
        },
      )
    },
  )
}

@Composable
private fun UpdatePasswordContent(isLoading: Boolean = false, onClick: (String, Boolean) -> Unit) {
  var newPassword by rememberSaveable { mutableStateOf("") }
  var confirmPassword by rememberSaveable { mutableStateOf("") }
  var signOutOfOtherDevices by rememberSaveable { mutableStateOf(false) }
  Column(modifier = Modifier.fillMaxWidth()) {
    ClerkTextField(
      modifier = Modifier.fillMaxWidth(),
      value = newPassword,
      onValueChange = { newPassword = it },
      label = stringResource(R.string.new_password),
      visualTransformation = PasswordVisualTransformation(),
      inputContentType = ContentType.NewPassword,
      keyboardOptions = PasswordKeyboardOptions.copy(imeAction = ImeAction.Next),
    )
    Spacers.Vertical.Spacer16()
    ClerkTextField(
      modifier = Modifier.fillMaxWidth(),
      value = confirmPassword,
      onValueChange = { confirmPassword = it },
      label = stringResource(R.string.confirm_password),
      visualTransformation = PasswordVisualTransformation(),
      inputContentType = ContentType.NewPassword,
      keyboardOptions = PasswordKeyboardOptions,
    )
    Spacers.Vertical.Spacer20()
    SignOutOtherDevicesContent(
      signOutOfOtherDevices = signOutOfOtherDevices,
      onCheckChange = { signOutOfOtherDevices = !it },
    )
    Spacers.Vertical.Spacer24()
    ClerkButton(
      isLoading = isLoading,
      isEnabled =
        newPassword.isNotBlank() && confirmPassword.isNotBlank() && newPassword == confirmPassword,
      modifier = Modifier.fillMaxWidth(),
      text = stringResource(R.string.save),
      onClick = { onClick(newPassword, signOutOfOtherDevices) },
    )
  }
}

@Composable
private fun SignOutOtherDevicesContent(
  signOutOfOtherDevices: Boolean,
  onCheckChange: (Boolean) -> Unit,
) {
  Column(
    modifier =
      Modifier.background(color = ClerkMaterialTheme.colors.muted, shape = ClerkMaterialTheme.shape)
        .padding(horizontal = dp16, vertical = dp8)
  ) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = stringResource(R.string.sign_out_of_all_other_devices),
        color = ClerkMaterialTheme.colors.foreground,
        style = ClerkMaterialTheme.typography.bodyLarge,
      )
      Spacer(modifier = Modifier.weight(1f))
      Switch(
        checked = signOutOfOtherDevices,
        onCheckedChange = { onCheckChange(signOutOfOtherDevices) },
        colors =
          SwitchDefaults.colors(
            uncheckedBorderColor = ClerkMaterialTheme.computedColors.inputBorder,
            uncheckedThumbColor = ClerkMaterialTheme.colors.mutedForeground,
            uncheckedTrackColor = ClerkMaterialTheme.colors.muted,
          ),
      )
    }
    Text(
      text = stringResource(R.string.it_is_recommended_to_sign_out),
      color = ClerkMaterialTheme.colors.mutedForeground,
      style = ClerkMaterialTheme.typography.bodyMedium,
    )
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  ClerkMaterialTheme {
    UserProfileNewPasswordViewImpl(
      passwordAction = PasswordAction.Add,
      "MySecretPassword123",
      onSuccess = {},
    )
  }
}
