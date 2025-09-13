package com.clerk.ui.core.error

import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
fun ClerkErrorSnackbar(snackbarHostState: SnackbarHostState, modifier: Modifier = Modifier) {
  SnackbarHost(snackbarHostState) { data ->
    Snackbar(
      modifier = modifier,
      containerColor = ClerkMaterialTheme.computedColors.backgroundDanger,
      contentColor = ClerkMaterialTheme.colors.foreground,
      dismissActionContentColor = ClerkMaterialTheme.colors.foreground,
      snackbarData = data,
    )
  }
}
