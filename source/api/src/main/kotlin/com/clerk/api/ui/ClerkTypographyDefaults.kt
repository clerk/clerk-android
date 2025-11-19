@file:Suppress("MagicNumber")

package com.clerk.api.ui

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Provides the default typography values used by Clerk UI components.
 *
 * Use these values when you need to tweak only a subset of the typography scale while keeping the
 * remaining slots aligned with Clerk's defaults. This makes it easy to take an existing style and
 * call `copy(...)` with your customization.
 *
 * ```
 * val customTypography =
 *   ClerkTypographyDefaults.typography {
 *     displaySmall = displaySmall.copy(fontWeight = FontWeight.SemiBold)
 *   }
 *
 * ClerkTheme(typography = customTypography)
 * ```
 */
object ClerkTypographyDefaults {

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

  /**
   * Returns a [ClerkTypography] where every slot is pre-populated with Clerk's defaults.
   *
   * Apply [builder] to override any slots you want to customize.
   */
  fun typography(builder: ClerkTypographyBuilder.() -> Unit = {}): ClerkTypography {
    val scope =
      ClerkTypographyBuilder(
        displaySmall = displaySmall,
        headlineLarge = headlineLarge,
        headlineMedium = headlineMedium,
        headlineSmall = headlineSmall,
        titleMedium = titleMedium,
        titleSmall = titleSmall,
        bodyLarge = bodyLarge,
        bodyMedium = bodyMedium,
        bodySmall = bodySmall,
        labelMedium = labelMedium,
        labelSmall = labelSmall,
      )
    scope.builder()
    return scope.build()
  }

  /**
   * Convenience helper for retrieving the full default [ClerkTypography] without any overrides.
   */
  fun default(): ClerkTypography = typography()
}

/**
 * Builder used by [ClerkTypographyDefaults.typography] to let callers selectively override values.
 *
 * ```
 * val typography = ClerkTypographyDefaults.typography {
 *   titleMedium = titleMedium.copy(fontSize = 18.sp)
 * }
 * ```
 */
class ClerkTypographyBuilder internal constructor(
  var displaySmall: TextStyle,
  var headlineLarge: TextStyle,
  var headlineMedium: TextStyle,
  var headlineSmall: TextStyle,
  var titleMedium: TextStyle,
  var titleSmall: TextStyle,
  var bodyLarge: TextStyle,
  var bodyMedium: TextStyle,
  var bodySmall: TextStyle,
  var labelMedium: TextStyle,
  var labelSmall: TextStyle,
) {

  internal fun build(): ClerkTypography =
    ClerkTypography(
      displaySmall = displaySmall,
      headlineLarge = headlineLarge,
      headlineMedium = headlineMedium,
      headlineSmall = headlineSmall,
      titleMedium = titleMedium,
      titleSmall = titleSmall,
      bodyLarge = bodyLarge,
      bodyMedium = bodyMedium,
      bodySmall = bodySmall,
      labelMedium = labelMedium,
      labelSmall = labelSmall,
    )
}
