package com.clerk.ui.userprofile.detail

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import com.clerk.api.emailaddress.EmailAddress
import com.clerk.api.phonenumber.PhoneNumber
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.userprofile.LocalUserProfileState
import com.clerk.ui.userprofile.UserProfileDestination
import com.clerk.ui.userprofile.connectedaccount.UserProfileAddConnectedAccountView
import com.clerk.ui.userprofile.email.UserProfileAddEmailView
import com.clerk.ui.userprofile.phone.UserProfileAddPhoneView
import com.clerk.ui.userprofile.verify.UserProfileVerifyBottomSheetContent
import com.clerk.ui.userprofile.verify.VerifyBottomSheetMode

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun UserProfileDetailBottomSheet(
  bottomSheetType: BottomSheetMode,
  onDismissRequest: () -> Unit,
  onVerify: (BottomSheetMode) -> Unit,
  onError: (String) -> Unit,
) {
  ModalBottomSheet(
    onDismissRequest = onDismissRequest,
    containerColor = ClerkMaterialTheme.colors.background,
  ) {
    BottomSheetContent(
      bottomSheetType = bottomSheetType,
      onDismissRequest = onDismissRequest,
      onVerify = onVerify,
      onError = onError,
    )
  }
}

@Composable
private fun BottomSheetContent(
  bottomSheetType: BottomSheetMode,
  onDismissRequest: () -> Unit,
  onVerify: (BottomSheetMode) -> Unit,
  onError: (String) -> Unit,
) {
  when (bottomSheetType) {
    BottomSheetMode.ExternalAccount -> ExternalAccountSheet(onDismissRequest)
    BottomSheetMode.PhoneNumber -> PhoneNumberSheet(onDismissRequest, onError, onVerify)
    BottomSheetMode.EmailAddress -> EmailAddressSheet(onDismissRequest, onError, onVerify)
    is BottomSheetMode.VerifyEmailAddress ->
      VerifyEmailSheet(bottomSheetType.emailAddress, onDismissRequest, onError)
    is BottomSheetMode.VerifyPhoneNumber ->
      VerifyPhoneSheet(bottomSheetType.phoneNumber, onDismissRequest, onError)
  }
}

@Composable
private fun ExternalAccountSheet(onDismiss: () -> Unit) {
  UserProfileAddConnectedAccountView(onBackPressed = onDismiss)
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
) {
  val userProfileState = LocalUserProfileState.current
  UserProfileVerifyBottomSheetContent(
    mode = VerifyBottomSheetMode.Email(email),
    onError = onError,
    onVerified = { backupCodes -> handleVerified(backupCodes, onDismiss, userProfileState) },
    onDismiss = onDismiss,
  )
}

@Composable
private fun VerifyPhoneSheet(phone: PhoneNumber, onDismiss: () -> Unit, onError: (String) -> Unit) {
  val userProfileState = LocalUserProfileState.current
  UserProfileVerifyBottomSheetContent(
    mode = VerifyBottomSheetMode.Phone(phone),
    onError = onError,
    onVerified = { backupCodes -> handleVerified(backupCodes, onDismiss, userProfileState) },
    onDismiss = onDismiss,
  )
}

private fun handleVerified(
  backupCodes: List<String>?,
  onDismiss: () -> Unit,
  userProfileState: com.clerk.ui.userprofile.UserProfileState,
) {
  if (backupCodes == null) {
    onDismiss()
  } else {
    userProfileState.navigateTo(UserProfileDestination.BackupCodeView(codes = backupCodes))
  }
}
