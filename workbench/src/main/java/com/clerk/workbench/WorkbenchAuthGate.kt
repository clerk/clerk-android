package com.clerk.workbench

import androidx.compose.runtime.Composable
import com.clerk.ui.auth.AuthView
import com.clerk.ui.core.composition.LocalClerk

@Composable
internal fun WorkbenchAuthGate(signedInContent: @Composable () -> Unit) {
  if (LocalClerk.isAuthFlowComplete) signedInContent() else AuthView(isDismissible = false)
}
