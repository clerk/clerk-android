package com.clerk.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.clerk.api.Clerk
import com.clerk.ui.colors.ColorPalettes

@Stable
class ClerkComposeTheme(
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
  val shadow: Color,
  val ring: Color,
  val muted: Color,
) {
  companion object Companion {
    fun light() =
      ClerkComposeTheme(
        primary = ColorPalettes.Light.primary,
        background = ColorPalettes.Light.background,
        input = ColorPalettes.Light.input,
        danger = ColorPalettes.Light.danger,
        success = ColorPalettes.Light.success,
        warning = ColorPalettes.Light.warning,
        foreground = ColorPalettes.Light.foreground,
        mutedForeground = ColorPalettes.Light.mutedForeground,
        primaryForeground = ColorPalettes.Light.primaryForeground,
        inputForeground = ColorPalettes.Light.inputForeground,
        neutral = ColorPalettes.Light.neutral,
        border = ColorPalettes.Light.border,
        shadow = ColorPalettes.Light.shadow,
        ring = ColorPalettes.Light.ring,
        muted = ColorPalettes.Light.muted,
      )

    fun dark() =
      ClerkComposeTheme(
        primary = ColorPalettes.Dark.primary,
        background = ColorPalettes.Dark.background,
        input = ColorPalettes.Dark.input,
        danger = ColorPalettes.Dark.danger,
        success = ColorPalettes.Dark.success,
        warning = ColorPalettes.Dark.warning,
        foreground = ColorPalettes.Dark.foreground,
        mutedForeground = ColorPalettes.Dark.mutedForeground,
        primaryForeground = ColorPalettes.Dark.primaryForeground,
        inputForeground = ColorPalettes.Dark.inputForeground,
        neutral = ColorPalettes.Dark.neutral,
        border = ColorPalettes.Dark.border,
        shadow = ColorPalettes.Dark.shadow,
        ring = ColorPalettes.Dark.ring,
        muted = ColorPalettes.Dark.muted,
      )

    fun clerk() =
      ClerkComposeTheme(
        primary = ColorPalettes.Clerk.primary,
        background = ColorPalettes.Clerk.background,
        input = ColorPalettes.Clerk.input,
        danger = ColorPalettes.Clerk.danger,
        success = ColorPalettes.Clerk.success,
        warning = ColorPalettes.Clerk.warning,
        foreground = ColorPalettes.Clerk.foreground,
        mutedForeground = ColorPalettes.Clerk.mutedForeground,
        primaryForeground = ColorPalettes.Clerk.primaryForeground,
        inputForeground = ColorPalettes.Clerk.inputForeground,
        neutral = ColorPalettes.Clerk.neutral,
        border = ColorPalettes.Clerk.border,
        shadow = ColorPalettes.Clerk.shadow,
        ring = ColorPalettes.Clerk.ring,
        muted = ColorPalettes.Clerk.muted,
      )
  }
}

val LocalClerkTheme =
  staticCompositionLocalOf<ClerkComposeTheme> { error("No ComposeTheme provided. ") }

@Composable
fun ClerkTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {

  val clerkTheme =
    Clerk.customTheme
      ?: if (darkTheme) {
        ClerkComposeTheme.dark()
      } else {
        ClerkComposeTheme.light()
      }

  val colorScheme =
    if (darkTheme) {
      darkColorScheme(
        primary = clerkTheme.primary,
        background = clerkTheme.background,
        surface = clerkTheme.input,
        error = clerkTheme.danger,
        onPrimary = clerkTheme.primaryForeground,
        onBackground = clerkTheme.foreground,
        onSurface = clerkTheme.inputForeground,
        outline = clerkTheme.border,
      )
    } else {
      lightColorScheme(
        primary = clerkTheme.primary,
        background = clerkTheme.background,
        surface = clerkTheme.input,
        error = clerkTheme.danger,
        onPrimary = clerkTheme.primaryForeground,
        onBackground = clerkTheme.foreground,
        onSurface = clerkTheme.inputForeground,
        outline = clerkTheme.border,
      )
    }

  CompositionLocalProvider(LocalClerkTheme provides clerkTheme) {
    MaterialTheme(colorScheme = colorScheme) { content() }
  }
}

val clerkTheme: ClerkComposeTheme
  @Composable @ReadOnlyComposable get() = LocalClerkTheme.current
