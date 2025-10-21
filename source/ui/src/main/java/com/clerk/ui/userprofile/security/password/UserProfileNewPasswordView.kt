package com.clerk.ui.userprofile.security.password

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark

@Composable
fun UserProfileNewPasswordView(currentPassword: String, modifier: Modifier = Modifier) {
  UserProfileNewPasswordViewImpl(modifier = modifier, currentPassword = currentPassword)
}

@Composable
private fun UserProfileNewPasswordViewImpl(currentPassword: String, modifier: Modifier = Modifier) {
  Text(modifier = modifier, text = "Current password: $currentPassword")
}

@PreviewLightDark
@Composable
private fun Preview() {
  UserProfileNewPasswordViewImpl("MySecretPassword123")
}
