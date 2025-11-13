package com.clerk.ui.core.error

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
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

/**
 * A composable that displays a Snackbar with Clerk's danger/error styling.
 *
 * This component is a wrapper around [SnackbarHost] and is intended to be used as the
 * `snackbarHost` parameter in a Scaffold. It automatically applies Clerk's error color palette to
 * the snackbar elements.
 *
 * @param snackbarHostState The [SnackbarHostState] used to show snackbars.
 * @param modifier The [Modifier] to be applied to the [SnackbarHost].
 */
@Composable
fun ClerkErrorSnackbar(
  snackbarHostState: SnackbarHostState,
  modifier: Modifier = Modifier,
  clerkTheme: ClerkTheme? = null,
) {
  ClerkMaterialTheme(clerkTheme = clerkTheme) {
    val dangerPalette =
      ClerkMaterialTheme.computedColors.backgroundDanger.generateDangerPaletteHsl()
    SnackbarHost(snackbarHostState) { data ->
      Snackbar(
        modifier = modifier.windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars)),
        containerColor = dangerPalette.danger950,
        contentColor = dangerPalette.danger25,
        dismissActionContentColor = dangerPalette.danger150,
        actionContentColor = dangerPalette.danger150,
        actionColor = dangerPalette.danger150,
        snackbarData = data,
      )
    }
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
