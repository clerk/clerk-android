@file:SuppressLint("ComposeCompositionLocalUsage")

package com.clerk.ui.theme

import android.annotation.SuppressLint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.clerk.api.Clerk
import com.clerk.api.ui.ClerkColors
import com.clerk.api.ui.ClerkDesign
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.theme.colors.ComputedColors
import com.clerk.ui.theme.colors.isDark
import com.materialkolor.ktx.darken
import com.materialkolor.ktx.lighten

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
  compositionLocalOf<ClerkThemeColors> { error("ComposeColors not provided") }

internal val LocalComputedColors =
  compositionLocalOf<ComputedColors> { error("ComputedColors not provided") }

internal val LocalClerkDesign =
  compositionLocalOf<ClerkDesign> { error("ClerkDesign not provided") }

internal val LocalClerkThemeOverride = compositionLocalOf<ClerkTheme?> { null }

@Composable
internal fun ClerkThemeOverrideProvider(clerkTheme: ClerkTheme?, content: @Composable () -> Unit) {
  val parentTheme = LocalClerkThemeOverride.current
  val effectiveTheme = clerkTheme ?: parentTheme
  CompositionLocalProvider(LocalClerkThemeOverride provides effectiveTheme, content = content)
}

@Composable
internal fun ClerkMaterialTheme(clerkTheme: ClerkTheme? = null, content: @Composable () -> Unit) {
  val overrideTheme = LocalClerkThemeOverride.current
  val resolvedTheme = clerkTheme ?: overrideTheme ?: Clerk.customTheme

  ClerkThemeProvider(theme = resolvedTheme) {
    val colors = ClerkThemeProviderAccess.colors
    val design = ClerkThemeProviderAccess.design
    val isDarkMode = isSystemInDarkTheme()

    val materialColors = remember(colors, isDarkMode) { computeColorScheme(colors, isDarkMode) }
    val computedColors = remember(colors) { generateComputedColors(colors) }

    val themeColors = remember(colors) { ClerkThemeColors(colors) }

    CompositionLocalProvider(
      LocalComposeColors provides themeColors,
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

internal object ClerkMaterialTheme {
  val colors: ClerkThemeColors
    @Composable @ReadOnlyComposable get() = LocalComposeColors.current

  val typography: Typography
    @Composable get() = ClerkThemeProviderAccess.typography

  val design: ClerkDesign
    @Composable @ReadOnlyComposable get() = LocalClerkDesign.current

  val shape: RoundedCornerShape
    @Composable @ReadOnlyComposable get() = RoundedCornerShape(design.borderRadius)

  val computedColors: ComputedColors
    @Composable @ReadOnlyComposable get() = LocalComputedColors.current
}

/**
 * Non-nullable wrapper for ClerkColors within the theme context.
 *
 * This class provides safe, non-nullable access to all color properties by wrapping the underlying
 * ClerkColors instance. Since colors are guaranteed to be non-null within a ClerkMaterialTheme
 * context (they fall back to defaults), this wrapper eliminates the need for null checks and !!
 * operators in UI components.
 */
internal class ClerkThemeColors internal constructor(private val colors: ClerkColors) {
  val primary: Color
    get() = colors.primary!!

  val background: Color
    get() = colors.background!!

  val input: Color
    get() = colors.input!!

  val danger: Color
    get() = colors.danger!!

  val success: Color
    get() = colors.success!!

  val warning: Color
    get() = colors.warning!!

  val foreground: Color
    get() = colors.foreground!!

  val mutedForeground: Color
    get() = colors.mutedForeground!!

  val primaryForeground: Color
    get() = colors.primaryForeground!!

  val inputForeground: Color
    get() = colors.inputForeground!!

  val neutral: Color
    get() = colors.neutral!!

  val border: Color
    get() = colors.border!!

  val ring: Color
    get() = colors.ring!!

  val muted: Color
    get() = colors.muted!!

  val secondaryButtonBackground: Color
    get() = colors.secondaryButtonBackground!!

  val secondaryButtonForeground: Color
    get() = colors.secondaryButtonForeground!!

  val shadow: Color
    get() = colors.shadow!!
}

@Suppress("CyclomaticComplexMethod")
private fun generateComputedColors(colors: ClerkColors): ComputedColors {

  val computed =
    ComputedColors(
      primaryPressed =
        colors.primary?.let { primary ->
          if (primary.isDark) primary.lighten(PRIMARY_PRESSED_FACTOR)
          else primary.darken(PRIMARY_PRESSED_FACTOR)
        } ?: Color.Transparent,
      border = colors.border?.copy(alpha = BORDER_ALPHA_SUBTLE) ?: Color.Transparent,
      buttonBorder = colors.border?.copy(alpha = BUTTON_BORDER_ALPHA) ?: Color.Transparent,
      inputBorder = colors.border?.copy(alpha = INPUT_BORDER_ALPHA) ?: Color.Transparent,
      inputBorderFocused =
        colors.ring?.copy(alpha = INPUT_BORDER_FOCUSED_ALPHA) ?: Color.Transparent,
      dangerInputBorder =
        colors.danger?.copy(alpha = DANGER_INPUT_BORDER_ALPHA) ?: Color.Transparent,
      dangerInputBorderFocused =
        colors.danger?.copy(alpha = DANGER_INPUT_BORDER_FOCUSED_ALPHA) ?: Color.Transparent,
      backgroundTransparent =
        colors.background?.copy(alpha = BACKGROUND_TRANSPARENT_ALPHA) ?: Color.Transparent,
      backgroundSuccess =
        colors.success?.copy(alpha = SUCCESS_BACKGROUND_ALPHA) ?: Color.Transparent,
      borderSuccess = colors.success?.copy(alpha = SUCCESS_BORDER_ALPHA) ?: Color.Transparent,
      backgroundDanger = colors.danger?.copy(alpha = DANGER_BACKGROUND_ALPHA) ?: Color.Transparent,
      borderDanger = colors.danger?.copy(alpha = DANGER_BORDER_ALPHA) ?: Color.Transparent,
      backgroundWarning =
        colors.warning?.copy(alpha = WARNING_BACKGROUND_ALPHA) ?: Color.Transparent,
      borderWarning = colors.warning?.copy(alpha = WARNING_BORDER_ALPHA) ?: Color.Transparent,
    )
  return computed
}

private fun computeColorScheme(colors: ClerkColors, isDarkMode: Boolean): ColorScheme {

  return if (isDarkMode) {
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

/**
 * @deprecated Use [ClerkMaterialTheme] object instead for accessing theme values. This object is
 *   maintained for backwards compatibility but will be removed in a future version.
 *
 * Migration guide:
 * - `ClerkThemeAccess.colors` → `ClerkMaterialTheme.colors`
 * - `ClerkThemeAccess.typography` → `ClerkMaterialTheme.typography`
 * - `ClerkThemeAccess.design` → `ClerkMaterialTheme.design`
 * - `ClerkThemeAccess.computed` → `ClerkMaterialTheme.computed`
 */
@Deprecated(
  message = "Use ClerkMaterialTheme object instead for accessing theme values",
  replaceWith = ReplaceWith("ClerkMaterialTheme", "com.clerk.ui.theme.ClerkMaterialTheme"),
  level = DeprecationLevel.WARNING,
)
internal object ClerkThemeAccess {
  internal val colors: ClerkThemeColors
    @Composable get() = ClerkMaterialTheme.colors

  internal val typography: Typography
    @Composable get() = ClerkMaterialTheme.typography

  internal val design: ClerkDesign
    @Composable get() = ClerkMaterialTheme.design

  internal val computed: ComputedColors
    @Composable get() = ClerkMaterialTheme.computedColors
}
