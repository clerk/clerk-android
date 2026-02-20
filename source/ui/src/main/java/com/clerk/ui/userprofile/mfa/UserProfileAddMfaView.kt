package com.clerk.ui.userprofile.mfa

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.ui.userprofile.totp.UserProfileMfaAddTotpView
import com.clerk.ui.userprofile.verify.Mode
import kotlinx.serialization.Serializable

@Composable
internal fun UserProfileAddMfaView(
  viewType: ViewType,
  callbacks: AddMfaCallbacks,
  modifier: Modifier = Modifier,
) {
  UserProfileAddMfaViewImpl(viewType = viewType, callbacks = callbacks, modifier = modifier)
}

@Composable
private fun UserProfileAddMfaViewImpl(
  viewType: ViewType,
  callbacks: AddMfaCallbacks,
  modifier: Modifier = Modifier,
) {
  when (viewType) {
    ViewType.Sms ->
      UserProfileMfaAddSmsView(
        modifier = modifier,
        onDismiss = callbacks.onDismiss,
        onNavigateToBackupCodes = callbacks.onNavigateToBackupCodes,
        onError = callbacks.onError,
        onAddPhoneNumber = callbacks.onAddPhoneNumber,
      )
    ViewType.AuthenticatorApp ->
      UserProfileMfaAddTotpView(
        modifier = modifier,
        onVerify = callbacks.onVerify,
        onDismiss = callbacks.onDismiss,
      )
  }
}

@Serializable
internal sealed interface ViewType {

  @Serializable data object Sms : ViewType

  @Serializable data object AuthenticatorApp : ViewType
}

internal data class AddMfaCallbacks(
  val onDismiss: () -> Unit,
  val onNavigateToBackupCodes: (List<String>) -> Unit,
  val onError: (String) -> Unit,
  val onAddPhoneNumber: () -> Unit,
  val onVerify: (Mode) -> Unit,
)

@PreviewLightDark
@Composable
private fun Preview() {
  UserProfileAddMfaView(
    viewType = ViewType.Sms,
    callbacks =
      AddMfaCallbacks(
        onDismiss = {},
        onNavigateToBackupCodes = {},
        onError = {},
        onAddPhoneNumber = {},
        onVerify = {},
      ),
  )
}
