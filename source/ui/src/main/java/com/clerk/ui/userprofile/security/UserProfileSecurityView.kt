package com.clerk.ui.userprofile.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.ui.R
import com.clerk.ui.core.appbar.ClerkTopAppBar
import com.clerk.ui.core.footer.SecuredByClerkView
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.userprofile.security.delete.UserProfileDeleteAccountSection
import com.clerk.ui.userprofile.security.device.UserProfileDevicesSection
import com.clerk.ui.userprofile.security.mfa.UserProfileMfaSection
import com.clerk.ui.userprofile.security.passkey.UserProfilePasskeySection
import com.clerk.ui.userprofile.security.password.UserProfilePasswordSection

@Composable
fun UserProfileSecurityView(modifier: Modifier = Modifier) {
  UserProfileSecurityViewImpl(modifier = modifier)
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
  ClerkMaterialTheme {
    Scaffold(modifier = Modifier.then(modifier), snackbarHost = {}) { innerPadding ->
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .fillMaxSize()
            .background(ClerkMaterialTheme.colors.muted)
            .padding(innerPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        ClerkTopAppBar(
          hasLogo = false,
          hasBackButton = true,
          title = stringResource(R.string.security),
          backgroundColor = ClerkMaterialTheme.colors.muted,
          onBackPressed = {},
        )
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
        Spacer(modifier = Modifier.weight(1f))
        Spacers.Vertical.Spacer24()
        SecuredByClerkView()
        Spacers.Vertical.Spacer24()
      }
    }
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  ClerkMaterialTheme {
    UserProfileSecurityViewImpl(
      hasActiveSessions = true,
      isPasskeyEnabled = true,
      isPasswordEnabled = true,
      isMfaEnabled = true,
      isDeleteSelfEnabled = true,
    )
  }
}
