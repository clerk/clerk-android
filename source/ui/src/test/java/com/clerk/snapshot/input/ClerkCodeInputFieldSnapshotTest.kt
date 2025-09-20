package com.clerk.snapshot.input

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import com.clerk.api.Clerk
import com.clerk.api.ui.ClerkTheme
import com.clerk.base.BaseSnapshotTest
import com.clerk.ui.core.common.dimens.dp16
import com.clerk.ui.core.input.ClerkCodeInputField
import com.clerk.ui.signin.code.VerificationState
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.DefaultColors
import org.junit.Test

class ClerkCodeInputFieldSnapshotTest : BaseSnapshotTest() {

  @Test
  fun testClerkCodeInputField() {
    paparazzi.snapshot {
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
  }

  @Test
  fun testClerkCodeInputFieldDarkMode() {
    Clerk.customTheme = ClerkTheme(colors = DefaultColors.dark)
    paparazzi.snapshot {
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
  }
}
