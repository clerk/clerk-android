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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.clerk.api.ui.ClerkColors
import com.clerk.api.ui.ClerkDesign
import com.clerk.api.ui.ClerkFontWeight
import com.clerk.api.ui.ClerkTheme
import com.clerk.api.ui.ClerkTypography
import com.clerk.api.ui.Font as ClerkFont
import com.clerk.api.ui.ThemeColor
import com.clerk.ui.colors.ComposeClerkColors
import com.clerk.ui.colors.ComputedColors
import com.clerk.ui.colors.toComposeClerkColors
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
private const val DEFAULT_BORDER_RADIUS = 8f

internal val LocalMaterialColors =
  compositionLocalOf<ColorScheme> { error("MaterialColors not provided") }

internal val LocalMaterialTypography =
  compositionLocalOf<Typography> { error("MaterialTypography not provided") }

internal val LocalComposeColors =
  compositionLocalOf<ComposeClerkColors> { error("ComposeColors not provided") }

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
internal fun ClerkMaterialTheme(clerkTheme: ClerkTheme? = null, content: @Composable () -> Unit) {
  ClerkThemeProvider(theme = clerkTheme) {
    val colors = ClerkThemeApiAccess.colors
    val typography = ClerkThemeApiAccess.typography
    val design = clerkTheme?.design ?: createDefaultDesign()

    // Map Clerk colors to Material colors
    val materialColors = computeColorScheme(colors)

    // Map Clerk typography to Material typography
    val materialTypography = computerTypography(typography)

    val composeColors = colors.toComposeClerkColors()

    val computedColors = generateComputedColors(composeColors)

    CompositionLocalProvider(
      LocalMaterialColors provides materialColors,
      LocalMaterialTypography provides materialTypography,
      LocalComposeColors provides composeColors,
      LocalComputedColors provides computedColors,
      LocalClerkDesign provides design,
    ) {
      MaterialTheme(colorScheme = materialColors, typography = materialTypography) { content() }
    }
  }
}

@Composable
private fun computerTypography(typography: ClerkTypography): Typography {
  val materialTypography =
    Typography(
      displaySmall = typography.displaySmall.toTextStyle(),
      headlineLarge = typography.headlineLarge.toTextStyle(),
      headlineMedium = typography.headlineMedium.toTextStyle(),
      headlineSmall = typography.headlineSmall.toTextStyle(),
      titleMedium = typography.titleMedium.toTextStyle(),
      titleSmall = typography.titleSmall.toTextStyle(),
      bodyLarge = typography.bodyLarge.toTextStyle(),
      bodyMedium = typography.bodyMedium.toTextStyle(),
      bodySmall = typography.bodySmall.toTextStyle(),
      labelMedium = typography.labelMedium.toTextStyle(),
      labelSmall = typography.labelSmall.toTextStyle(),
    )
  return materialTypography
}

@Composable
private fun generateComputedColors(composeColors: ComposeClerkColors): ComputedColors =
  ComputedColors(
    primaryPressed =
      if (isSystemInDarkTheme()) composeColors.primary.lighten(PRIMARY_PRESSED_FACTOR)
      else composeColors.primary.darken(PRIMARY_PRESSED_FACTOR),
    border = composeColors.border.copy(alpha = BORDER_ALPHA_SUBTLE),
    buttonBorder = composeColors.border.copy(alpha = BUTTON_BORDER_ALPHA),
    inputBorder = composeColors.border.copy(alpha = INPUT_BORDER_ALPHA),
    inputBorderFocused = composeColors.ring.copy(alpha = INPUT_BORDER_FOCUSED_ALPHA),
    dangerInputBorder = composeColors.danger.copy(alpha = DANGER_INPUT_BORDER_ALPHA),
    dangerInputBorderFocused = composeColors.danger.copy(alpha = DANGER_INPUT_BORDER_FOCUSED_ALPHA),
    backgroundTransparent = composeColors.background.copy(alpha = BACKGROUND_TRANSPARENT_ALPHA),
    backgroundSuccess = composeColors.success.copy(alpha = SUCCESS_BACKGROUND_ALPHA),
    borderSuccess = composeColors.success.copy(alpha = SUCCESS_BORDER_ALPHA),
    backgroundDanger = composeColors.danger.copy(alpha = DANGER_BACKGROUND_ALPHA),
    borderDanger = composeColors.danger.copy(alpha = DANGER_BORDER_ALPHA),
    backgroundWarning = composeColors.warning.copy(alpha = WARNING_BACKGROUND_ALPHA),
    borderWarning = composeColors.warning.copy(alpha = WARNING_BORDER_ALPHA),
  )

