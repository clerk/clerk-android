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
 * is represented by a lightweight [Font] that points to a font resource and a nominal size, which
 * downstream can interpret appropriately.
 *
 * Slots correspond roughly to textual hierarchy levels, from larger display styles to small labels:
 * - displayLarge, displayMedium, displaySmall
 * - headlineLarge, headlineMedium, headlineSmall
 * - titleLarge, titleMedium, titleSmall
 * - bodyLarge, bodyMedium, bodySmall
 * - labelLarge, labelMedium, labelSmall
 *
 * Note: Weight, letter-spacing, and other typographic nuances are intentionally not encoded here.
 * They can be supplied by the UI layer when mapping to platform text styles.
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
 * @property fontResId Android font resource ID (e.g., R.font.inter_regular). The resource should be
 *   a valid font resource packaged with the app. If the UI layer uses a different platform, this
 *   can be adapted accordingly by providing an alternative mapping to platform fonts.
 * @property size The nominal point size for the text style. UI layers may choose to convert this to
 *   sp or another appropriate unit.
 */
data class Font(@FontRes val fontResId: Int, val size: Double, val weight: ClerkFontWeight)

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
