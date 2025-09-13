package com.clerk.ui.signin.password.set

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.clerk.api.Clerk
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.R
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
import com.clerk.ui.core.button.standard.ClerkTextButton
import com.clerk.ui.core.common.ClerkThemedAuthScaffold
import com.clerk.ui.core.common.dimens.dp24
import com.clerk.ui.core.common.dimens.dp8
import com.clerk.ui.core.input.ClerkTextField
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.DefaultColors

/**
 * A composable view for the first factor password step in the sign-in flow.
 *
 * This view displays a password input field, the user's email, and options for "Forgot Password"
 * and "Use another method".
 *
 * @param onContinue A callback invoked with the entered password when the user clicks the continue
 *   button.
 * @param email The user's email address to be displayed.
 * @param modifier The [Modifier] to be applied to the view.
 * @param onUseAnotherMethod A callback invoked when the user clicks the "Use another method"
 *   button.
 * @param onForgotPassword A callback invoked when the user clicks the "Forgot password" button.
 * @param onBackPressed A callback invoked when the user clicks the back button in the app bar.
 */
@Composable
fun SignInFactorOnePasswordView(
  onContinue: (String) -> Unit,
  email: String,
  modifier: Modifier = Modifier,
  onUseAnotherMethod: () -> Unit = {},
  onForgotPassword: () -> Unit = {},
  onBackPressed: () -> Unit = {},
) {
  SignInFactorOnePasswordViewImpl(
    modifier = modifier,
    onBackPressed = onBackPressed,
    email = email,
    onContinue = onContinue,
    onUseAnotherMethod = onUseAnotherMethod,
    onForgotPassword = onForgotPassword,
  )
}

@Composable
private fun SignInFactorOnePasswordViewImpl(
  onBackPressed: () -> Unit,
  email: String,
  onContinue: (String) -> Unit,
  onUseAnotherMethod: () -> Unit,
  modifier: Modifier = Modifier,
  onForgotPassword: () -> Unit,
) {
  var password by remember { mutableStateOf("") }
  ClerkThemedAuthScaffold(
    onBackPressed = onBackPressed,
    identifier = email,
    modifier = modifier,
    title = stringResource(R.string.enter_password),
    subtitle = stringResource(R.string.enter_the_password_associated_with_your_account),
  ) {
    ClerkTextField(
      value = password,
      label = stringResource(R.string.enter_your_password),
      visualTransformation = PasswordVisualTransformation(),
    )
    Spacer(Modifier.height(dp24))
    ClerkButton(
      modifier = Modifier.fillMaxWidth(),
      onClick = { onContinue(password) },
      text = stringResource(R.string.continue_text),
      icons =
        ClerkButtonDefaults.icons(
          trailingIcon = R.drawable.ic_triangle_right,
          trailingIconColor = ClerkMaterialTheme.colors.primaryForeground,
        ),
    )
    Spacer(Modifier.height(dp24))
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = dp8)) {
      ClerkTextButton(
        text = stringResource(R.string.use_another_method),
        onClick = onUseAnotherMethod,
      )
      Spacer(modifier = Modifier.weight(1f))
      ClerkTextButton(text = stringResource(R.string.forgot_password), onClick = onForgotPassword)
    }
  }
}

@Preview
@Composable
private fun PreviewSignInFactorOnePasswordView() {
  Clerk.customTheme = ClerkTheme(colors = DefaultColors.clerk)
  ClerkMaterialTheme { SignInFactorOnePasswordView(onContinue = {}, email = "sam@clerk.dev") }
}
