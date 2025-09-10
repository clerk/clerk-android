package com.clerk.ui.signin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.ui.core.appbar.ClerkTopAppBar
import com.clerk.ui.core.common.HeaderTextView
import com.clerk.ui.core.common.HeaderType
import com.clerk.ui.core.common.dimens.dp8
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
fun SignInFactorOnePasswordView(modifier: Modifier = Modifier) {
  ClerkMaterialTheme {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .background(color = ClerkMaterialTheme.colors.background)
          .padding(dp8),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      ClerkTopAppBar {}
      HeaderTextView(type = HeaderType.Title, text = "Enter password")
      Spacer(Modifier.height(dp8))
      HeaderTextView(
        type = HeaderType.Subtitle,
        text = "Enter the password associated with your account.",
      )

    }
  }
}

@PreviewLightDark
@Composable
private fun PreviewSignInFactorOnePasswordView() {
  SignInFactorOnePasswordView()
}
