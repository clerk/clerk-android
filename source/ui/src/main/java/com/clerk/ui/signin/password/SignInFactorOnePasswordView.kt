package com.clerk.ui.signin.password

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.clerk.api.Clerk
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.R
import com.clerk.ui.core.appbar.ClerkTopAppBar
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonConfig
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
import com.clerk.ui.core.button.standard.TextButton
import com.clerk.ui.core.common.HeaderTextView
import com.clerk.ui.core.common.HeaderType
import com.clerk.ui.core.common.dimens.dp18
import com.clerk.ui.core.common.dimens.dp24
import com.clerk.ui.core.common.dimens.dp8
import com.clerk.ui.core.input.ClerkTextField
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.DefaultColors

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
  ClerkMaterialTheme {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .background(color = ClerkMaterialTheme.colors.background)
          .padding(horizontal = dp18)
          .then(modifier),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      ClerkTopAppBar(onBackPressed = onBackPressed)
      HeaderTextView(type = HeaderType.Title, text = stringResource(R.string.enter_password))
      Spacer(Modifier.height(dp8))
      HeaderTextView(
        type = HeaderType.Subtitle,
        text = stringResource(R.string.enter_the_password_associated_with_your_account),
      )
      Spacer(Modifier.height(dp8))
      ClerkButton(
        text = email,
        onClick = {},
        modifier = Modifier.wrapContentHeight(),
        buttonConfig = ClerkButtonConfig(style = ClerkButtonConfig.ButtonStyle.Secondary),
        icons =
          ClerkButtonDefaults.icons(
            trailingIcon = R.drawable.ic_edit,
            trailingIconColor = ClerkMaterialTheme.colors.mutedForeground,
          ),
      )
      Spacer(Modifier.height(dp24))
      ClerkTextField(
        value = password,
        onValueChange = { password = it },
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
        TextButton(text = stringResource(R.string.use_another_method), onClick = onUseAnotherMethod)
        Spacer(modifier = Modifier.weight(1f))
        TextButton(text = stringResource(R.string.forgot_password), onClick = onForgotPassword)
      }
    }
  }
}

@Preview
@Composable
private fun PreviewSignInFactorOnePasswordView() {
  Clerk.customTheme = ClerkTheme(colors = DefaultColors.clerk)
  ClerkMaterialTheme { SignInFactorOnePasswordView(onContinue = {}, email = "sam@clerk.dev") }
}
