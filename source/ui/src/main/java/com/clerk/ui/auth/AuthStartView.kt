package com.clerk.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.ui.core.common.ClerkThemedAuthScaffold
import com.clerk.ui.core.input.ClerkTextField

@Composable
fun AuthStartView(
  authMode: AuthMode,
  modifier: Modifier = Modifier,
  authViewHelper: AuthViewHelper = AuthViewHelper(),
) {

  ClerkThemedAuthScaffold(
    modifier = modifier,
    hasBackButton = false,
    title = authViewHelper.titleString(authMode),
  ) {
    ClerkTextField(value = "", onValueChange = {}, label = "Email address")
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  AuthStartView(authMode = AuthMode.SignIn)
}
