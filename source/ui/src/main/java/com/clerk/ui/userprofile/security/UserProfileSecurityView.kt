package com.clerk.ui.userprofile.security

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.Clerk
import com.clerk.api.user.activeSessions
import com.clerk.ui.R
import com.clerk.ui.core.scaffold.ClerkThemedProfileScaffold
import com.clerk.ui.userprofile.security.delete.UserProfileDeleteAccountSection
import com.clerk.ui.userprofile.security.device.UserProfileDevicesSection
import com.clerk.ui.userprofile.security.mfa.UserProfileMfaSection
import com.clerk.ui.userprofile.security.passkey.UserProfilePasskeySection
import com.clerk.ui.userprofile.security.password.UserProfilePasswordSection

@Composable
fun UserProfileSecurityView(modifier: Modifier = Modifier) {
//  UserProfileSecurityViewImpl(hasActiveSessions = Clerk.user?.activeSessions().i)
}

@Composable
private fun UserProfileSecurityViewImpl(
  modifier: Modifier = Modifier,
  hasActiveSessions: Boolean = false,
  isPasswordEnabled: Boolean = false,
  isPasskeyEnabled: Boolean = false,
  isMfaEnabled: Boolean = false,
  isDeleteSelfEnabled: Boolean = false,
) {
  ClerkThemedProfileScaffold(
    modifier = modifier,
    hasBackButton = true,
    title = stringResource(R.string.security),
  ) {
    if (isPasswordEnabled) {
      UserProfilePasswordSection(onAction = {})
    }
    if (isPasskeyEnabled) {
      UserProfilePasskeySection() {}
    }
    if (isMfaEnabled) {
      UserProfileMfaSection(onRemove = {}, onAdd = {})
    }
    if (hasActiveSessions) {
      UserProfileDevicesSection {}
    }
    if (isDeleteSelfEnabled) {
      UserProfileDeleteAccountSection(onDeleteAccount = {})
    }
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  UserProfileSecurityViewImpl(
    hasActiveSessions = true,
    isPasskeyEnabled = true,
    isPasswordEnabled = true,
    isMfaEnabled = true,
    isDeleteSelfEnabled = true,
  )
}
