package com.clerk.ui.userprofile.verify

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.emailaddress.EmailAddress
import com.clerk.api.network.model.backupcodes.BackupCodeResource
import com.clerk.api.phonenumber.PhoneNumber
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.input.ClerkCodeInputField
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.signin.code.VerificationState as CodeVerificationState
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.userprofile.common.BottomSheetTopBar
import kotlinx.serialization.Serializable

@Composable
fun UserProfileVerifyBottomSheetContent(
  mode: VerifyBottomSheetMode,
  onVerified: (BackupCodeResource?) -> Unit,
  onError: (String) -> Unit,
  modifier: Modifier = Modifier,
  onDismiss: () -> Unit,
) {
  UserProfileVerifyBottomSheetContentImpl(
    mode = mode,
    modifier = modifier,
    onVerified = onVerified,
    onDismiss = onDismiss,
    onError = onError,
  )
}

@Composable
private fun UserProfileVerifyBottomSheetContentImpl(
  mode: VerifyBottomSheetMode,
  onError: (String) -> Unit,
  onVerified: (BackupCodeResource?) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: UserProfileVerifyViewModel = viewModel(),
  onDismiss: () -> Unit,
) {

  val state by viewModel.state.collectAsStateWithLifecycle()
  val verificationTextState by viewModel.verificationTextState.collectAsStateWithLifecycle()
  val errorMessage = (state as? UserProfileVerifyViewModel.AuthState.Error)?.error

  LaunchedEffect(mode) {
    viewModel.resetState()
    prepareCode(mode, viewModel)
  }

  LaunchedEffect(verificationTextState) {
    if (verificationTextState is UserProfileVerifyViewModel.VerificationTextState.Verified) {
      (verificationTextState as UserProfileVerifyViewModel.VerificationTextState.Verified)
        .backupCodes
        ?.let {} ?: run {}
      viewModel.resetState()
    }
  }

  Column(modifier = Modifier.fillMaxWidth().then(modifier)) {
    BottomSheetTopBar(title = mode.title(), onClosePressed = onDismiss)
    Spacers.Vertical.Spacer12()
    Text(
      modifier = Modifier.padding(horizontal = dp24),
      text = mode.instructionString(),
      style = ClerkMaterialTheme.typography.bodyMedium,
      color = ClerkMaterialTheme.colors.mutedForeground,
    )
    Spacers.Vertical.Spacer24()
    ClerkCodeInputField(
      onTextChange = {
        if (it.length == 6) {
          when (mode) {
            is VerifyBottomSheetMode.Email -> viewModel.attemptEmailAddress(mode.emailAddress, it)
            is VerifyBottomSheetMode.Phone -> viewModel.attemptPhoneNumber(mode.phoneNumber, it)
            is VerifyBottomSheetMode.Totp -> viewModel.attemptTotp(it)
          }
        }
      },
      onClickResend = { prepareCode(mode, viewModel) },
      showResend = mode.showResend(),
      verificationState = verificationTextState.getTextVerificationState(),
    )
  }
}

private fun UserProfileVerifyViewModel.VerificationTextState.getTextVerificationState():
  CodeVerificationState {
  return when (this) {
    UserProfileVerifyViewModel.VerificationTextState.Default -> CodeVerificationState.Default
    is UserProfileVerifyViewModel.VerificationTextState.Error -> CodeVerificationState.Error
    is UserProfileVerifyViewModel.VerificationTextState.Verified -> CodeVerificationState.Success
    UserProfileVerifyViewModel.VerificationTextState.Verifying -> CodeVerificationState.Verifying
  }
}

private fun prepareCode(mode: VerifyBottomSheetMode, viewModel: UserProfileVerifyViewModel) {
  when (mode) {
    is VerifyBottomSheetMode.Email -> viewModel.prepareEmailAddress(mode.emailAddress)
    is VerifyBottomSheetMode.Phone -> viewModel.preparePhoneNumber(mode.phoneNumber)
    VerifyBottomSheetMode.Totp -> {}
  }
}

@PreviewLightDark
@Composable
private fun Preview() {

  ClerkMaterialTheme {
    UserProfileVerifyBottomSheetContent(
      mode =
        VerifyBottomSheetMode.Email(
          emailAddress = EmailAddress(id = "id", emailAddress = "user@email.com")
        ),
      onDismiss = {},
      onVerified = {},
      onError = {},
    )
  }
}

@PreviewLightDark
@Composable
private fun PreviewTotp() {
  ClerkMaterialTheme {
    UserProfileVerifyBottomSheetContent(
      mode = VerifyBottomSheetMode.Totp,
      onVerified = {},
      onDismiss = {},
      onError = {},
    )
  }
}

private fun VerifyBottomSheetMode.showResend(): Boolean {
  return when (this) {
    is VerifyBottomSheetMode.Email -> true
    is VerifyBottomSheetMode.Phone -> true
    VerifyBottomSheetMode.Totp -> false
  }
}

@Composable
private fun VerifyBottomSheetMode.title(): String {
  return when (this) {
    is VerifyBottomSheetMode.Email -> stringResource(R.string.verify_email_address)
    is VerifyBottomSheetMode.Phone -> stringResource(R.string.verify_phone_number)
    VerifyBottomSheetMode.Totp -> stringResource(R.string.verify_authenticator_app)
  }
}

@Composable
private fun VerifyBottomSheetMode.instructionString(): String {
  return when (this) {
    is VerifyBottomSheetMode.Email ->
      stringResource(R.string.enter_the_verification_code_sent_to, this.emailAddress.emailAddress)
    is VerifyBottomSheetMode.Phone ->
      stringResource(R.string.enter_the_verification_code_sent_to, this.phoneNumber.phoneNumber)
    VerifyBottomSheetMode.Totp ->
      stringResource(R.string.enter_the_verification_code_from_your_authenticator_application)
  }
}

@Immutable
@Serializable
sealed interface VerifyBottomSheetMode {
  @Serializable data class Email(val emailAddress: EmailAddress) : VerifyBottomSheetMode

  @Serializable data class Phone(val phoneNumber: PhoneNumber) : VerifyBottomSheetMode

  @Serializable data object Totp : VerifyBottomSheetMode
}
