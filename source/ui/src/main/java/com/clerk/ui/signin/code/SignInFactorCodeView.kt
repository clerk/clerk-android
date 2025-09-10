package com.clerk.ui.signin.code

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.clerk.api.Clerk
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.R
import com.clerk.ui.core.appbar.ClerkTopAppBar
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonConfig
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
import com.clerk.ui.core.common.HeaderTextView
import com.clerk.ui.core.common.HeaderType
import com.clerk.ui.core.common.StrategyKeys
import com.clerk.ui.core.common.dimens.dp18
import com.clerk.ui.core.common.dimens.dp32
import com.clerk.ui.core.common.dimens.dp8
import com.clerk.ui.core.input.ClerkCodeInputField
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.DefaultColors
import kotlinx.coroutines.delay

private const val DEFAULT_TIMER_LENGTH = 60

private const val DEFAULT_DELAY = 1000L

@Composable
fun SignInFactorCodeView(
  factor: Factor,
  modifier: Modifier = Modifier,
  onBackPressed: () -> Unit = {},
) {
  var timeLeft by remember { mutableIntStateOf(DEFAULT_TIMER_LENGTH) }
  LaunchedEffect(Unit) {
    while (timeLeft > 0) {
      delay(DEFAULT_DELAY)
      timeLeft--
    }
  }

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
      HeaderTextView(
        text = SignInFactorCodeHelper.titleForStrategy(factor),
        type = HeaderType.Title,
      )
      Spacer(modifier = Modifier.height(dp8))
      HeaderTextView(
        text = SignInFactorCodeHelper.subtitleForStrategy(factor),
        type = HeaderType.Subtitle,
      )
      Spacer(modifier = Modifier.height(dp8))
      ClerkButton(
        text = factor.safeIdentifier.orEmpty(),
        onClick = {},
        modifier = Modifier.wrapContentHeight(),
        buttonConfig = ClerkButtonConfig(style = ClerkButtonConfig.ButtonStyle.Secondary),
        icons =
          ClerkButtonDefaults.icons(
            trailingIcon = R.drawable.ic_edit,
            trailingIconColor = ClerkMaterialTheme.colors.mutedForeground,
          ),
      )
      Spacer(modifier = Modifier.height(dp32))
      ClerkCodeInputField(onOtpTextChange = {}, secondsLeft = timeLeft, onClickResend = {})
    }
  }
}

@Preview
@Composable
private fun PreviewSignInFactorCodeView() {
  Clerk.customTheme = ClerkTheme(colors = DefaultColors.clerk)
  ClerkMaterialTheme {
    SignInFactorCodeView(Factor(StrategyKeys.PHONE_CODE, safeIdentifier = "sam@clerk.dev"))
  }
}

sealed interface VerificationState {
  data object Default : VerificationState

  data object Verifying : VerificationState

  data object Success : VerificationState

  data class Error(val message: String) : VerificationState
}
