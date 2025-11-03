package com.clerk.ui.userprofile.security.password

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.ui.R
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.input.ClerkTextField
import com.clerk.ui.core.input.PasswordKeyboardOptions
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.userprofile.LocalUserProfileState
import com.clerk.ui.userprofile.UserProfileDestination
import com.clerk.ui.userprofile.common.BottomSheetTopBar

@Composable
internal fun UserProfileCurrentPasswordView(
  passwordAction: PasswordAction,
  modifier: Modifier = Modifier,
  onClosePressed: () -> Unit,
) {
  UserProfileCurrentPasswordViewImpl(
    modifier = modifier,
    passwordAction = passwordAction,
    onClosePressed = onClosePressed,
  )
}

@Composable
private fun UserProfileCurrentPasswordViewImpl(
  passwordAction: PasswordAction,
  onClosePressed: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val userProfileState = LocalUserProfileState.current
  var currentPassword by rememberSaveable { mutableStateOf("") }
  ClerkMaterialTheme {
    Column(modifier = modifier) {
      BottomSheetTopBar(
        title =
          if (passwordAction == PasswordAction.Add) stringResource(R.string.add_password)
          else stringResource(R.string.update_password),
        onClosePressed = onClosePressed,
      )

      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = dp24).padding(bottom = dp24)) {
        Text(
          text = stringResource(R.string.enter_your_current_password_to_set_a_new_one),
          style = ClerkMaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal),
          color = ClerkMaterialTheme.colors.mutedForeground,
        )
        Spacers.Vertical.Spacer12()
        ClerkTextField(
          value = currentPassword,
          onValueChange = { currentPassword = it },
          label = stringResource(R.string.current_password),
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = PasswordKeyboardOptions,
          inputContentType = ContentType.Password,
        )
        Spacers.Vertical.Spacer24()
        ClerkButton(
          isEnabled = currentPassword.isNotBlank(),
          modifier = Modifier.fillMaxWidth(),
          text = stringResource(R.string.next),
          onClick = {
            userProfileState.navigateTo(
              UserProfileDestination.UpdatePasswordNew(
                currentPassword = currentPassword,
                passwordAction = passwordAction,
              )
            )
          },
        )
      }
    }
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  ClerkMaterialTheme {
    Box(modifier = Modifier.background(ClerkMaterialTheme.colors.background)) {
      UserProfileCurrentPasswordViewImpl(passwordAction = PasswordAction.Reset, onClosePressed = {})
    }
  }
}
