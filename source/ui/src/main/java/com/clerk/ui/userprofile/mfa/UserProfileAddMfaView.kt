package com.clerk.ui.userprofile.mfa

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.ui.userprofile.totp.UserProfileMfaAddTotpView
import kotlinx.serialization.Serializable

@Composable
internal fun UserProfileAddMfaView(
  viewType: ViewType,
  onDismiss: () -> Unit,
  onNavigateToBackupCodes: (List<String>) -> Unit,
  onError: (String) -> Unit,
  onAddPhoneNumber: () -> Unit,
) {
  UserProfileAddMfaViewImpl(
    viewType = viewType,
    onError = onError,
    onAddPhoneNumber = onAddPhoneNumber,
    onNavigateToBackupCodes = onNavigateToBackupCodes,
    onDismiss = onDismiss,
  )
}

@Composable
private fun UserProfileAddMfaViewImpl(
  viewType: ViewType,
  onDismiss: () -> Unit,
  onNavigateToBackupCodes: (List<String>) -> Unit,
  onError: (String) -> Unit,
  onAddPhoneNumber: () -> Unit,
) {
  when (viewType) {
    ViewType.Sms ->
      UserProfileMfaAddSmsView(
        onDismiss = onDismiss,
        onNavigateToBackupCodes = onNavigateToBackupCodes,
        onError = onError,
        onAddPhoneNumber = onAddPhoneNumber,
      )
    ViewType.AuthenticatorApp -> UserProfileMfaAddTotpView()
  }
}

@Serializable
internal sealed interface ViewType {

  @Serializable data object Sms : ViewType

  @Serializable data object AuthenticatorApp : ViewType
}

@PreviewLightDark
@Composable
private fun Preview() {
  UserProfileAddMfaView(
    viewType = ViewType.Sms,
    onNavigateToBackupCodes = {},
    onDismiss = {},
    onError = {},
    onAddPhoneNumber = {},
  )
}
