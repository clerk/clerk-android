package com.clerk.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.clerk.api.ui.ClerkFontWeight
import com.clerk.api.ui.ClerkTheme
import com.clerk.api.ui.Font as ClerkFont
import com.clerk.api.ui.ThemeColor

/**
 * Provides both Clerk theme and Material3 theme integration.
 *
 * @param clerkTheme The Clerk theme to apply. If null, uses system defaults.
 * @param content The composable content.
 */
@Composable
fun ClerkMaterialTheme(clerkTheme: ClerkTheme? = null, content: @Composable () -> Unit) {
  ClerkThemeProvider(theme = clerkTheme) {
    val isDarkTheme = isSystemInDarkTheme()
    val colors = ClerkThemeAccess.colors

    // Map Clerk colors to Material colors
    val materialColors =
      if (isDarkTheme) {
        darkColorScheme(
          primary = Color(colors.primary.argb.toULong()),
          background = Color(colors.background.argb.toULong()),
          surface = Color(colors.input.argb.toULong()),
          error = Color(colors.danger.argb.toULong()),
          onPrimary = Color(colors.primaryForeground.argb.toULong()),
          onBackground = Color(colors.foreground.argb.toULong()),
          onSurface = Color(colors.inputForeground.argb.toULong()),
          outline = Color(colors.border.argb.toULong()),
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
        )
      }

    // Map Clerk typography to Material typography
    val clerkTypography = ClerkThemeAccess.typography
    val materialTypography =
      Typography(
        displaySmall = clerkTypography.displaySmall.toTextStyle(),
        headlineLarge = clerkTypography.headlineLarge.toTextStyle(),
        headlineMedium = clerkTypography.headlineMedium.toTextStyle(),
        headlineSmall = clerkTypography.headlineSmall.toTextStyle(),
        titleMedium = clerkTypography.titleMedium.toTextStyle(),
        titleSmall = clerkTypography.titleSmall.toTextStyle(),
        bodyLarge = clerkTypography.bodyLarge.toTextStyle(),
        bodyMedium = clerkTypography.bodyMedium.toTextStyle(),
        bodySmall = clerkTypography.bodySmall.toTextStyle(),
        labelMedium = clerkTypography.labelMedium.toTextStyle(),
        labelSmall = clerkTypography.labelSmall.toTextStyle(),
      )

    MaterialTheme(colorScheme = materialColors, typography = materialTypography) { content() }
  }
}

fun ThemeColor.toComposeColor(): Color = Color(this.argb.toULong())

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
