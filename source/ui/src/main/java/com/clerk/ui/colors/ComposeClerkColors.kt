package com.clerk.ui.colors

import androidx.compose.ui.graphics.Color
import com.clerk.api.ui.ClerkColors
import com.clerk.ui.theme.toComposeColor

public data class ComposeClerkColors(
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
  companion object {
    internal val light =
      ComposeClerkColors(
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
        shadow = Color(0xFF000000),
        ring = Color(0xFF000000),
        muted = Color(0xFFF7F7F7),
      )

    internal val dark =
      ComposeClerkColors(
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
        shadow = Color(0xFFFFFFFF),
        ring = Color(0xFFFFFFFF),
        muted = Color(0xFF1A1A1D),
      )
  }
}

public data class ComputedColors(
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

public fun ClerkColors.toComposeClerkColors() =
  ComposeClerkColors(
    primary = this.primary.toComposeColor(),
    background = this.background.toComposeColor(),
    input = this.input.toComposeColor(),
    danger = this.danger.toComposeColor(),
    success = this.success.toComposeColor(),
    warning = this.warning.toComposeColor(),
    foreground = this.foreground.toComposeColor(),
    mutedForeground = this.mutedForeground.toComposeColor(),
    primaryForeground = this.primaryForeground.toComposeColor(),
    inputForeground = this.inputForeground.toComposeColor(),
    neutral = this.neutral.toComposeColor(),
    border = this.border.toComposeColor(),
    shadow = this.shadow.toComposeColor(),
    ring = this.ring.toComposeColor(),
    muted = this.muted.toComposeColor(),
  )
