package com.clerk.ui.signin.code

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.Clerk
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.R
import com.clerk.ui.auth.AuthDestination
import com.clerk.ui.auth.AuthStateEffects
import com.clerk.ui.auth.PreviewAuthStateProvider
import com.clerk.ui.auth.VerificationUiState
import com.clerk.ui.auth.verificationState
import com.clerk.ui.core.button.standard.ClerkTextButton
import com.clerk.ui.core.common.StrategyKeys
import com.clerk.ui.core.composition.LocalAuthState
import com.clerk.ui.core.input.ClerkCodeInputField
import com.clerk.ui.core.scaffold.ClerkThemedAuthScaffold
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.signin.clienttrust.ClientTrustWarningMessage
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.ClerkThemeOverrideProvider

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
 * @sample
 *
 * ```kotlin
 * SignInFactorCodeView(
 *   factor = Factor(strategy = "email_code", emailAddressId = "user@example.com")
 * )
 * ```
 */
@Composable
fun SignInFactorCodeView(
  factor: Factor,
  modifier: Modifier = Modifier,
  isSecondFactor: Boolean = false,
  isClientTrust: Boolean = false,
  clerkTheme: ClerkTheme? = null,
  onAuthComplete: () -> Unit,
) {
  ClerkThemeOverrideProvider(clerkTheme) {
    SignInFactorCodeViewImpl(
      factor = factor,
      modifier = modifier,
      isSecondFactor = isSecondFactor,
      isClientTrust = isClientTrust,
      onAuthComplete = onAuthComplete,
    )
  }
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
 * @param modifier Optional modifier for styling
 */
@Composable
private fun SignInFactorCodeViewImpl(
  factor: Factor,
  modifier: Modifier = Modifier,
  isSecondFactor: Boolean = false,
  isClientTrust: Boolean = false,
  viewModel: SignInFactorCodeViewModel =
    viewModel(key = signInFactorCodeViewModelKey(isSecondFactor)),
  onAuthComplete: () -> Unit,
) {
  val authState = LocalAuthState.current
  val state by viewModel.state.collectAsStateWithLifecycle()
  val verificationTextState by viewModel.verificationUiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(Unit) { viewModel.prepare(factor, isSecondFactor = isSecondFactor) }

  AuthStateEffects(
    authState = authState,
    state = state,
    snackbarHostState = snackbarHostState,
    onAuthComplete = onAuthComplete,
    onReset = {
      viewModel.resetState()
      viewModel.resetVerificationState()
    },
  )
  ClerkThemedAuthScaffold(
    modifier = modifier,
    onBackPressed = { authState.navigateBack() },
    title = SignInFactorCodeUiHelper.titleForStrategy(factor),
    subtitle = SignInFactorCodeUiHelper.subtitleForStrategy(factor),
    identifier = factor.safeIdentifier,
    snackbarHostState = snackbarHostState,
    onClickIdentifier = { authState.clearBackStack() },
  ) {
    if (isClientTrust) {
      ClientTrustWarningMessage()
    }
    SignInCodeInput(
      factor = factor,
      verificationTextState = verificationTextState,
      isSecondFactor = isSecondFactor,
      viewModel = viewModel,
    )
    Spacers.Vertical.Spacer24()
    if (SignInFactorCodeUiHelper.showUseAnotherMethod(factor)) {
      ClerkTextButton(
        text = stringResource(R.string.use_another_method),
        onClick = {
          if (isSecondFactor) {
            authState.navigateTo(
              AuthDestination.SignInFactorTwoUseAnotherMethod(currentFactor = factor)
            )
          } else {
            authState.navigateTo(
              AuthDestination.SignInFactorOneUseAnotherMethod(currentFactor = factor)
            )
          }
        },
      )
    }
  }
}

private fun signInFactorCodeViewModelKey(isSecondFactor: Boolean): String =
  "sign-in-code-${Clerk.auth.currentSignIn?.id ?: "no-sign-in"}-$isSecondFactor"

@Composable
private fun SignInCodeInput(
  factor: Factor,
  verificationTextState: VerificationUiState,
  isSecondFactor: Boolean,
  viewModel: SignInFactorCodeViewModel,
) {
  ClerkCodeInputField(
    verificationState = verificationTextState.verificationState(),
    onTextChange = {
      if (verificationTextState is VerificationUiState.Error) {
        viewModel.resetState()
        viewModel.resetVerificationState()
      }
      if (it.length == 6) {
        viewModel.attempt(factor, isSecondFactor = isSecondFactor, code = it)
      }
    },
    showResend =
      SignInFactorCodeUiHelper.showResend(factor, verificationTextState.verificationState()),
    onClickResend = { viewModel.prepare(factor, isSecondFactor = isSecondFactor) },
  )
}

/**
 * Preview function for the SignInFactorCodeView component.
 *
 * Demonstrates the component with a phone code factor for development and design purposes.
 */
@PreviewLightDark
@Composable
private fun PreviewSignInFactorCodeView() {
  ClerkMaterialTheme {
    PreviewAuthStateProvider {
      SignInFactorCodeView(
        Factor(StrategyKeys.PHONE_CODE, safeIdentifier = "sam@clerk.dev"),
        onAuthComplete = {},
      )
    }
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
