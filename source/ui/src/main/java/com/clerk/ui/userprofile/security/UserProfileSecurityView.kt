package com.clerk.ui.userprofile.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.session.Session
import com.clerk.ui.theme.ClerkMaterialTheme
import kotlinx.collections.immutable.ImmutableList

@Composable
fun UserProfileSecurityView(modifier: Modifier = Modifier) {
  UserProfileSecurityViewImpl()
}

@Composable
private fun UserProfileSecurityViewImpl(
  sessions: ImmutableList<Session>,
  modifier: Modifier = Modifier,
  isPasswordEnabled: Boolean = false,
  isPasskeyEnabled: Boolean = false,
  isMfaEnabled: Boolean = false,
) {
  ClerkMaterialTheme {
    Column(
      modifier = Modifier.fillMaxSize().background(color = ClerkMaterialTheme.colors.background)
    ) {}
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  UserProfileSecurityViewImpl()
}
