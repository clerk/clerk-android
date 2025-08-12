package com.clerk.ui.theme

import android.annotation.SuppressLint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import com.clerk.api.ui.ClerkColors
import com.clerk.api.ui.ClerkDesign
import com.clerk.api.ui.ClerkFontWeight
import com.clerk.api.ui.ClerkTheme
import com.clerk.api.ui.ClerkTypography
import com.clerk.api.ui.Font
import com.clerk.ui.colors.ComposeClerkColors

@SuppressLint("ComposeCompositionLocalUsage")
val LocalClerkColors = compositionLocalOf<ClerkColors> { error("ClerkColors not provided") }

@SuppressLint("ComposeCompositionLocalUsage")
val LocalClerkTypography =
  compositionLocalOf<ClerkTypography> { error("ClerkTypography not provided") }

/**
 * Provides Clerk theme values through composition locals.
 *
 * @param theme The theme to provide. If null, uses system default theme.
 * @param content The composable content that will have access to the theme.
 */
@Composable
internal fun ClerkThemeProvider(theme: ClerkTheme? = null, content: @Composable () -> Unit) {
  val isDarkTheme = isSystemInDarkTheme()

  // Use ComposeClerkColors defaults and convert to ClerkColors
  val defaultColors = if (isDarkTheme) ComposeClerkColors.dark else ComposeClerkColors.light
  val colors = theme?.colors ?: defaultColors.toClerkColors()

  val typography = theme?.typography ?: createDefaultTypography()
  val design = theme?.design ?: createDefaultDesign()

  CompositionLocalProvider(
    LocalClerkColors provides colors,
    LocalClerkTypography provides typography,
    content = content,
  )
}

/** Object providing easy access to current theme values within composables. */
internal object ClerkThemeApiAccess {
  val colors: ClerkColors
    @Composable get() = LocalClerkColors.current

  val typography: ClerkTypography
    @Composable get() = LocalClerkTypography.current
}

// Extension function to convert ComposeClerkColors to ClerkColors
private fun ComposeClerkColors.toClerkColors(): ClerkColors {
  return ClerkColors(
    primary = com.clerk.api.ui.ThemeColor(this.primary.value.toLong()),
    background = com.clerk.api.ui.ThemeColor(this.background.value.toLong()),
    input = com.clerk.api.ui.ThemeColor(this.input.value.toLong()),
    danger = com.clerk.api.ui.ThemeColor(this.danger.value.toLong()),
    success = com.clerk.api.ui.ThemeColor(this.success.value.toLong()),
    warning = com.clerk.api.ui.ThemeColor(this.warning.value.toLong()),
    foreground = com.clerk.api.ui.ThemeColor(this.foreground.value.toLong()),
    mutedForeground = com.clerk.api.ui.ThemeColor(this.mutedForeground.value.toLong()),
    primaryForeground = com.clerk.api.ui.ThemeColor(this.primaryForeground.value.toLong()),
    inputForeground = com.clerk.api.ui.ThemeColor(this.inputForeground.value.toLong()),
    neutral = com.clerk.api.ui.ThemeColor(this.neutral.value.toLong()),
    border = com.clerk.api.ui.ThemeColor(this.border.value.toLong()),
    ring = com.clerk.api.ui.ThemeColor(this.ring.value.toLong()),
    muted = com.clerk.api.ui.ThemeColor(this.muted.value.toLong()),
    shadow = com.clerk.api.ui.ThemeColor(this.shadow.value.toLong()),
  )
}

// Create default instances
private fun createDefaultTypography(): ClerkTypography =
  ClerkTypography(
    displaySmall = Font(size = 34.0, weight = ClerkFontWeight.Regular),
    headlineLarge = Font(size = 28.0, weight = ClerkFontWeight.Regular),
    headlineMedium = Font(size = 22.0, weight = ClerkFontWeight.Regular),
    headlineSmall = Font(size = 20.0, weight = ClerkFontWeight.Regular),
    titleMedium = Font(size = 17.0, weight = ClerkFontWeight.SemiBold),
    titleSmall = Font(size = 15.0, weight = ClerkFontWeight.Regular),
    bodyLarge = Font(size = 17.0, weight = ClerkFontWeight.Regular),
    bodyMedium = Font(size = 16.0, weight = ClerkFontWeight.Regular),
    bodySmall = Font(size = 13.0, weight = ClerkFontWeight.Regular),
    labelMedium = Font(size = 12.0, weight = ClerkFontWeight.Regular),
    labelSmall = Font(size = 11.0, weight = ClerkFontWeight.Regular),
  )

private fun createDefaultDesign(): ClerkDesign = ClerkDesign(borderRadius = 8f)
