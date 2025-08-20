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

internal data class ResolvedClerkColors(
  val primary: Color,
  val background: Color,
  val input: Color,
  val danger: Color,
  val success: Color,
  val warning: Color,
  val foreground: Color,
  val mutedForeground: Color,
  val primaryForeground: Color,
  val inputForeground: Color,
  val neutral: Color,
  val border: Color,
  val ring: Color,
  val muted: Color,
  val shadow: Color,
)

internal object DefaultColors {
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
      inputForeground = Color(0xFF2B2B34),
      neutral = Color(0xFFF9F9F9),
      border = Color(0xFF2B2B34),
      shadow = Color(0xFF2B2B34),
      ring = Color(0xFF6C47FF),
      muted = Color(0xFFF9F9F9),
      primaryForeground = Color(0xFF000000),
    )
}

@SuppressLint("ComposeCompositionLocalUsage")
internal val LocalClerkThemeColors =
  compositionLocalOf<ResolvedClerkColors> { error("ClerkColors not provided") }

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
    LocalClerkThemeColors provides colors,
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
private fun generateColors(theme: ClerkTheme?): ResolvedClerkColors {
  val defaultColors = if (isSystemInDarkTheme()) DefaultColors.dark else DefaultColors.lightColors
  val t = theme?.colors
  val colors =
    ResolvedClerkColors(
      primary = t?.primary ?: defaultColors.primary!!,
      background = t?.background ?: defaultColors.background!!,
      input = t?.input ?: defaultColors.input!!,
      danger = t?.danger ?: defaultColors.danger!!,
      success = t?.success ?: defaultColors.success!!,
      warning = t?.warning ?: defaultColors.warning!!,
      foreground = t?.foreground ?: defaultColors.foreground!!,
      mutedForeground = t?.mutedForeground ?: defaultColors.mutedForeground!!,
      primaryForeground = t?.primaryForeground ?: defaultColors.primaryForeground!!,
      inputForeground = t?.inputForeground ?: defaultColors.inputForeground!!,
      neutral = t?.neutral ?: defaultColors.neutral!!,
      border = t?.border ?: defaultColors.border!!,
      ring = t?.ring ?: defaultColors.ring!!,
      muted = t?.muted ?: defaultColors.muted!!,
      shadow = t?.shadow ?: defaultColors.shadow!!,
    )
  return colors
}

/** Object providing easy access to current theme values within composables. */
internal object ClerkThemeProviderAccess {
  internal val colors: ResolvedClerkColors
    @Composable get() = LocalClerkThemeColors.current

  internal val typography: Typography
    @Composable get() = LocalClerkTypography.current

  internal val design: ClerkDesign
    @Composable get() = LocalClerkDesign.current
}
