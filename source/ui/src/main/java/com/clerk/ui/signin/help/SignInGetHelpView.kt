package com.clerk.ui.signin.help

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.ui.R
import com.clerk.ui.core.common.ClerkAuthScaffold
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
fun SignInGetHelpView(modifier: Modifier = Modifier, onBackPressed: () -> Unit = {}) {
  ClerkMaterialTheme {
    ClerkAuthScaffold(
      modifier = modifier,
      title = stringResource(R.string.get_help),
      subtitle = stringResource(R.string.if_you_have_trouble_signing_into_your_account),
      onBackPressed = onBackPressed,
    ) {}
  }
}

@PreviewLightDark
@Composable
private fun PreviewSignInGetHelpView() {
  ClerkMaterialTheme() { SignInGetHelpView() }
}
