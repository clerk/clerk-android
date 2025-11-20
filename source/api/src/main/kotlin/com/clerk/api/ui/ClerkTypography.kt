@file:Suppress("MagicNumber")

package com.clerk.api.ui

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

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
 * Use [ClerkTypographyDefaults] or the zero-argument constructor (`ClerkTypography()`) when you need
 * to start from Clerk's baseline styles and override only a subset.
 *
 * Notes:
 * - Size is expressed as a floating-point number (commonly interpreted as points by consumers); UI
 *   layers should convert to the appropriate unit (e.g., sp) during mapping.
 * - Weight values are provided via the [ClerkFontWeight] enum which wraps named constants defined
 *   in [ClerkFontWeights] instead of using hard-coded numbers.
 */
data class ClerkTypography(
  val displaySmall: TextStyle = ClerkTypographyBaseline.displaySmall,
  val headlineLarge: TextStyle = ClerkTypographyBaseline.headlineLarge,
  val headlineMedium: TextStyle = ClerkTypographyBaseline.headlineMedium,
  val headlineSmall: TextStyle = ClerkTypographyBaseline.headlineSmall,
  val titleMedium: TextStyle = ClerkTypographyBaseline.titleMedium,
  val titleSmall: TextStyle = ClerkTypographyBaseline.titleSmall,
  val bodyLarge: TextStyle = ClerkTypographyBaseline.bodyLarge,
  val bodyMedium: TextStyle = ClerkTypographyBaseline.bodyMedium,
  val bodySmall: TextStyle = ClerkTypographyBaseline.bodySmall,
  val labelMedium: TextStyle = ClerkTypographyBaseline.labelMedium,
  val labelSmall: TextStyle = ClerkTypographyBaseline.labelSmall,
)

internal object ClerkTypographyBaseline {
  private val defaultFontFamily = FontFamily.Default

  val displaySmall: TextStyle =
    TextStyle(
      fontFamily = defaultFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = 36.sp,
      lineHeight = 44.sp,
      letterSpacing = 0.sp,
    )

  val headlineLarge: TextStyle =
    TextStyle(
      fontFamily = defaultFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = 32.sp,
      lineHeight = 40.sp,
      letterSpacing = 0.sp,
    )

  val headlineMedium: TextStyle =
    TextStyle(
      fontFamily = defaultFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = 28.sp,
      lineHeight = 36.sp,
      letterSpacing = 0.sp,
    )

  val headlineSmall: TextStyle =
    TextStyle(
      fontFamily = defaultFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = 24.sp,
      lineHeight = 32.sp,
      letterSpacing = 0.sp,
    )

  val titleMedium: TextStyle =
    TextStyle(
      fontFamily = defaultFontFamily,
      fontWeight = FontWeight.Medium,
      fontSize = 16.sp,
      lineHeight = 24.sp,
      letterSpacing = 0.15.sp,
    )

  val titleSmall: TextStyle =
    TextStyle(
      fontFamily = defaultFontFamily,
      fontWeight = FontWeight.Medium,
      fontSize = 14.sp,
      lineHeight = 20.sp,
      letterSpacing = 0.1.sp,
    )

  val bodyLarge: TextStyle =
    TextStyle(
      fontFamily = defaultFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = 16.sp,
      lineHeight = 24.sp,
      letterSpacing = 0.5.sp,
    )

  val bodyMedium: TextStyle =
    TextStyle(
      fontFamily = defaultFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = 14.sp,
      lineHeight = 20.sp,
      letterSpacing = 0.25.sp,
    )

  val bodySmall: TextStyle =
    TextStyle(
      fontFamily = defaultFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = 12.sp,
      lineHeight = 16.sp,
      letterSpacing = 0.4.sp,
    )

  val labelMedium: TextStyle =
    TextStyle(
      fontFamily = defaultFontFamily,
      fontWeight = FontWeight.Medium,
      fontSize = 12.sp,
      lineHeight = 16.sp,
      letterSpacing = 0.5.sp,
    )

  val labelSmall: TextStyle =
    TextStyle(
      fontFamily = defaultFontFamily,
      fontWeight = FontWeight.Medium,
      fontSize = 11.sp,
      lineHeight = 16.sp,
      letterSpacing = 0.5.sp,
    )
}
