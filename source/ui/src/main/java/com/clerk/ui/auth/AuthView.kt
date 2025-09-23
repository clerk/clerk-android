package com.clerk.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark

@Composable fun AuthView(authMode: AuthMode, modifier: Modifier = Modifier) {}

@PreviewLightDark
@Composable
private fun Preview() {
  AuthView()
}
