package com.clerk.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.Clerk
import com.clerk.ui.core.common.ClerkThemedAuthScaffold
import com.clerk.ui.core.input.ClerkTextField

@Composable
fun AuthStartView(modifier: Modifier = Modifier) {
  val applicationName = Clerk.applicationName
  ClerkThemedAuthScaffold(
    modifier = modifier,
    hasBackButton = false,
    title = "Continue to $applicationName",
  ) {
    ClerkTextField(value = "", onValueChange = {}, label = "Email address")
  }
}

@PreviewLightDark @Composable private fun Preview() {}
