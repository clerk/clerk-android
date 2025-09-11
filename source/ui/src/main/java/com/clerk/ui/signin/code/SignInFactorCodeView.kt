package com.clerk.ui.signin.code

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.Clerk
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.R
import com.clerk.ui.core.common.AuthViewHeader
import com.clerk.ui.core.common.SecuredByClerk
import com.clerk.ui.core.common.Spacers
import com.clerk.ui.core.common.StrategyKeys
import com.clerk.ui.core.common.TextButton
import com.clerk.ui.core.common.dimens.dp18
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
  onClickResend: () -> Unit = {},
) {
  SignInFactorCodeViewImpl(
    factor = factor,
    modifier = modifier,
    onBackPressed = onBackPressed,
    onClickResend = onClickResend,
  )
}

@Composable
private fun SignInFactorCodeViewImpl(
  factor: Factor,
  onBackPressed: () -> Unit,
  onClickResend: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: SignInFactorCodeViewModel = viewModel(),
  onUserAnotherMethod: () -> Unit = {},
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val verificationState = state.verificationState()

  var timeLeft by remember { mutableIntStateOf(DEFAULT_TIMER_LENGTH) }
  LaunchedEffect(Unit) {
    viewModel.prepare(factor, isSecondFactor = false)
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
      AuthViewHeader(factor, onBackPressed = onBackPressed)
      Spacers.Vertical.Spacer32()
      ClerkCodeInputField(
        verificationState = verificationState,
        onOtpTextChange = {
          if (it.length == 6) {
            viewModel.attempt(factor, isSecondFactor = false, code = it)
          }
        },
        showResend = SignInFactorCodeHelper.showResend(factor, verificationState),
        secondsLeft = timeLeft,
        onClickResend = onClickResend,
      )
      Spacers.Vertical.Spacer24()
      if (SignInFactorCodeHelper.showUseAnotherMethod(factor)) {
        TextButton(
          text = stringResource(R.string.use_another_method),
          onClick = onUserAnotherMethod,
        )
      }
      Spacers.Vertical.Spacer32()
      SecuredByClerk()
    }
  }
}

private fun SignInFactorCodeViewModel.State.verificationState(): VerificationState {
  return when (this) {
    SignInFactorCodeViewModel.State.Error -> VerificationState.Error
    SignInFactorCodeViewModel.State.Idle -> VerificationState.Default
    SignInFactorCodeViewModel.State.Success -> VerificationState.Success
    SignInFactorCodeViewModel.State.Verifying -> VerificationState.Verifying
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

  data object Error : VerificationState
}
