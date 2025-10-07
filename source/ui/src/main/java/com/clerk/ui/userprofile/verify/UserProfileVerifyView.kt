package com.clerk.ui.userprofile.verify

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.emailaddress.EmailAddress
import com.clerk.api.phonenumber.PhoneNumber
import com.clerk.ui.R
import com.clerk.ui.core.input.ClerkCodeInputField
import com.clerk.ui.core.scaffold.ClerkThemedProfileScaffold
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
fun UserProfileVerifyView(mode: Mode, modifier: Modifier = Modifier) {

  ClerkThemedProfileScaffold(
    modifier = modifier,
    title = mode.title(),
    hasBackButton = mode.hasBackButton(),
  ) {
    Text(
      text = mode.instructionString(),
      style = ClerkMaterialTheme.typography.bodyMedium,
      color = ClerkMaterialTheme.colors.mutedForeground,
    )
    Spacers.Vertical.Spacer24()
    ClerkCodeInputField(onTextChange = {}, onClickResend = {}, showResend = mode.showResend())
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  UserProfileVerifyView(
    mode = Mode.Email(emailAddress = EmailAddress(id = "id", emailAddress = "user@email.com"))
  )
}

sealed interface VerificationState {
  data object Default : VerificationState

  data object Verifying : VerificationState

  data object Success : VerificationState

  data class Error(val message: String) : VerificationState
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
