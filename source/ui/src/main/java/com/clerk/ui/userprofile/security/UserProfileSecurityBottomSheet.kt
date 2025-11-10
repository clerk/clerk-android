package com.clerk.ui.userprofile.security

import androidx.compose.runtime.Composable
import com.clerk.api.Clerk
import com.clerk.ui.userprofile.account.UserProfileDeleteAccountConfirmationView
import com.clerk.ui.userprofile.mfa.UserProfileAddMfaBottomSheetContent
import com.clerk.ui.userprofile.mfa.UserProfileAddMfaView
import com.clerk.ui.userprofile.mfa.ViewType
import com.clerk.ui.userprofile.phone.UserProfileAddPhoneView
import com.clerk.ui.userprofile.security.password.PasswordAction
import com.clerk.ui.userprofile.security.password.UserProfileCurrentPasswordView
import com.clerk.ui.userprofile.security.password.UserProfileNewPasswordView
import com.clerk.ui.userprofile.verify.Mode
import com.clerk.ui.userprofile.verify.UserProfileVerifyBottomSheetContent
import com.clerk.ui.userprofile.verify.VerifyBottomSheetMode
import kotlinx.collections.immutable.toImmutableList

internal sealed interface BottomSheetType {
  data object DeleteAccount : BottomSheetType

  data class CurrentPassword(val passwordAction: PasswordAction) : BottomSheetType

  data class NewPassword(val currentPassword: String?, val passwordAction: PasswordAction) :
    BottomSheetType

  data object ChooseMfa : BottomSheetType

  data class AddMfa(val viewType: ViewType) : BottomSheetType

  data class BackupCodes(val codes: List<String>) : BottomSheetType

  data object AddPhoneNumber : BottomSheetType

  data class Verify(val mode: Mode) : BottomSheetType
}

@Composable
internal fun BottomSheetBody(currentSheetType: BottomSheetType, callbacks: BottomSheetCallbacks) {
  when (currentSheetType) {
    is BottomSheetType.AddMfa -> AddMfaSheet(currentSheetType, callbacks)
    BottomSheetType.ChooseMfa -> ChooseMfaSheet(callbacks)
    BottomSheetType.DeleteAccount -> DeleteAccountSheet(callbacks)
    is BottomSheetType.CurrentPassword -> CurrentPasswordSheet(currentSheetType, callbacks)
    is BottomSheetType.NewPassword -> NewPasswordSheet(currentSheetType, callbacks)
    is BottomSheetType.BackupCodes -> BackupCodesSheet(currentSheetType, callbacks)
    BottomSheetType.AddPhoneNumber -> AddPhoneNumberSheet(callbacks)
    is BottomSheetType.Verify -> VerifySheet(currentSheetType, callbacks)
  }
}

@Composable
internal fun AddMfaSheet(type: BottomSheetType.AddMfa, callbacks: BottomSheetCallbacks) {
  UserProfileAddMfaView(
    type.viewType,
    callbacks =
      com.clerk.ui.userprofile.mfa.AddMfaCallbacks(
        onDismiss = callbacks.onDismiss,
        onNavigateToBackupCodes = callbacks.onNavigateToBackupCodes,
        onError = { message -> callbacks.onError(message) },
        onAddPhoneNumber = callbacks.onAddPhoneNumber,
        onVerify = callbacks.onVerify,
      ),
  )
}

@Composable
internal fun ChooseMfaSheet(callbacks: BottomSheetCallbacks) {
  UserProfileAddMfaBottomSheetContent(
    mfaPhoneCodeIsEnabled = Clerk.mfaPhoneCodeIsEnabled,
    mfaAuthenticatorAppIsEnabled = Clerk.mfaAuthenticatorAppIsEnabled,
    onClick = callbacks.onClickMfaType,
  )
}

@Composable
internal fun DeleteAccountSheet(callbacks: BottomSheetCallbacks) {
  UserProfileDeleteAccountConfirmationView(
    onClose = callbacks.onDismiss,
    onError = callbacks.onError,
  )
}

@Composable
internal fun CurrentPasswordSheet(
  type: BottomSheetType.CurrentPassword,
  callbacks: BottomSheetCallbacks,
) {
  UserProfileCurrentPasswordView(
    type.passwordAction,
    onClosePressed = callbacks.onDismiss,
    onCurrentPasswordEntered = { currentPassword, passwordAction ->
      callbacks.onCurrentPasswordEntered(currentPassword, passwordAction)
    },
  )
}

@Composable
internal fun NewPasswordSheet(type: BottomSheetType.NewPassword, callbacks: BottomSheetCallbacks) {
  UserProfileNewPasswordView(
    currentPassword = type.currentPassword,
    passwordAction = type.passwordAction,
    onError = callbacks.onError,
    onDismiss = callbacks.onDismiss,
    onPasswordChanged = { callbacks.onDismiss() },
  )
}

@Composable
internal fun BackupCodesSheet(type: BottomSheetType.BackupCodes, callbacks: BottomSheetCallbacks) {
  BackupCodesView(codes = type.codes.toImmutableList(), onDismiss = callbacks.onDismiss)
}

@Composable
internal fun AddPhoneNumberSheet(callbacks: BottomSheetCallbacks) {
  UserProfileAddPhoneView(onVerify = callbacks.onVerify, onDismiss = callbacks.onDismiss)
}

@Composable
internal fun VerifySheet(type: BottomSheetType.Verify, callbacks: BottomSheetCallbacks) {
  UserProfileVerifyBottomSheetContent(
    mode = type.mode.toVerifyMode(),
    onVerified = { codes ->
      if (codes.isNullOrEmpty()) {
        callbacks.onDismiss()
      } else {
        callbacks.onNavigateToBackupCodes(codes)
      }
    },
    onError = callbacks.onError,
    onDismiss = callbacks.onDismiss,
  )
}

internal fun Mode.toVerifyMode(): VerifyBottomSheetMode {
  return when (this) {
    is Mode.Email -> VerifyBottomSheetMode.Email(emailAddress)
    is Mode.Phone -> VerifyBottomSheetMode.Phone(phoneNumber)
    Mode.Totp -> VerifyBottomSheetMode.Totp
  }
}

internal data class BottomSheetCallbacks(
  val onClickMfaType: (ViewType) -> Unit,
  val onCurrentPasswordEntered: (String, PasswordAction) -> Unit,
  val onDismiss: () -> Unit,
  val onError: (String?) -> Unit,
  val onAddPhoneNumber: () -> Unit,
  val onNavigateToBackupCodes: (List<String>) -> Unit,
  val onVerify: (Mode) -> Unit,
)
