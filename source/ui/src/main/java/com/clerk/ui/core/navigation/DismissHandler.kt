package com.clerk.ui.core.navigation

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal fun rememberDismissHandler(onDismiss: (() -> Unit)?): () -> Unit {
  val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
  return remember(onDismiss, dispatcher) { { onDismiss?.invoke() ?: dispatcher?.onBackPressed() } }
}
