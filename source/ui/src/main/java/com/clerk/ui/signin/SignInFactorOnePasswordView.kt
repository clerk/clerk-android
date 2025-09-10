package com.clerk.ui.signin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.ui.R
import com.clerk.ui.core.appbar.ClerkTopAppBar
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonConfig
import com.clerk.ui.core.common.HeaderTextView
import com.clerk.ui.core.common.HeaderType
import com.clerk.ui.core.common.TextButton
import com.clerk.ui.core.common.dimens.dp18
import com.clerk.ui.core.common.dimens.dp24
import com.clerk.ui.core.common.dimens.dp8
import com.clerk.ui.core.input.ClerkTextField
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
fun SignInFactorOnePasswordView(modifier: Modifier = Modifier) {
  var password by remember { mutableStateOf("") }
  ClerkMaterialTheme {
    Scaffold(topBar = { ClerkTopAppBar {} }) { innerPadding ->
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .background(color = ClerkMaterialTheme.colors.background)
            .padding(innerPadding)
            .padding(dp18)
            .then(modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        HeaderTextView(type = HeaderType.Title, text = "Enter password")
        Spacer(Modifier.height(dp8))
        HeaderTextView(
          type = HeaderType.Subtitle,
          text = "Enter the password associated with your account.",
        )
        Spacer(Modifier.height(dp8))
        ClerkButton(
          text = "example@gmail.com",
          onClick = {},
          buttonConfig = ClerkButtonConfig(style = ClerkButtonConfig.ButtonStyle.Secondary),
          trailingIcon = R.drawable.ic_edit,
          trailingIconTint = ClerkMaterialTheme.colors.mutedForeground,
        )
        Spacer(Modifier.height(dp24))
        ClerkTextField(
          value = password,
          onValueChange = { password = it },
          label = "Enter your password",
          visualTransformation = PasswordVisualTransformation(),
        )
        Spacer(Modifier.height(dp24))
        ClerkButton(
          modifier = Modifier.fillMaxWidth(),
          onClick = {},
          text = "Continue",
          trailingIcon = R.drawable.ic_triangle_right,
          trailingIconTint = ClerkMaterialTheme.colors.primaryForeground,
        )
        Spacer(Modifier.height(dp24))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = dp8)) {
          TextButton("Use another method", onClick = {})
          Spacer(modifier = Modifier.weight(1f))
          TextButton("Forgot password?", onClick = {})
        }
      }
    }
  }
}

@PreviewLightDark
@Composable
private fun PreviewSignInFactorOnePasswordView() {
  ClerkMaterialTheme { SignInFactorOnePasswordView() }
}
