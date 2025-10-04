package com.clerk.ui.userprofile.mfa

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark

@Composable
private fun UserProfileAddMfaView(viewType: ViewType) {
  UserProfileAddMfaViewImpl(viewType = viewType)
}

@Composable
private fun UserProfileAddMfaViewImpl(viewType: ViewType) {
  when (viewType) {
    ViewType.SMS ->
      UserProfileMfaAddSmsView(onClickUsePhoneNumber = {}, onReserveForSecondFactorSuccess = {})
    ViewType.AuthenticatorApp -> TODO()
  }
}

internal enum class ViewType {
  SMS,
  AuthenticatorApp,
}

@PreviewLightDark
@Composable
private fun Preview() {
  UserProfileAddMfaView(viewType = ViewType.SMS)
}
