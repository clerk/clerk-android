package com.clerk.ui.userprofile.mfa

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark

@Composable
private fun UserProfileAddMfaView(viewType: ViewType, modifier: Modifier = Modifier) {
  UserProfileAddMfaViewImpl(viewType = viewType, modifier = modifier)
}

@Composable
private fun UserProfileAddMfaViewImpl(viewType: ViewType, modifier: Modifier = Modifier) {}

internal enum class ViewType {
  SMS,
  AuthenticatorApp,
}

@PreviewLightDark
@Composable
private fun Preview() {
  UserProfileAddMfaView(viewType = ViewType.SMS)
}
