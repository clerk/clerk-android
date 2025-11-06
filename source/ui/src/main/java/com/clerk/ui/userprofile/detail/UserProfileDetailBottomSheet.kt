package com.clerk.ui.userprofile.detail

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.clerk.api.emailaddress.EmailAddress
import com.clerk.api.phonenumber.PhoneNumber
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.userprofile.connectedaccount.UserProfileAddConnectedAccountView
import com.clerk.ui.userprofile.email.UserProfileAddEmailView
import com.clerk.ui.userprofile.phone.UserProfileAddPhoneView
import com.clerk.ui.userprofile.security.BackupCodesView
import com.clerk.ui.userprofile.verify.UserProfileVerifyBottomSheetContent
import com.clerk.ui.userprofile.verify.VerifyBottomSheetMode
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun UserProfileDetailBottomSheet(
  bottomSheetType: BottomSheetMode,
  onDismissRequest: () -> Unit,
  onVerify: (BottomSheetMode) -> Unit,
  onError: (String) -> Unit,
  onShowBackupCodes: (List<String>) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState()
  val scope = rememberCoroutineScope()

  fun animatedDismiss() {
    scope.launch {
      sheetState.hide()
      onDismissRequest()
    }
  }

  ModalBottomSheet(
    scrimColor = ClerkMaterialTheme.colors.neutral.copy(alpha = .5f),
    onDismissRequest = { animatedDismiss() },
    containerColor = ClerkMaterialTheme.colors.background,
    sheetState = sheetState,
  ) {
    BottomSheetContent(
      bottomSheetType = bottomSheetType,
      onDismissRequest = { animatedDismiss() },
      onVerify = onVerify,
      onError = onError,
      onShowBackupCodes = onShowBackupCodes,
    )
  }
}

@Composable
private fun BottomSheetContent(
  bottomSheetType: BottomSheetMode,
  onDismissRequest: () -> Unit,
  onVerify: (BottomSheetMode) -> Unit,
  onError: (String) -> Unit,
  onShowBackupCodes: (List<String>) -> Unit,
) {
  when (bottomSheetType) {
    BottomSheetMode.ExternalAccount -> ExternalAccountSheet(onDismissRequest)
    BottomSheetMode.PhoneNumber -> PhoneNumberSheet(onDismissRequest, onError, onVerify)
    BottomSheetMode.EmailAddress -> EmailAddressSheet(onDismissRequest, onError, onVerify)
    is BottomSheetMode.VerifyEmailAddress ->
      VerifyEmailSheet(bottomSheetType.emailAddress, onDismissRequest, onError, onShowBackupCodes)
    is BottomSheetMode.VerifyPhoneNumber ->
      VerifyPhoneSheet(bottomSheetType.phoneNumber, onDismissRequest, onError, onShowBackupCodes)

    is BottomSheetMode.BackupCodes ->
      BackupCodesSheet(
        codes = bottomSheetType.backupCodes.toImmutableList(),
        onDismiss = onDismissRequest,
      )
  }
}

@Composable
private fun ExternalAccountSheet(onDismiss: () -> Unit) {
  UserProfileAddConnectedAccountView(onBackPressed = onDismiss)
}

@Composable
private fun BackupCodesSheet(codes: ImmutableList<String>, onDismiss: () -> Unit) {
  BackupCodesView(codes = codes, onDismiss = onDismiss)
}

@Composable
private fun PhoneNumberSheet(
  onDismiss: () -> Unit,
  onError: (String) -> Unit,
  onVerify: (BottomSheetMode) -> Unit,
) {
  UserProfileAddPhoneView(
    onDismiss = onDismiss,
    onError = onError,
    onVerify = { onVerify(BottomSheetMode.VerifyPhoneNumber(it.phoneNumber)) },
  )
}

@Composable
private fun EmailAddressSheet(
  onDismiss: () -> Unit,
  onError: (String) -> Unit,
  onVerify: (BottomSheetMode) -> Unit,
) {
  UserProfileAddEmailView(
    onDismiss = onDismiss,
    onError = onError,
    onVerify = { onVerify(BottomSheetMode.VerifyEmailAddress(it.emailAddress)) },
  )
}

@Composable
private fun VerifyEmailSheet(
  email: EmailAddress,
  onDismiss: () -> Unit,
  onError: (String) -> Unit,
  onShowBackupCodes: (List<String>) -> Unit,
) {
  UserProfileVerifyBottomSheetContent(
    mode = VerifyBottomSheetMode.Email(email),
    onError = onError,
    onVerified = { backupCodes ->
      handleVerified(
        backupCodes = backupCodes,
        onDismiss = onDismiss,
        onShowBackupCodes = onShowBackupCodes,
      )
    },
    onDismiss = onDismiss,
  )
}

@Composable
private fun VerifyPhoneSheet(
  phone: PhoneNumber,
  onDismiss: () -> Unit,
  onError: (String) -> Unit,
  onShowBackupCodes: (List<String>) -> Unit,
) {
  UserProfileVerifyBottomSheetContent(
    mode = VerifyBottomSheetMode.Phone(phone),
    onError = onError,
    onVerified = { backupCodes ->
      handleVerified(
        backupCodes = backupCodes,
        onDismiss = onDismiss,
        onShowBackupCodes = onShowBackupCodes,
      )
    },
    onDismiss = onDismiss,
  )
}

private fun handleVerified(
  backupCodes: List<String>?,
  onDismiss: () -> Unit,
  onShowBackupCodes: (List<String>) -> Unit,
) {
  if (backupCodes == null) {
    onDismiss()
  } else {
    onShowBackupCodes(backupCodes)
  }
}
