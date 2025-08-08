package com.clerk.api.ui

import androidx.annotation.FontRes

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
 * Notes:
 * - Size is expressed as a floating-point number (commonly interpreted as points by consumers); UI
 *   layers should convert to the appropriate unit (e.g., sp) during mapping.
 * - Weight values are provided via the [ClerkFontWeight] enum which wraps named constants defined
 *   in [ClerkFontWeights] instead of using hard-coded numbers.
 */
class ClerkTypography(
  val displayLarge: Font,
  val displayMedium: Font,
  val displaySmall: Font,
  val headlineLarge: Font,
  val headlineMedium: Font,
  val headlineSmall: Font,
  val titleLarge: Font,
  val titleMedium: Font,
  val titleSmall: Font,
  val bodyLarge: Font,
  val bodyMedium: Font,
  val bodySmall: Font,
  val labelLarge: Font,
  val labelMedium: Font,
  val labelSmall: Font,
)

/**
 * A simple value object describing a font for a given typography slot.
 *
 * @property fontResId Android font resource ID (e.g., `R.font.inter_regular`). The resource should
 *   be packaged with the app. Alternate platforms can adapt this field as needed.
 * @property size Nominal text size (e.g., in points). UI layers should convert to appropriate
 *   display units (e.g., sp) when constructing platform text styles.
 * @property weight Semantic weight for the style. See [ClerkFontWeight] and [ClerkFontWeights].
 */
data class Font(@FontRes val fontResId: Int, val size: Double, val weight: ClerkFontWeight)

/** Named constants for font weight values to avoid magic numbers. */
object ClerkFontWeights {
  const val THIN = 100
  const val EXTRA_LIGHT = 200
  const val LIGHT = 300
  const val REGULAR = 400
  const val MEDIUM = 500
  const val SEMI_BOLD = 600
  const val BOLD = 700
  const val EXTRA_BOLD = 800
  const val BLACK = 900
}

/**
 * Semantic font weights used across the typography scale. Backed by values from [ClerkFontWeights].
 */
enum class ClerkFontWeight(val value: Int) {
  Thin(ClerkFontWeights.THIN),
  ExtraLight(ClerkFontWeights.EXTRA_LIGHT),
  Light(ClerkFontWeights.LIGHT),
  Regular(ClerkFontWeights.REGULAR),
  Medium(ClerkFontWeights.MEDIUM),
  SemiBold(ClerkFontWeights.SEMI_BOLD),
  Bold(ClerkFontWeights.BOLD),
  ExtraBold(ClerkFontWeights.EXTRA_BOLD),
  Black(ClerkFontWeights.BLACK),
}
