package com.clerk.api.ui

/**
 * Aggregated theme definition used throughout the Clerk UI layer.
 *
 * This holder groups together the three individual theme configurations that compose the overall
 * look-and-feel of the application:
 * 1. [ClerkColors] – the color palette of the app (primary/secondary/background, etc.).
 * 2. [ClerkTypography] – the typography scale (font families, weights, sizes).
 * 3. [ClerkDesign] – corner radii, spacing, component shapes and other design tokens.
 *
 * Having a single container greatly simplifies passing theme data through composables and other UI
 * components.
 *
 * @property colors colors used by the UI layer across light and dark modes.
 * @property lightColors optional overrides that only apply when the system is in light mode.
 * @property darkColors optional overrides that only apply when the system is in dark mode.
 * @property typography fonts and typography definitions.
 * @property design design tokens such as spacing and shapes.
 */
data class ClerkTheme(
  val colors: ClerkColors? = null,
  val lightColors: ClerkColors? = null,
  val darkColors: ClerkColors? = null,
  val typography: ClerkTypography? = null,
  val design: ClerkDesign? = null,
)
