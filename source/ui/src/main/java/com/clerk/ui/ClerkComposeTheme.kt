package com.clerk.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.clerk.api.Clerk
import com.clerk.api.ui.ClerkDesign
import com.clerk.api.ui.ClerkFontWeight
import com.clerk.api.ui.ClerkTheme
import com.clerk.api.ui.Font as ClerkFont
import com.clerk.api.ui.ThemeColor
import com.clerk.ui.colors.ColorPalettes

val LocalClerkTheme = staticCompositionLocalOf<ClerkTheme> { error("No theme provided") }

@Composable
fun ClerkTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {

  val colorScheme =
    if (darkTheme) {
      darkColorScheme(
        primary =
          Clerk.customTheme?.colors?.primary?.toComposeColor() ?: ColorPalettes.Dark.primary,
        background =
          Clerk.customTheme?.colors?.background?.toComposeColor() ?: ColorPalettes.Dark.background,
        surface = Clerk.customTheme?.colors?.input?.toComposeColor() ?: ColorPalettes.Dark.input,
        error = Clerk.customTheme?.colors?.danger?.toComposeColor() ?: ColorPalettes.Dark.danger,
        onPrimary =
          Clerk.customTheme?.colors?.primaryForeground?.toComposeColor()
            ?: ColorPalettes.Dark.primaryForeground,
        onBackground =
          Clerk.customTheme?.colors?.foreground?.toComposeColor() ?: ColorPalettes.Dark.foreground,
        onSurface =
          Clerk.customTheme?.colors?.inputForeground?.toComposeColor()
            ?: ColorPalettes.Dark.inputForeground,
        outline = Clerk.customTheme?.colors?.border?.toComposeColor() ?: ColorPalettes.Dark.border,
      )
    } else {
      lightColorScheme(
        primary =
          Clerk.customTheme?.colors?.primary?.toComposeColor() ?: ColorPalettes.Light.primary,
        background =
          Clerk.customTheme?.colors?.background?.toComposeColor() ?: ColorPalettes.Light.background,
        surface = Clerk.customTheme?.colors?.input?.toComposeColor() ?: ColorPalettes.Light.input,
        error = Clerk.customTheme?.colors?.danger?.toComposeColor() ?: ColorPalettes.Light.danger,
        onPrimary =
          Clerk.customTheme?.colors?.primaryForeground?.toComposeColor()
            ?: ColorPalettes.Light.primaryForeground,
        onBackground =
          Clerk.customTheme?.colors?.foreground?.toComposeColor() ?: ColorPalettes.Light.foreground,
        onSurface =
          Clerk.customTheme?.colors?.inputForeground?.toComposeColor()
            ?: ColorPalettes.Light.inputForeground,
        outline = Clerk.customTheme?.colors?.border?.toComposeColor() ?: ColorPalettes.Light.border,
      )
    }

  // Build Material3 Typography from API-level ClerkTypography if provided, else fall back
  val t = Clerk.customTheme?.typography
  val defaults = Typography()
  val typography =
    Typography(
      displayLarge = t?.displayLarge?.toTextStyle() ?: defaults.displayLarge,
      displayMedium = t?.displayMedium?.toTextStyle() ?: defaults.displayMedium,
      displaySmall = t?.displaySmall?.toTextStyle() ?: defaults.displaySmall,
      headlineLarge = t?.headlineLarge?.toTextStyle() ?: defaults.headlineLarge,
      headlineMedium = t?.headlineMedium?.toTextStyle() ?: defaults.headlineMedium,
      headlineSmall = t?.headlineSmall?.toTextStyle() ?: defaults.headlineSmall,
      titleLarge = t?.titleLarge?.toTextStyle() ?: defaults.titleLarge,
      titleMedium = t?.titleMedium?.toTextStyle() ?: defaults.titleMedium,
      titleSmall = t?.titleSmall?.toTextStyle() ?: defaults.titleSmall,
      bodyLarge = t?.bodyLarge?.toTextStyle() ?: defaults.bodyLarge,
      bodyMedium = t?.bodyMedium?.toTextStyle() ?: defaults.bodyMedium,
      bodySmall = t?.bodySmall?.toTextStyle() ?: defaults.bodySmall,
      labelLarge = t?.labelLarge?.toTextStyle() ?: defaults.labelLarge,
      labelMedium = t?.labelMedium?.toTextStyle() ?: defaults.labelMedium,
      labelSmall = t?.labelSmall?.toTextStyle() ?: defaults.labelSmall,
    )

  val design = Clerk.customTheme?.design ?: ClerkDesign(borderRadius = 12f)
  CompositionLocalProvider(LocalClerkTheme provides clerkTheme, LocalClerkDesign provides design) {
    MaterialTheme(colorScheme = colorScheme, typography = typography) { content() }
  }
}

val clerkTheme: ClerkTheme
  @Composable @ReadOnlyComposable get() = LocalClerkTheme.current

fun ThemeColor.toComposeColor(): Color {
  return Color(this.argb)
}

private fun ClerkFont.toTextStyle(): TextStyle {
  // fontResId is non-nullable in API. If a consumer passes 0, we skip setting a custom family.
  val fontFamily =
    if (this.fontResId != 0) FontFamily(Font(this.fontResId, weight = this.weight.toCompose()))
    else null
  return TextStyle(
    fontFamily = fontFamily ?: FontFamily.Default,
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
