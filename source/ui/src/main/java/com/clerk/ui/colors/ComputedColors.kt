package com.clerk.ui.colors

import android.annotation.SuppressLint
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

// Internal tuning constants for computed colors
internal object ComputedColorConstants {
  const val PRIMARY_PRESS_DELTA = 0.06f
  const val BORDER_OPACITY = 0.06f
  const val BUTTON_BORDER_OPACITY = 0.08f
  const val INPUT_BORDER_OPACITY = 0.11f
  const val INPUT_BORDER_FOCUSED_OPACITY = 0.28f
  const val DANGER_INPUT_BORDER_OPACITY = 0.53f
  const val DANGER_INPUT_BORDER_FOCUSED_OPACITY = 0.15f
  const val BACKGROUND_TRANSPARENT_OPACITY = 0.5f
  const val BACKGROUND_SUCCESS_OPACITY = 0.12f
  const val BORDER_SUCCESS_OPACITY = 0.77f
  const val BACKGROUND_DANGER_OPACITY = 0.12f
  const val BORDER_DANGER_OPACITY = 0.77f
  const val BACKGROUND_WARNING_OPACITY = 0.12f
  const val BORDER_WARNING_OPACITY = 0.77f
  const val DARK_LUMA_THRESHOLD = 0.5f

  // sRGB luminance coefficients
  const val LUMA_R = 0.2126f
  const val LUMA_G = 0.7152f
  const val LUMA_B = 0.0722f
}

// Internal-only computed colors for UI components. Mirrors the Swift computed set.
internal data class UiComputedColors(
  val primaryPressed: Color,
  val border: Color,
  val buttonBorder: Color,
  val inputBorder: Color,
  val inputBorderFocused: Color,
  val dangerInputBorder: Color,
  val dangerInputBorderFocused: Color,
  val backgroundTransparent: Color,
  val backgroundSuccess: Color,
  val borderSuccess: Color,
  val backgroundDanger: Color,
  val borderDanger: Color,
  val backgroundWarning: Color,
  val borderWarning: Color,
)

// Lightweight helpers: opacity and simple lighten/darken using lerp.
private fun Color.withOpacity(fraction: Float): Color =
  this.copy(alpha = (fraction).coerceIn(0f, 1f))

private fun Color.isDark(): Boolean {
  // sRGB relative luminance approximation
  val r = red
  val g = green
  val b = blue
  val luma =
    ComputedColorConstants.LUMA_R * r +
      ComputedColorConstants.LUMA_G * g +
      ComputedColorConstants.LUMA_B * b
  return luma < ComputedColorConstants.DARK_LUMA_THRESHOLD
}

private fun Color.lighten(by: Float): Color = lerp(this, Color.White, by.coerceIn(0f, 1f))

private fun Color.darken(by: Float): Color = lerp(this, Color.Black, by.coerceIn(0f, 1f))

// Map base palette colors to computed set.
internal fun BaseColors.toComputed(): UiComputedColors {
  val primaryPressed =
    if (primary.isDark()) primary.lighten(ComputedColorConstants.PRIMARY_PRESS_DELTA)
    else primary.darken(ComputedColorConstants.PRIMARY_PRESS_DELTA)
  return UiComputedColors(
    primaryPressed = primaryPressed,
    border = border.withOpacity(ComputedColorConstants.BORDER_OPACITY),
    buttonBorder = border.withOpacity(ComputedColorConstants.BUTTON_BORDER_OPACITY),
    inputBorder = border.withOpacity(ComputedColorConstants.INPUT_BORDER_OPACITY),
    inputBorderFocused = ring.withOpacity(ComputedColorConstants.INPUT_BORDER_FOCUSED_OPACITY),
    dangerInputBorder = danger.withOpacity(ComputedColorConstants.DANGER_INPUT_BORDER_OPACITY),
    dangerInputBorderFocused =
      danger.withOpacity(ComputedColorConstants.DANGER_INPUT_BORDER_FOCUSED_OPACITY),
    backgroundTransparent =
      background.withOpacity(ComputedColorConstants.BACKGROUND_TRANSPARENT_OPACITY),
    backgroundSuccess = success.withOpacity(ComputedColorConstants.BACKGROUND_SUCCESS_OPACITY),
    borderSuccess = success.withOpacity(ComputedColorConstants.BORDER_SUCCESS_OPACITY),
    backgroundDanger = danger.withOpacity(ComputedColorConstants.BACKGROUND_DANGER_OPACITY),
    borderDanger = danger.withOpacity(ComputedColorConstants.BORDER_DANGER_OPACITY),
    backgroundWarning = warning.withOpacity(ComputedColorConstants.BACKGROUND_WARNING_OPACITY),
    borderWarning = warning.withOpacity(ComputedColorConstants.BORDER_WARNING_OPACITY),
  )
}

// Minimal base colors contract the theme can map from ColorPalettes.
internal data class BaseColors(
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

// Internal CompositionLocal for computed colors.
@SuppressLint("ComposeCompositionLocalUsage")
internal val LocalComputedColors =
  staticCompositionLocalOf<UiComputedColors> {
    // Provide a safe light default using the Clerk palette as baseline
    val base =
      BaseColors(
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
        ring = ColorPalettes.Clerk.ring,
        muted = ColorPalettes.Clerk.muted,
        shadow = ColorPalettes.Clerk.shadow,
      )
    base.toComputed()
  }
