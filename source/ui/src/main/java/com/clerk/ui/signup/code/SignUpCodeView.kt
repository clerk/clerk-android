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
import com.clerk.ui.core.common.ClerkThemedAuthScaffold
import com.clerk.ui.core.common.Spacers
import com.clerk.ui.core.input.ClerkCodeInputField
import com.clerk.ui.core.progress.ClerkLinearProgressIndicator
import com.clerk.ui.signin.code.VerificationState
import com.clerk.ui.util.formattedAsPhoneNumberIfPossible

@Composable
fun SignUpCodeView(field: Field, modifier: Modifier = Modifier) {
  SignUpCodeViewImpl(field, modifier)
}

@Composable
private fun SignUpCodeViewImpl(
  field: Field,
  modifier: Modifier = Modifier,
  viewModel: SignUpCodeViewModel = viewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val genericErrorMessage = stringResource(R.string.something_went_wrong_please_try_again)

  LaunchedEffect(Unit) { viewModel.prepare(field) }
  val errorMessage =
    when (state) {
      is SignUpCodeViewModel.AuthenticationState.Error ->
        (state as SignUpCodeViewModel.AuthenticationState.Error).message
      else -> null
    }
  LaunchedEffect(errorMessage) {
    errorMessage?.let { snackbarHostState.showSnackbar(it.ifBlank { genericErrorMessage }) }
  }

  val title =
    when (field) {
      is Field.Phone -> stringResource(R.string.check_your_phone)
      is Field.Email -> stringResource(R.string.check_your_email)
    }

  val verificationState =
    when (state) {
      SignUpCodeViewModel.AuthenticationState.CodeSent -> VerificationState.Default
      is SignUpCodeViewModel.AuthenticationState.Error -> VerificationState.Error
      SignUpCodeViewModel.AuthenticationState.Idle -> VerificationState.Default
      SignUpCodeViewModel.AuthenticationState.Loading -> VerificationState.Verifying
    }

  ClerkThemedAuthScaffold(
    modifier = modifier,
    title = title,
    hasLogo = false,
    identifier = field.value.formattedAsPhoneNumberIfPossible,
    snackbarHostState = snackbarHostState,
  ) {
    ClerkLinearProgressIndicator(progress = 0)
    Spacers.Vertical.Spacer32()
    ClerkCodeInputField(
      verificationState = verificationState,
      onTextChange = {
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
  SignUpCodeView(field = Field.Phone("3012370655"))
}

sealed interface Field {
  val value: String

  data class Phone(override val value: String) : Field

  data class Email(override val value: String) : Field
}
