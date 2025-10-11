package com.clerk.ui.userprofile.mfa

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.network.model.totp.TOTPResource
import com.clerk.ui.userprofile.totp.UserProfileMfaAddTotpView

@Composable
private fun UserProfileAddMfaView(viewType: ViewType) {
  UserProfileAddMfaViewImpl(viewType = viewType)
}

@Composable
private fun UserProfileAddMfaViewImpl(viewType: ViewType) {
  when (viewType) {
    ViewType.Sms ->
      UserProfileMfaAddSmsView(onClickUsePhoneNumber = {}, onReserveForSecondFactorSuccess = {})
    is ViewType.AuthenticatorApp -> UserProfileMfaAddTotpView(viewType.totp)
  }
}

internal sealed interface ViewType {
  data object Sms : ViewType

  data class AuthenticatorApp(val totp: TOTPResource) : ViewType
}

@PreviewLightDark
@Composable
private fun Preview() {
  UserProfileAddMfaView(viewType = ViewType.Sms)
}
