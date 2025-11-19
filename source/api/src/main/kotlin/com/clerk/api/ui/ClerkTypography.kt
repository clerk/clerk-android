package com.clerk.api.ui

import androidx.compose.ui.text.TextStyle

/**
 * Platform-agnostic description of a complete typography scale used by Clerk UI components.
 *
 * This container mirrors the Material3 typography slots to make it straightforward for UI
 * implementations (Compose/View) to map these definitions to concrete platform `TextStyle`s or
 * `Typeface`s.
 *
 * Unlike UI-layer types, this API layer does not prescribe a specific rendering engine. Each entry
 * is represented by a lightweight [Font] that points to a font resource, a nominal size, and a
 * semantic weight ([ClerkFontWeight]) that downstream code can translate to platform equivalents.
 *
 * Slots correspond roughly to textual hierarchy levels, from larger display styles to small labels:
 * - displayLarge, displayMedium, displaySmall
 * - headlineLarge, headlineMedium, headlineSmall
 * - titleLarge, titleMedium, titleSmall
 * - bodyLarge, bodyMedium, bodySmall
 * - labelLarge, labelMedium, labelSmall
 *
 * Use [ClerkTypographyDefaults] when you need to start from Clerk's baseline styles and override
 * only a subset.
 *
 * Notes:
 * - Size is expressed as a floating-point number (commonly interpreted as points by consumers); UI
 *   layers should convert to the appropriate unit (e.g., sp) during mapping.
 * - Weight values are provided via the [ClerkFontWeight] enum which wraps named constants defined
 *   in [ClerkFontWeights] instead of using hard-coded numbers.
 */
class ClerkTypography(
  val displaySmall: TextStyle? = null,
  val headlineLarge: TextStyle? = null,
  val headlineMedium: TextStyle? = null,
  val headlineSmall: TextStyle? = null,
  val titleMedium: TextStyle? = null,
  val titleSmall: TextStyle? = null,
  val bodyLarge: TextStyle? = null,
  val bodyMedium: TextStyle? = null,
  val bodySmall: TextStyle? = null,
  val labelMedium: TextStyle? = null,
  val labelSmall: TextStyle? = null,
)