@Composable
private fun computeColorScheme(colors: ClerkColors): ColorScheme {
  val materialColors =
    if (isSystemInDarkTheme()) {
      darkColorScheme(
        primary = Color(colors.primary.argb.toULong()),
        background = Color(colors.background.argb.toULong()),
        surface = Color(colors.input.argb.toULong()),
        error = Color(colors.danger.argb.toULong()),
        onPrimary = Color(colors.primaryForeground.argb.toULong()),
        onBackground = Color(colors.foreground.argb.toULong()),
        onSurface = Color(colors.inputForeground.argb.toULong()),
        outline = Color(colors.border.argb.toULong()),
        secondary = Color(colors.muted.argb.toULong()),
        tertiary = Color(colors.neutral.argb.toULong()),
        surfaceVariant = Color(colors.muted.argb.toULong()),
        onSecondary = Color(colors.foreground.argb.toULong()),
        onTertiary = Color(colors.foreground.argb.toULong()),
        onSurfaceVariant = Color(colors.mutedForeground.argb.toULong()),
      )
    } else {
      lightColorScheme(
        primary = Color(colors.primary.argb.toULong()),
        background = Color(colors.background.argb.toULong()),
        surface = Color(colors.input.argb.toULong()),
        error = Color(colors.danger.argb.toULong()),
        onPrimary = Color(colors.primaryForeground.argb.toULong()),
        onBackground = Color(colors.foreground.argb.toULong()),
        onSurface = Color(colors.inputForeground.argb.toULong()),
        outline = Color(colors.border.argb.toULong()),
        secondary = Color(colors.muted.argb.toULong()),
        tertiary = Color(colors.neutral.argb.toULong()),
        surfaceVariant = Color(colors.muted.argb.toULong()),
        onSecondary = Color(colors.foreground.argb.toULong()),
        onTertiary = Color(colors.foreground.argb.toULong()),
        onSurfaceVariant = Color(colors.mutedForeground.argb.toULong()),
      )
    }
  return materialColors
}

/** Object providing easy access to all color values within composables. */
internal object ClerkThemeAccess {
  val material: ColorScheme
    @Composable get() = LocalMaterialColors.current

  val materialTypography: Typography
    @Composable get() = LocalMaterialTypography.current

  val compose: ComposeClerkColors
    @Composable get() = LocalComposeColors.current

  val computed: ComputedColors
    @Composable get() = LocalComputedColors.current

  val clerkDesign: ClerkDesign
    @Composable get() = LocalClerkDesign.current
}

internal fun ThemeColor.toComposeColor(): Color = Color(this.argb.toULong())

private fun ClerkFont.toTextStyle(): TextStyle {
  val fontFamily =
    if (this.fontResId != null) {
      FontFamily(Font(this.fontResId!!, weight = this.weight.toCompose()))
    } else {
      FontFamily.Default
    }

  return TextStyle(
    fontFamily = fontFamily,
    fontSize = this.size.sp,
    fontWeight = this.weight.toCompose(),
  )
}

private fun ClerkFontWeight.toCompose(): FontWeight =
  when (this) {
    ClerkFontWeight.Thin -> FontWeight.Thin
    ClerkFontWeight.ExtraLight -> FontWeight.ExtraLight
    ClerkFontWeight.Light -> FontWeight.Light
    ClerkFontWeight.Regular -> FontWeight.Normal
    ClerkFontWeight.Medium -> FontWeight.Medium
    ClerkFontWeight.SemiBold -> FontWeight.SemiBold
    ClerkFontWeight.Bold -> FontWeight.Bold
    ClerkFontWeight.ExtraBold -> FontWeight.ExtraBold
    ClerkFontWeight.Black -> FontWeight.Black
  }

private fun createDefaultDesign(): ClerkDesign {
  return ClerkDesign(borderRadius = DEFAULT_BORDER_RADIUS)
}
