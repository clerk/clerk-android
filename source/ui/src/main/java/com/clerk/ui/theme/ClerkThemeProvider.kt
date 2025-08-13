package com.clerk.ui.theme

import android.annotation.SuppressLint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.clerk.api.ui.ClerkColors
import com.clerk.api.ui.ClerkDesign
import com.clerk.api.ui.ClerkTheme
import com.clerk.api.ui.ClerkTypography

// Default color values
private object DefaultClerkColors {
  val lightColors =
    ClerkColors(
      primary = Color(0xFF2F3037),
      background = Color(0xFFFFFFFF),
      input = Color(0xFFFFFFFF),
      danger = Color(0xFFEF4444),
      success = Color(0xFF22C543),
      warning = Color(0xFFF36B16),
      foreground = Color(0xFF212126),
      mutedForeground = Color(0xFF747686),
      primaryForeground = Color(0xFFFFFFFF),
      inputForeground = Color(0xFF212126),
      neutral = Color(0xFF000000),
      border = Color(0xFF000000),
      ring = Color(0xFF000000),
      muted = Color(0xFFF7F7F7),
      shadow = Color(0xFF000000),
    )

  val darkColors =
    ClerkColors(
      primary = Color(0xFFFAFAFB),
      background = Color(0xFF131316),
      input = Color(0xFF212126),
      danger = Color(0xFFEF4444),
      success = Color(0xFF22C543),
      warning = Color(0xFFF36B16),
      foreground = Color(0xFFFFFFFF),
      mutedForeground = Color(0xFFB7B8C2),
      primaryForeground = Color(0xFF000000),
      inputForeground = Color(0xFFFFFFFF),
      neutral = Color(0xFFFFFFFF),
      border = Color(0xFFFFFFFF),
      ring = Color(0xFFFFFFFF),
      muted = Color(0xFF1A1A1D),
      shadow = Color(0xFFFFFFFF),
    )
}

private object DefaultClerkTypography {
  val defaultTypography =
    ClerkTypography(
      displaySmall =
        TextStyle(
          fontFamily = FontFamily.Default,
          fontWeight = FontWeight.Normal,
          fontSize = 34.sp,
        ),
      headlineLarge =
        TextStyle(
          fontFamily = FontFamily.Default,
          fontWeight = FontWeight.Normal,
          fontSize = 28.sp,
        ),
      headlineMedium =
        TextStyle(
          fontFamily = FontFamily.Default,
          fontWeight = FontWeight.Normal,
          fontSize = 22.sp,
        ),
      headlineSmall =
        TextStyle(
          fontFamily = FontFamily.Default,
          fontWeight = FontWeight.Normal,
          fontSize = 20.sp,
        ),
      titleMedium =
        TextStyle(
          fontFamily = FontFamily.Default,
          fontWeight = FontWeight.SemiBold,
          fontSize = 17.sp,
        ),
      titleSmall =
        TextStyle(
          fontFamily = FontFamily.Default,
          fontWeight = FontWeight.Normal,
          fontSize = 15.sp,
        ),
      bodyLarge =
        TextStyle(
          fontFamily = FontFamily.Default,
          fontWeight = FontWeight.Normal,
          fontSize = 17.sp,
        ),
      bodyMedium =
        TextStyle(
          fontFamily = FontFamily.Default,
          fontWeight = FontWeight.Normal,
          fontSize = 16.sp,
        ),
      bodySmall =
        TextStyle(
          fontFamily = FontFamily.Default,
          fontWeight = FontWeight.Normal,
          fontSize = 13.sp,
        ),
      labelMedium =
        TextStyle(
          fontFamily = FontFamily.Default,
          fontWeight = FontWeight.Normal,
          fontSize = 12.sp,
        ),
      labelSmall =
        TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 11.sp),
    )
}

@SuppressLint("ComposeCompositionLocalUsage")
internal val LocalClerkColors =
  compositionLocalOf<ClerkColors> { error("ClerkColors not provided") }

