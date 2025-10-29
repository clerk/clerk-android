package com.clerk.ui.userprofile.mfa

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.ui.userprofile.totp.UserProfileMfaAddTotpView
import kotlinx.serialization.Serializable

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

@Serializable
internal sealed interface ViewType {

  @Serializable data object Sms : ViewType

  @Serializable data object AuthenticatorApp : ViewType
}

@PreviewLightDark
@Composable
private fun Preview() {
  UserProfileAddMfaView(viewType = ViewType.Sms)
}
