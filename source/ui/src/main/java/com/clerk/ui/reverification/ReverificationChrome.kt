package com.clerk.ui.reverification

import androidx.compose.material3.SnackbarHostState

internal class ReverificationChrome(
  val snackbarHostState: SnackbarHostState,
  val canGoBack: Boolean,
  val onBack: () -> Unit,
  val onClose: () -> Unit,
)