@SuppressLint("ComposeCompositionLocalUsage")
internal val LocalClerkTypography =
  compositionLocalOf<ClerkTypography> { error("ClerkTypography not provided") }

/**
 * Provides Clerk theme values through composition locals.
 *
 * @param theme The theme to provide. If null, uses system default theme.
 * @param content The composable content that will have access to the theme.
 */
@Composable
internal fun ClerkThemeProvider(theme: ClerkTheme? = null, content: @Composable () -> Unit) {

  // Resolve colors - use provided values or system defaults
  val colors = generateColors(theme)

  // Resolve typography - use provided values or defaults
  val defaultTypography = DefaultClerkTypography.defaultTypography
  val typography = generateTypography(theme, defaultTypography)

  val design = theme?.design ?: ClerkDesign()

  CompositionLocalProvider(
    LocalClerkColors provides colors,
    LocalClerkTypography provides typography,
    LocalClerkDesign provides design,
    content = content,
  )
}

@Composable
private fun generateTypography(
  theme: ClerkTheme?,
  defaultTypography: ClerkTypography,
): ClerkTypography {
  val typography =
    ClerkTypography(
      displaySmall = theme?.typography?.displaySmall ?: defaultTypography.displaySmall,
      headlineLarge = theme?.typography?.headlineLarge ?: defaultTypography.headlineLarge,
      headlineMedium = theme?.typography?.headlineMedium ?: defaultTypography.headlineMedium,
      headlineSmall = theme?.typography?.headlineSmall ?: defaultTypography.headlineSmall,
      titleMedium = theme?.typography?.titleMedium ?: defaultTypography.titleMedium,
      titleSmall = theme?.typography?.titleSmall ?: defaultTypography.titleSmall,
      bodyLarge = theme?.typography?.bodyLarge ?: defaultTypography.bodyLarge,
      bodyMedium = theme?.typography?.bodyMedium ?: defaultTypography.bodyMedium,
      bodySmall = theme?.typography?.bodySmall ?: defaultTypography.bodySmall,
      labelMedium = theme?.typography?.labelMedium ?: defaultTypography.labelMedium,
      labelSmall = theme?.typography?.labelSmall ?: defaultTypography.labelSmall,
    )
  return typography
}

@Composable
private fun generateColors(theme: ClerkTheme?): ClerkColors {
  val defaultColors =
    if (isSystemInDarkTheme()) DefaultClerkColors.darkColors else DefaultClerkColors.lightColors
  val colors =
    ClerkColors(
      primary = theme?.colors?.primary ?: defaultColors.primary,
      background = theme?.colors?.background ?: defaultColors.background,
      input = theme?.colors?.input ?: defaultColors.input,
      danger = theme?.colors?.danger ?: defaultColors.danger,
      success = theme?.colors?.success ?: defaultColors.success,
      warning = theme?.colors?.warning ?: defaultColors.warning,
      foreground = theme?.colors?.foreground ?: defaultColors.foreground,
      mutedForeground = theme?.colors?.mutedForeground ?: defaultColors.mutedForeground,
      primaryForeground = theme?.colors?.primaryForeground ?: defaultColors.primaryForeground,
      inputForeground = theme?.colors?.inputForeground ?: defaultColors.inputForeground,
      neutral = theme?.colors?.neutral ?: defaultColors.neutral,
      border = theme?.colors?.border ?: defaultColors.border,
      ring = theme?.colors?.ring ?: defaultColors.ring,
      muted = theme?.colors?.muted ?: defaultColors.muted,
      shadow = theme?.colors?.shadow ?: defaultColors.shadow,
    )
  return colors
}

/** Object providing easy access to current theme values within composables. */
internal object ClerkThemeApiAccess {
  internal val colors: ClerkColors
    @Composable get() = LocalClerkColors.current

  internal val typography: ClerkTypography
    @Composable get() = LocalClerkTypography.current

  internal val design: ClerkDesign
    @Composable get() = LocalClerkDesign.current
}
