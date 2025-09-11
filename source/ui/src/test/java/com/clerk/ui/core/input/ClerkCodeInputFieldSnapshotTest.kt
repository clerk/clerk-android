package com.clerk.ui.core.input

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import com.clerk.api.Clerk
import com.clerk.api.ui.ClerkTheme
import com.clerk.base.BaseSnapshotTest
import com.clerk.ui.core.common.dimens.dp16
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
          ClerkCodeInputField(onOtpTextChange = {}, secondsLeft = 30, onClickResend = {})
          ClerkCodeInputField(
            onOtpTextChange = {},
            verificationState = VerificationState.Error,
            secondsLeft = 30,
            onClickResend = {},
          )
          ClerkCodeInputField(
            onOtpTextChange = {},
            verificationState = VerificationState.Success,
            secondsLeft = 0,
            onClickResend = {},
          )
          ClerkCodeInputField(
            onOtpTextChange = {},
            verificationState = VerificationState.Verifying,
            secondsLeft = 0,
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
          ClerkCodeInputField(onOtpTextChange = {}, secondsLeft = 30, onClickResend = {})
          ClerkCodeInputField(
            onOtpTextChange = {},
            verificationState = VerificationState.Error,
            secondsLeft = 30,
            onClickResend = {},
          )
          ClerkCodeInputField(
            onOtpTextChange = {},
            verificationState = VerificationState.Success,
            secondsLeft = 0,
            onClickResend = {},
          )
          ClerkCodeInputField(
            onOtpTextChange = {},
            verificationState = VerificationState.Verifying,
            secondsLeft = 0,
            onClickResend = {},
          )
        }
      }
    }
  }
}
