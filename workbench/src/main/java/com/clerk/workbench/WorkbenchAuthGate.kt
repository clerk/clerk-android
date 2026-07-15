package com.clerk.workbench

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clerk.api.Clerk
import com.clerk.ui.auth.AuthView

@Composable
internal fun WorkbenchAuthGate(signedInContent: @Composable () -> Unit) {
  val isInitialized by Clerk.isInitialized.collectAsStateWithLifecycle()
  val isAuthFlowComplete by Clerk.isAuthFlowCompleteFlow.collectAsStateWithLifecycle()

  when {
    !isInitialized -> CircularProgressIndicator()
    !isAuthFlowComplete -> AuthView(isDismissible = false)
    else -> signedInContent()
  }
}
