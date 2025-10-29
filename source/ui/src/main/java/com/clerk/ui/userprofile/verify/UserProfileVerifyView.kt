package com.clerk.ui.userprofile.verify

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.emailaddress.EmailAddress
import com.clerk.api.phonenumber.PhoneNumber
import com.clerk.ui.R
import com.clerk.ui.core.input.ClerkCodeInputField
import com.clerk.ui.core.scaffold.ClerkThemedProfileScaffold
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.signin.code.VerificationState as CodeVerificationState
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
fun UserProfileVerifyView(mode: Mode, modifier: Modifier = Modifier) {
  UserProfileVerifyViewImpl(mode = mode, modifier = modifier)
}

@Composable
private fun UserProfileVerifyViewImpl(
  mode: Mode,
  modifier: Modifier = Modifier,
  viewModel: UserProfileVerifyViewModel = viewModel(),
) {

  val state by viewModel.state.collectAsStateWithLifecycle()
  val verificationTextState by viewModel.verificationTextState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  LaunchedEffect(mode) { prepareCode(mode, viewModel) }
  val errorMessage: String? =
    when (val s = state) {
      is UserProfileVerifyViewModel.AuthState.Error ->
        s.error ?: context.getString(R.string.something_went_wrong_please_try_again)
      else -> null
    }

  ClerkThemedProfileScaffold(
    modifier = modifier,
    title = mode.title(),
    errorMessage = errorMessage,
    hasBackButton = mode.hasBackButton(),
    content = {
      Text(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        text = mode.instructionString(),
        style = ClerkMaterialTheme.typography.bodyMedium,
        color = ClerkMaterialTheme.colors.mutedForeground,
      )
      Spacers.Vertical.Spacer24()
      ClerkCodeInputField(
        onTextChange = {
          if (it.length == 6) {
            when (mode) {
              is Mode.Email -> viewModel.attemptEmailAddress(mode.emailAddress, it)
              is Mode.Phone -> viewModel.attemptPhoneNumber(mode.phoneNumber, it)
              Mode.Totp -> viewModel.attemptTotp(it)
            }
          }
        },
        onClickResend = { prepareCode(mode, viewModel) },
        showResend = mode.showResend(),
        verificationState = verificationTextState.getTextVerificationState(),
      )
    },
  )
}

private fun UserProfileVerifyViewModel.VerificationTextState.getTextVerificationState():
  CodeVerificationState {
  return when (this) {
    UserProfileVerifyViewModel.VerificationTextState.Default -> CodeVerificationState.Default
    is UserProfileVerifyViewModel.VerificationTextState.Error -> CodeVerificationState.Error
    UserProfileVerifyViewModel.VerificationTextState.Verified -> CodeVerificationState.Success
    UserProfileVerifyViewModel.VerificationTextState.Verifying -> CodeVerificationState.Verifying
  }
}

private fun prepareCode(mode: Mode, viewModel: UserProfileVerifyViewModel) {
  when (mode) {
    is Mode.Email -> viewModel.prepareEmailAddress(mode.emailAddress)
    is Mode.Phone -> viewModel.preparePhoneNumber(mode.phoneNumber)
    Mode.Totp -> {}
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  ClerkMaterialTheme {
    UserProfileVerifyView(
      mode = Mode.Email(emailAddress = EmailAddress(id = "id", emailAddress = "user@email.com"))
    )
  }
}

private fun Mode.showResend(): Boolean {
  return when (this) {
    is Mode.Email -> true
    is Mode.Phone -> true
    Mode.Totp -> false
  }
}

private fun Mode.hasBackButton(): Boolean {
  return when (this) {
    is Mode.Email -> true
    is Mode.Phone -> true
    Mode.Totp -> false
  }
}

@Composable
private fun Mode.title(): String {
  return when (this) {
    is Mode.Email -> stringResource(R.string.verify_email_address)
    is Mode.Phone -> stringResource(R.string.verify_phone_number)
    Mode.Totp -> stringResource(R.string.verify_authenticator_app)
  }
}

@Composable
private fun Mode.instructionString(): String {
  return when (this) {
    is Mode.Email ->
      stringResource(R.string.enter_the_verification_code_sent_to, this.emailAddress.emailAddress)
    is Mode.Phone ->
      stringResource(R.string.enter_the_verification_code_sent_to, this.phoneNumber.phoneNumber)
    Mode.Totp ->
      stringResource(R.string.enter_the_verification_code_from_your_authenticator_application)
  }
}

@Immutable
sealed interface Mode {
  data class Email(val emailAddress: EmailAddress) : Mode

  data class Phone(val phoneNumber: PhoneNumber) : Mode

  data object Totp : Mode
}
