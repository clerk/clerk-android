package com.clerk.ui.core.input

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.signin.code.VerificationState
import com.clerk.ui.theme.ClerkMaterialTheme

@PreviewLightDark
@Composable
private fun PreviewClerkCodeInputField() {
  ClerkMaterialTheme {
    Column(
      modifier = Modifier.background(ClerkMaterialTheme.colors.background).padding(dp16),
      verticalArrangement = Arrangement.spacedBy(dp16),
    ) {
      ClerkCodeInputField(onTextChange = {}, onClickResend = {})
      ClerkCodeInputField(
        onTextChange = {},
        verificationState = VerificationState.Error,
        onClickResend = {},
      )
      ClerkCodeInputField(
        onTextChange = {},
        verificationState = VerificationState.Success,
        onClickResend = {},
      )
      ClerkCodeInputField(
        onTextChange = {},
        verificationState = VerificationState.Verifying,
        onClickResend = {},
      )
    }
  }
}
