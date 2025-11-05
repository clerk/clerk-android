package com.clerk.ui.theme.colors

import androidx.compose.ui.graphics.Color

/**
 * Computed color variants derived from the base Clerk colors. These colors are calculated at
 * runtime based on the current theme and system settings.
 *
 * Access this through [ClerkMaterialTheme.computedColors] within a `ClerkMaterialTheme` scope.
 */
public data class ComputedColors(
  /** Primary color in pressed state (lightened or darkened based on theme) */
  val primaryPressed: Color,
  /** Subtle border color with reduced opacity */
  val border: Color,
  /** Button border color with specific opacity */
  val buttonBorder: Color,
  /** Input field border color */
  val inputBorder: Color,
  /** Focused input field border color */
  val inputBorderFocused: Color,
  /** Danger state input border color */
  val dangerInputBorder: Color,
  /** Focused danger state input border color */
  val dangerInputBorderFocused: Color,
  /** Semi-transparent background color */
  val backgroundTransparent: Color,
  /** Success state background color */
  val backgroundSuccess: Color,
  /** Success state border color */
  val borderSuccess: Color,
  /** Danger state background color */
  val backgroundDanger: Color,
  /** Danger state border color */
  val borderDanger: Color,
  /** Warning state background color */
  val backgroundWarning: Color,
  /** Warning state border color */
  val borderWarning: Color,
)
