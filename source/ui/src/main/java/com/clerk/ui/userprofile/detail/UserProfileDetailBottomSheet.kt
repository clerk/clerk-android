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
import com.clerk.ui.userprofile.email.UserProfileAddEmailViewBottomSheetContent
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
  onShowBackupCodes: (List<String>) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val scope = rememberCoroutineScope()

  fun programmaticDismiss() {
    scope.launch {
      sheetState.hide()
    }.invokeOnCompletion {
      onDismissRequest()
    }
  }

  ModalBottomSheet(
    scrimColor = ClerkMaterialTheme.colors.neutral.copy(alpha = .5f),
    onDismissRequest = onDismissRequest,
    containerColor = ClerkMaterialTheme.colors.background,
    sheetState = sheetState,
  ) {
    BottomSheetContent(
      bottomSheetType = bottomSheetType,
      onDismissRequest = { programmaticDismiss() },
      onVerify = onVerify,
      onShowBackupCodes = onShowBackupCodes,
    )
  }
}

@Composable
private fun BottomSheetContent(
  bottomSheetType: BottomSheetMode,
  onDismissRequest: () -> Unit,
  onVerify: (BottomSheetMode) -> Unit,
  onShowBackupCodes: (List<String>) -> Unit,
) {
  when (bottomSheetType) {
    BottomSheetMode.ExternalAccount -> ExternalAccountSheet(onDismissRequest)
    BottomSheetMode.PhoneNumber -> PhoneNumberSheet(onDismissRequest, onVerify)
    BottomSheetMode.EmailAddress -> EmailAddressSheet(onDismissRequest, onVerify)
    is BottomSheetMode.VerifyEmailAddress ->
      VerifyEmailSheet(bottomSheetType.emailAddress, onDismissRequest, onShowBackupCodes)
    is BottomSheetMode.VerifyPhoneNumber ->
      VerifyPhoneSheet(bottomSheetType.phoneNumber, onDismissRequest, onShowBackupCodes)

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
private fun PhoneNumberSheet(onDismiss: () -> Unit, onVerify: (BottomSheetMode) -> Unit) {
  UserProfileAddPhoneView(
    onDismiss = onDismiss,
    onVerify = { onVerify(BottomSheetMode.VerifyPhoneNumber(it.phoneNumber)) },
  )
}

@Composable
private fun EmailAddressSheet(onDismiss: () -> Unit, onVerify: (BottomSheetMode) -> Unit) {
  UserProfileAddEmailViewBottomSheetContent(
    onDismiss = onDismiss,
    onVerify = { onVerify(BottomSheetMode.VerifyEmailAddress(it.emailAddress)) },
  )
}

@Composable
private fun VerifyEmailSheet(
  email: EmailAddress,
  onDismiss: () -> Unit,
  onShowBackupCodes: (List<String>) -> Unit,
) {
  UserProfileVerifyBottomSheetContent(
    mode = VerifyBottomSheetMode.Email(email),
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
  onShowBackupCodes: (List<String>) -> Unit,
) {
  UserProfileVerifyBottomSheetContent(
    mode = VerifyBottomSheetMode.Phone(phone),
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
