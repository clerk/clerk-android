package com.clerk.ui.core.error

import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.Clerk
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.DefaultColors
import generateDangerPaletteHsl

@Composable
fun ClerkErrorSnackbar(snackbarHostState: SnackbarHostState, modifier: Modifier = Modifier) {
  val dangerPalette = ClerkMaterialTheme.computedColors.backgroundDanger.generateDangerPaletteHsl()
  SnackbarHost(snackbarHostState) { data ->
    Snackbar(
      modifier = modifier,
      containerColor = dangerPalette.danger950,
      contentColor = dangerPalette.danger25,
      dismissActionContentColor = dangerPalette.danger150,
      actionContentColor = dangerPalette.danger150,
      actionColor = dangerPalette.danger150,
      snackbarData = data,
    )
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  Clerk.customTheme = ClerkTheme(colors = DefaultColors.clerk)
  ClerkMaterialTheme {
    val hostState = remember { SnackbarHostState() }

    ClerkErrorSnackbar(snackbarHostState = hostState)

    LaunchedEffect(Unit) {
      hostState.showSnackbar(
        message = "Whooops, something is wrong. Similique qui enim placeat tempore...",
        actionLabel = "Longer action",
      )
    }
  }
}
