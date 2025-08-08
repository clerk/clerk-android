package com.clerk.api.ui

/**
 * Aggregated theme definition used throughout the Clerk UI layer.
 *
 * This holder groups together the three individual theme configurations that compose the overall
 * look-and-feel of the application:
 * 1. [ClerkColors] – the color palette of the app (primary/secondary/background, etc.).
 * 2. [ClerkFonts] – the typography scale (font families, weights, sizes).
 * 3. [ClerkDesign] – corner radii, spacing, component shapes and other design tokens.
 *
 * Having a single container greatly simplifies passing theme data through composables and other UI
 * components.
 *
 * @property colorConfig colors used by the UI layer.
 * @property fontsConfig fonts and typography definitions.
 * @property designConfig design tokens such as spacing and shapes.
 */
data class ClerkTheme(
  val colorConfig: ClerkColors,
  val fontsConfig: ClerkFonts,
  val designConfig: ClerkDesign,
)
