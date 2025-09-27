package com.clerk.ui.theme

import android.annotation.SuppressLint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.clerk.api.ui.ClerkColors
import com.clerk.api.ui.ClerkDesign
import com.clerk.api.ui.ClerkTheme

internal object DefaultColors {
  val light =
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

  val dark =
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

  val clerk =
    ClerkColors(
      primary = Color(color = 0xFF6C47FF),
      background = Color(0xFFFFFFFF),
      input = Color.White,
      danger = Color(0xFFDC2626),
      success = Color(0xFF22C543),
      warning = Color(0xFFF36B16),
      foreground = Color(0xFF212126),
      mutedForeground = Color(0xFF747686),
      primaryForeground = Color.White,
      inputForeground = Color(0xFF212126),
      neutral = Color(0xFF2B2B34),
      border = Color(0xFF2B2B34),
      shadow = Color(0xFF2B2B34),
      ring = Color(0xFF6C47FF),
      muted = Color(0xFFF9F9F9),
    )
}

@SuppressLint("ComposeCompositionLocalUsage")
internal val LocalClerkColors =
  compositionLocalOf<ClerkColors> { error("ClerkColors not provided") }

@SuppressLint("ComposeCompositionLocalUsage")
internal val LocalClerkTypography =
  compositionLocalOf<Typography> { error("ClerkTypography not provided") }

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
  val typography = generateTypography(theme)

  val design = theme?.design ?: ClerkDesign()

  CompositionLocalProvider(
    LocalClerkColors provides colors,
    LocalClerkTypography provides typography,
    LocalClerkDesign provides design,
    content = content,
  )
}

@Composable
private fun generateTypography(theme: ClerkTheme?): Typography {
  val typography =
    Typography(
      displaySmall = theme?.typography?.displaySmall ?: Typography().displaySmall,
      headlineLarge = theme?.typography?.headlineLarge ?: Typography().headlineLarge,
      headlineMedium = theme?.typography?.headlineMedium ?: Typography().headlineMedium,
      headlineSmall = theme?.typography?.headlineSmall ?: Typography().headlineSmall,
      titleMedium = theme?.typography?.titleMedium ?: Typography().titleMedium,
      titleSmall = theme?.typography?.titleSmall ?: Typography().titleSmall,
      bodyLarge = theme?.typography?.bodyLarge ?: Typography().bodyLarge,
      bodyMedium = theme?.typography?.bodyMedium ?: Typography().bodyMedium,
      bodySmall = theme?.typography?.bodySmall ?: Typography().bodySmall,
      labelMedium = theme?.typography?.labelMedium ?: Typography().labelMedium,
      labelSmall = theme?.typography?.labelSmall ?: Typography().labelSmall,
    )
  return typography
}

@Composable
private fun generateColors(theme: ClerkTheme?): ClerkColors {
  val defaultColors = if (isSystemInDarkTheme()) DefaultColors.dark else DefaultColors.light
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
internal object ClerkThemeProviderAccess {
  internal val colors: ClerkColors
    @Composable get() = LocalClerkColors.current

  internal val typography: Typography
    @Composable get() = LocalClerkTypography.current

  internal val design: ClerkDesign
    @Composable get() = LocalClerkDesign.current
}
