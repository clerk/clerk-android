package com.clerk.workbench

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clerk.api.Clerk
import com.clerk.api.session.pendingTaskKey
import com.clerk.ui.auth.AuthView

@Composable
internal fun WorkbenchAuthGate(
  persistIdentifiers: Boolean = true,
  signedInContent: @Composable () -> Unit,
) {
  val isInitialized by Clerk.isInitialized.collectAsStateWithLifecycle()
  val session by Clerk.sessionFlow.collectAsStateWithLifecycle()
  val user by Clerk.userFlow.collectAsStateWithLifecycle()
  val pendingTaskKey = session?.pendingTaskKey
  var isAuthFlowActive by rememberSaveable { mutableStateOf(false) }

  LaunchedEffect(isInitialized, user?.id, session?.id, pendingTaskKey) {
    if (!isInitialized) return@LaunchedEffect
    if (user == null || pendingTaskKey != null) {
      isAuthFlowActive = true
    }
  }

  when {
    !isInitialized -> CircularProgressIndicator()
    isAuthFlowActive || user == null || pendingTaskKey != null ->
      AuthView(
        persistIdentifiers = persistIdentifiers,
        onAuthComplete = { isAuthFlowActive = false },
      )
    else -> signedInContent()
  }
}
