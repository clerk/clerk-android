package com.clerk.ui.userprofile.mfa

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.ui.userprofile.totp.UserProfileMfaAddTotpView

@Composable
internal fun UserProfileAddMfaView(viewType: ViewType) {
  UserProfileAddMfaViewImpl(viewType = viewType)
}

@Composable
private fun UserProfileAddMfaViewImpl(viewType: ViewType) {
  when (viewType) {
    ViewType.Sms ->
      UserProfileMfaAddSmsView(onClickUsePhoneNumber = {}, onReserveForSecondFactorSuccess = {})
    ViewType.AuthenticatorApp -> UserProfileMfaAddTotpView()
  }
}

internal sealed interface ViewType {
  data object Sms : ViewType

  data object AuthenticatorApp : ViewType
}

@PreviewLightDark
@Composable
private fun Preview() {
  UserProfileAddMfaView(viewType = ViewType.Sms)
}
