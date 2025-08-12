package com.clerk.api.ui

import androidx.compose.ui.graphics.Color

/**
 * A collection of themed colors used throughout Clerkʼs design system.
 *
 * Each property represents a semantic color token that can be consumed by the UI layer. Rather than
 * referencing hard-coded ARGB values directly in UI code, prefer using these semantic tokens so
 * that color definitions can be swapped out at runtime (e.g. for dark mode) or adjusted centrally
 * without touching all call-sites.
 */
data class ClerkColors(
  /** Main brand color used for primary actions and highlights. */
  val primary: Color? = null,
  /** Default surface background. */
  val background: Color? = null,
  /** Background for input fields such as `TextField`. */
  val input: Color? = null,
  /** Color used to convey destructive or error states. */
  val danger: Color? = null,
  /** Color used to convey success states. */
  val success: Color? = null,
  /** Color used to convey warning states. */
  val warning: Color? = null,
  /** Default foreground (text/icon) color. */
  val foreground: Color? = null,
  /** A slightly subdued foreground color for secondary content. */
  val mutedForeground: Color? = null,
  /** Foreground color that pairs with [primary]. */
  val primaryForeground: Color? = null,
  /** Foreground color that pairs with [input]. */
  val inputForeground: Color? = null,
  /** Neutral gray used for borders or separators. */
  val neutral: Color? = null,
  /** Border color used for input fields and other elements. */
  val border: Color? = null,
  /** Stroke color used for focus rings. */
  val ring: Color? = null,
  /** Muted background color for minimal emphasis surfaces. */
  val muted: Color? = null,
  /** Shadow color used when drawing elevation overlays. */
  val shadow: Color? = null,
)
