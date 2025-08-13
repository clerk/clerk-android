@file:SuppressLint("ComposeCompositionLocalUsage")

package com.clerk.ui.theme

import android.annotation.SuppressLint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.clerk.api.Clerk
import com.clerk.api.ui.ClerkColors
import com.clerk.api.ui.ClerkDesign
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.colors.ComputedColors
import com.materialkolor.ktx.darken
import com.materialkolor.ktx.lighten

// Theme constants
private const val PRIMARY_PRESSED_FACTOR = 0.06F
private const val BORDER_ALPHA_SUBTLE = 0.06F
private const val BUTTON_BORDER_ALPHA = 0.08F
private const val INPUT_BORDER_ALPHA = 0.11F
private const val INPUT_BORDER_FOCUSED_ALPHA = 0.28F
private const val DANGER_INPUT_BORDER_ALPHA = 0.53F
private const val DANGER_INPUT_BORDER_FOCUSED_ALPHA = 0.15F
private const val BACKGROUND_TRANSPARENT_ALPHA = 0.5F
private const val SUCCESS_BACKGROUND_ALPHA = 0.12F
private const val SUCCESS_BORDER_ALPHA = 0.77F
private const val DANGER_BACKGROUND_ALPHA = 0.12F
private const val DANGER_BORDER_ALPHA = 0.77F
private const val WARNING_BACKGROUND_ALPHA = 0.12F
private const val WARNING_BORDER_ALPHA = 0.77F

internal val LocalComposeColors =
  compositionLocalOf<ClerkColors> { error("ComposeColors not provided") }

internal val LocalComputedColors =
  compositionLocalOf<ComputedColors> { error("ComputedColors not provided") }

internal val LocalClerkDesign =
  compositionLocalOf<ClerkDesign> { error("ClerkDesign not provided") }

/**
 * Provides both Clerk theme and Material3 theme integration.
 *
 * @param clerkTheme The Clerk theme to apply. If null, uses system defaults.
 * @param content The composable content.
 */
@Composable
internal fun ClerkMaterialTheme(
  clerkTheme: ClerkTheme? = Clerk.customTheme,
  content: @Composable () -> Unit,
) {
  ClerkThemeProvider(theme = clerkTheme) {
    val colors = ClerkThemeProviderAccess.colors
    val design = ClerkThemeProviderAccess.design

    val materialColors = computeColorScheme(colors)

    val computedColors = generateComputedColors(colors)

    CompositionLocalProvider(
      LocalComposeColors provides colors,
      LocalComputedColors provides computedColors,
      LocalClerkDesign provides design,
    ) {
      MaterialTheme(
        colorScheme = materialColors,
        typography = ClerkThemeProviderAccess.typography,
      ) {
        content()
      }
    }
  }
}

@Composable
private fun generateComputedColors(colors: ClerkColors): ComputedColors {

  return ComputedColors(
    primaryPressed =
      if (isSystemInDarkTheme())
        colors.primary?.lighten(PRIMARY_PRESSED_FACTOR) ?: Color.Transparent
      else colors.primary?.darken(PRIMARY_PRESSED_FACTOR) ?: Color.Transparent,
    border = colors.border?.copy(alpha = BORDER_ALPHA_SUBTLE) ?: Color.Transparent,
    buttonBorder = colors.border?.copy(alpha = BUTTON_BORDER_ALPHA) ?: Color.Transparent,
    inputBorder = colors.border?.copy(alpha = INPUT_BORDER_ALPHA) ?: Color.Transparent,
    inputBorderFocused = colors.ring?.copy(alpha = INPUT_BORDER_FOCUSED_ALPHA) ?: Color.Transparent,
    dangerInputBorder = colors.danger?.copy(alpha = DANGER_INPUT_BORDER_ALPHA) ?: Color.Transparent,
    dangerInputBorderFocused =
      colors.danger?.copy(alpha = DANGER_INPUT_BORDER_FOCUSED_ALPHA) ?: Color.Transparent,
    backgroundTransparent =
      colors.background?.copy(alpha = BACKGROUND_TRANSPARENT_ALPHA) ?: Color.Transparent,
    backgroundSuccess = colors.success?.copy(alpha = SUCCESS_BACKGROUND_ALPHA) ?: Color.Transparent,
    borderSuccess = colors.success?.copy(alpha = SUCCESS_BORDER_ALPHA) ?: Color.Transparent,
    backgroundDanger = colors.danger?.copy(alpha = DANGER_BACKGROUND_ALPHA) ?: Color.Transparent,
    borderDanger = colors.danger?.copy(alpha = DANGER_BORDER_ALPHA) ?: Color.Transparent,
    backgroundWarning = colors.warning?.copy(alpha = WARNING_BACKGROUND_ALPHA) ?: Color.Transparent,
    borderWarning = colors.warning?.copy(alpha = WARNING_BORDER_ALPHA) ?: Color.Transparent,
  )
}

@Composable
private fun computeColorScheme(colors: ClerkColors): ColorScheme {

  return if (isSystemInDarkTheme()) {
    darkColorScheme(
      primary = colors.primary!!,
      background = colors.background!!,
      surface = colors.input!!,
      error = colors.danger!!,
      onPrimary = colors.primaryForeground!!,
      onBackground = colors.foreground!!,
      onSurface = colors.inputForeground!!,
      outline = colors.border!!,
      secondary = colors.muted!!,
      tertiary = colors.neutral!!,
      onSurfaceVariant = colors.mutedForeground!!,
    )
  } else {
    lightColorScheme(
      primary = colors.primary!!,
      background = colors.background!!,
      surface = colors.input!!,
      error = colors.danger!!,
      onPrimary = colors.primaryForeground!!,
      onBackground = colors.foreground!!,
      onSurface = colors.inputForeground!!,
      outline = colors.border!!,
      secondary = colors.muted!!,
      tertiary = colors.neutral!!,
      onSurfaceVariant = colors.mutedForeground!!,
    )
  }
}

/** Object providing easy access to all theme values within composables. */
internal object ClerkThemeAccess {

  // Direct Clerk theme object access
  internal val colors: ClerkColors
    @Composable get() = LocalComposeColors.current

  internal val typography: Typography
    @Composable get() = ClerkThemeProviderAccess.typography

  internal val design: ClerkDesign
    @Composable get() = LocalClerkDesign.current

  // Computed color variants
  internal val computed: ComputedColors
    @Composable get() = LocalComputedColors.current
}
