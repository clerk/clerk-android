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
public object ClerkTypographyDefaults {

  private val defaultFontFamily = FontFamily.Default

  public val displaySmall: TextStyle =
    TextStyle(
      fontFamily = defaultFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = 36.sp,
      lineHeight = 44.sp,
      letterSpacing = 0.sp,
    )

  public val headlineLarge: TextStyle =
    TextStyle(
      fontFamily = defaultFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = 32.sp,
      lineHeight = 40.sp,
      letterSpacing = 0.sp,
    )

  public val headlineMedium: TextStyle =
    TextStyle(
      fontFamily = defaultFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = 28.sp,
      lineHeight = 36.sp,
      letterSpacing = 0.sp,
    )

  public val headlineSmall: TextStyle =
    TextStyle(
      fontFamily = defaultFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = 24.sp,
      lineHeight = 32.sp,
      letterSpacing = 0.sp,
    )

  public val titleMedium: TextStyle =
    TextStyle(
      fontFamily = defaultFontFamily,
      fontWeight = FontWeight.Medium,
      fontSize = 16.sp,
      lineHeight = 24.sp,
      letterSpacing = 0.15.sp,
    )

  public val titleSmall: TextStyle =
    TextStyle(
      fontFamily = defaultFontFamily,
      fontWeight = FontWeight.Medium,
      fontSize = 14.sp,
      lineHeight = 20.sp,
      letterSpacing = 0.1.sp,
    )

  public val bodyLarge: TextStyle =
    TextStyle(
      fontFamily = defaultFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = 16.sp,
      lineHeight = 24.sp,
      letterSpacing = 0.5.sp,
    )

  public val bodyMedium: TextStyle =
    TextStyle(
      fontFamily = defaultFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = 14.sp,
      lineHeight = 20.sp,
      letterSpacing = 0.25.sp,
    )

  public val bodySmall: TextStyle =
    TextStyle(
      fontFamily = defaultFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = 12.sp,
      lineHeight = 16.sp,
      letterSpacing = 0.4.sp,
    )

  public val labelMedium: TextStyle =
    TextStyle(
      fontFamily = defaultFontFamily,
      fontWeight = FontWeight.Medium,
      fontSize = 12.sp,
      lineHeight = 16.sp,
      letterSpacing = 0.5.sp,
    )

  public val labelSmall: TextStyle =
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
  public fun typography(builder: ClerkTypographyBuilder.() -> Unit = {}): ClerkTypography {
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

  /** Convenience helper for retrieving the full default [ClerkTypography] without any overrides. */
  public fun default(): ClerkTypography = typography()
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
public class ClerkTypographyBuilder
internal constructor(
  public var displaySmall: TextStyle,
  public var headlineLarge: TextStyle,
  public var headlineMedium: TextStyle,
  public var headlineSmall: TextStyle,
  public var titleMedium: TextStyle,
  public var titleSmall: TextStyle,
  public var bodyLarge: TextStyle,
  public var bodyMedium: TextStyle,
  public var bodySmall: TextStyle,
  public var labelMedium: TextStyle,
  public var labelSmall: TextStyle,
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
