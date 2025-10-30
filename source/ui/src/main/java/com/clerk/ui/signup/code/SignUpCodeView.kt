package com.clerk.ui.signup.code

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
import com.clerk.ui.R
import com.clerk.ui.auth.AuthStateEffects
import com.clerk.ui.auth.LocalAuthState
import com.clerk.ui.auth.VerificationUiState
import com.clerk.ui.auth.verificationState
import com.clerk.ui.core.dimens.dp28
import com.clerk.ui.core.input.ClerkCodeInputField
import com.clerk.ui.core.progress.ClerkLinearProgressIndicator
import com.clerk.ui.core.scaffold.ClerkThemedAuthScaffold
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.util.formattedAsPhoneNumberIfPossible
import kotlinx.serialization.Serializable

/**
 * Composable function for the Sign Up Code View.
 *
 * This function displays a view where the user can enter a verification code received via email or
 * phone to complete the sign-up process.
 *
 * @param field The [SignUpCodeField] indicating whether the code was sent to a phone or email.
 * @param modifier The [Modifier] to be applied to the view.
 */
@Composable
fun SignUpCodeView(
  field: SignUpCodeField,
  modifier: Modifier = Modifier,
  onAuthComplete: () -> Unit,
) {
  SignUpCodeViewImpl(field = field, modifier = modifier, onAuthComplete = onAuthComplete)
}

@Composable
private fun SignUpCodeViewImpl(
  field: SignUpCodeField,
  onAuthComplete: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: SignUpCodeViewModel = viewModel(),
) {
  val authState = LocalAuthState.current
  val state by viewModel.state.collectAsStateWithLifecycle()
  val verificationTextState by viewModel.verificationState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(field) { viewModel.prepare(field) }
  AuthStateEffects(authState, state, snackbarHostState, onAuthComplete = onAuthComplete) {
    viewModel.reset()
  }

  val title =
    when (field) {
      is SignUpCodeField.Phone -> stringResource(R.string.check_your_phone)
      is SignUpCodeField.Email -> stringResource(R.string.check_your_email)
    }

  ClerkThemedAuthScaffold(
    modifier = modifier,
    title = title,
    hasLogo = false,
    identifier = field.value.formattedAsPhoneNumberIfPossible,
    spacingAfterIdentifier = dp28,
    snackbarHostState = snackbarHostState,
  ) {
    ClerkLinearProgressIndicator(progress = 0)
    Spacers.Vertical.Spacer32()
    ClerkCodeInputField(
      verificationState = verificationTextState.verificationState(),
      onTextChange = {
        if (verificationTextState is VerificationUiState.Error) {
          viewModel.resetVerificationState()
        }
        if (it.length == 6) {
          viewModel.attempt(field = field, code = it)
        }
      },
      onClickResend = { viewModel.prepare(field) },
    )
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  ClerkMaterialTheme {
    SignUpCodeView(field = SignUpCodeField.Phone("3012370655"), onAuthComplete = {})
  }
}

@Serializable
sealed interface SignUpCodeField {
  val value: String

  @Serializable data class Phone(override val value: String) : SignUpCodeField

  @Serializable data class Email(override val value: String) : SignUpCodeField
}
