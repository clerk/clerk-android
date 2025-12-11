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
 * Use the zero-argument constructor (`ClerkTypography()`) to start from Clerk's baseline styles and
 * override only a subset via named parameters. If you need the raw `TextStyle` instances outside of
 * the theme container, reference [ClerkTypographyDefaults] directly.
 *
 * Notes:
 * - Size is expressed as a floating-point number (commonly interpreted as points by consumers); UI
 *   layers should convert to the appropriate unit (e.g., sp) during mapping.
 * - Weight values are provided via the [ClerkFontWeight] enum which wraps named constants defined
 *   in [ClerkFontWeights] instead of using hard-coded numbers.
 */
data class ClerkTypography(
  val displaySmall: TextStyle? = ClerkTypographyDefaults.displaySmall,
  val headlineLarge: TextStyle? = ClerkTypographyDefaults.headlineLarge,
  val headlineMedium: TextStyle? = ClerkTypographyDefaults.headlineMedium,
  val headlineSmall: TextStyle? = ClerkTypographyDefaults.headlineSmall,
  val titleMedium: TextStyle? = ClerkTypographyDefaults.titleMedium,
  val titleSmall: TextStyle? = ClerkTypographyDefaults.titleSmall,
  val bodyLarge: TextStyle? = ClerkTypographyDefaults.bodyLarge,
  val bodyMedium: TextStyle? = ClerkTypographyDefaults.bodyMedium,
  val bodySmall: TextStyle? = ClerkTypographyDefaults.bodySmall,
  val labelMedium: TextStyle? = ClerkTypographyDefaults.labelMedium,
  val labelSmall: TextStyle? = ClerkTypographyDefaults.labelSmall,
)
