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

/** Default timer length in seconds for code resend functionality. */
private const val DEFAULT_TIMER_LENGTH = 60

/** Default delay in milliseconds for timer countdown. */
private const val DEFAULT_DELAY = 1000L

/**
 * A Composable that displays a verification code input screen for sign-in authentication factors.
 *
 * This component handles various authentication strategies including:
 * - Email verification codes
 * - SMS verification codes
 * - Password reset codes
 * - TOTP codes for two-factor authentication
 *
 * The view automatically prepares the factor for verification and provides a code input field with
 * resend functionality and timer countdown.
 *
 * @param factor The authentication factor containing strategy and identifier information
 * @param modifier Optional [Modifier] to customize the appearance and behavior
 * @param onBackPressed Callback invoked when the back button is pressed
 * @param onClickResend Callback invoked when the resend code button is clicked
 * @sample
 *
 * ```kotlin
 * SignInFactorCodeView(
 *   factor = Factor(strategy = "email_code", emailAddressId = "user@example.com"),
 *   onBackPressed = { navController.popBackStack() },
 *   onClickResend = { /* Handle resend logic */ }
 * )
 * ```
 */
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

/**
 * Internal implementation of the SignInFactorCodeView that manages the UI state and interactions.
 *
 * This component handles:
 * - View model state management
 * - Factor preparation on composition
 * - Timer countdown for resend functionality
 * - Code input and automatic submission when complete
 * - Navigation actions (back, use another method)
 *
 * @param factor The authentication factor to process
 * @param onBackPressed Callback for back navigation
 * @param onClickResend Callback for resend code action
 * @param modifier Optional modifier for styling
 * @param viewModel The view model managing the sign-in state (injected via Compose)
 * @param onUserAnotherMethod Callback for "use another method" action
 */
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

  // Prepare the factor and start timer countdown on composition
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

/**
 * Extension function that converts a [SignInFactorCodeViewModel.State] to a [VerificationState].
 *
 * This mapping provides a UI-focused state representation that can be used by input components to
 * determine their visual appearance and behavior.
 *
 * @return The corresponding [VerificationState] for the current view model state
 */
private fun SignInFactorCodeViewModel.State.verificationState(): VerificationState {
  return when (this) {
    SignInFactorCodeViewModel.State.Error -> VerificationState.Error
    SignInFactorCodeViewModel.State.Idle -> VerificationState.Default
    SignInFactorCodeViewModel.State.Success -> VerificationState.Success
    SignInFactorCodeViewModel.State.Verifying -> VerificationState.Verifying
  }
}

/**
 * Preview function for the SignInFactorCodeView component.
 *
 * Demonstrates the component with a phone code factor for development and design purposes.
 */
@Preview
@Composable
private fun PreviewSignInFactorCodeView() {
  Clerk.customTheme = ClerkTheme(colors = DefaultColors.clerk)
  ClerkMaterialTheme {
    SignInFactorCodeView(Factor(StrategyKeys.PHONE_CODE, safeIdentifier = "sam@clerk.dev"))
  }
}

/**
 * Sealed interface representing the various verification states for code input components.
 *
 * This interface provides a UI-focused state system that abstracts the underlying view model states
 * into states that are relevant for verification input components.
 *
 * States correspond to different visual appearances and behaviors:
 * - [Default]: Initial state, ready for input
 * - [Verifying]: Currently processing the code
 * - [Success]: Code verification succeeded
 * - [Error]: Code verification failed
 */
sealed interface VerificationState {

  /**
   * Default state indicating the component is ready for user input. Typically shows normal input
   * styling without any status indicators.
   */
  data object Default : VerificationState

  /**
   * Verifying state indicating code submission is in progress. Usually displays loading indicators
   * and disables input.
   */
  data object Verifying : VerificationState

  /**
   * Success state indicating the code was successfully verified. Often shows success indicators and
   * may trigger navigation.
   */
  data object Success : VerificationState

  /**
   * Error state indicating code verification failed. Typically displays error styling and allows
   * retry.
   */
  data object Error : VerificationState
}
