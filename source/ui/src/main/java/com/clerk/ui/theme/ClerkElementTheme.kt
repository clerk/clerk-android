package com.clerk.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.clerk.api.ui.ClerkColors
import com.clerk.api.ui.ClerkDesign
import com.clerk.ui.theme.colors.ComputedColors

/**
 * A theme override that can be applied to individual UI elements.
 *
 * This allows you to customize the appearance of specific components without affecting the global
 * theme. Any properties set to `null` will inherit from the current [ClerkMaterialTheme].
 *
 * Example usage:
 * ```kotlin
 * ClerkButton(
 *   text = "Custom Button",
 *   onClick = {},
 *   elementTheme = ClerkElementTheme(
 *     colors = ClerkElementColors(primary = Color.Red)
 *   )
 * )
 * ```
 */
@Immutable
data class ClerkElementTheme(
  /**
   * Optional color overrides for this element. Only non-null values will override the theme.
   */
  val colors: ClerkElementColors? = null,

  /**
   * Optional typography overrides for this element. Only non-null values will override the theme.
   */
  val typography: ClerkElementTypography? = null,

  /**
   * Optional design token overrides for this element. Only non-null values will override the theme.
   */
  val design: ClerkElementDesign? = null,

  /**
   * Optional computed color overrides for this element. Only non-null values will override the theme.
   */
  val computedColors: ClerkElementComputedColors? = null,
)

/**
 * Partial color overrides for an element theme.
 *
 * All properties are optional. Only non-null values will override the current theme.
 */
@Immutable
data class ClerkElementColors(
  val primary: Color? = null,
  val background: Color? = null,
  val input: Color? = null,
  val danger: Color? = null,
  val success: Color? = null,
  val warning: Color? = null,
  val foreground: Color? = null,
  val mutedForeground: Color? = null,
  val primaryForeground: Color? = null,
  val inputForeground: Color? = null,
  val neutral: Color? = null,
  val border: Color? = null,
  val ring: Color? = null,
  val muted: Color? = null,
  val shadow: Color? = null,
)

/**
 * Partial typography overrides for an element theme.
 *
 * All properties are optional. Only non-null values will override the current theme.
 */
@Immutable
data class ClerkElementTypography(
  val displayLarge: TextStyle? = null,
  val displayMedium: TextStyle? = null,
  val displaySmall: TextStyle? = null,
  val headlineLarge: TextStyle? = null,
  val headlineMedium: TextStyle? = null,
  val headlineSmall: TextStyle? = null,
  val titleLarge: TextStyle? = null,
  val titleMedium: TextStyle? = null,
  val titleSmall: TextStyle? = null,
  val bodyLarge: TextStyle? = null,
  val bodyMedium: TextStyle? = null,
  val bodySmall: TextStyle? = null,
  val labelLarge: TextStyle? = null,
  val labelMedium: TextStyle? = null,
  val labelSmall: TextStyle? = null,
)

/**
 * Partial design token overrides for an element theme.
 *
 * All properties are optional. Only non-null values will override the current theme.
 */
@Immutable
data class ClerkElementDesign(
  val borderRadius: Dp? = null,
)

/**
 * Partial computed color overrides for an element theme.
 *
 * All properties are optional. Only non-null values will override the current theme.
 */
@Immutable
data class ClerkElementComputedColors(
  val primaryPressed: Color? = null,
  val border: Color? = null,
  val buttonBorder: Color? = null,
  val inputBorder: Color? = null,
  val inputBorderFocused: Color? = null,
  val dangerInputBorder: Color? = null,
  val dangerInputBorderFocused: Color? = null,
  val backgroundTransparent: Color? = null,
  val backgroundSuccess: Color? = null,
  val borderSuccess: Color? = null,
  val backgroundDanger: Color? = null,
  val borderDanger: Color? = null,
  val backgroundWarning: Color? = null,
  val borderWarning: Color? = null,
)

/**
 * Merges an element theme with the current ClerkMaterialTheme, returning merged theme values.
 *
 * This function takes the current theme values and applies any non-null overrides from the element
 * theme, creating a merged theme that can be used within a component.
 *
 * @param elementTheme The element theme containing optional overrides, or null to use the current theme.
 * @return A [MergedElementTheme] containing the merged theme values.
 */
@Composable
internal fun mergeElementTheme(elementTheme: ClerkElementTheme?): MergedElementTheme {
  if (elementTheme == null) {
    return MergedElementTheme(
      colors = ClerkMaterialTheme.colors,
      typography = ClerkMaterialTheme.typography,
      design = ClerkMaterialTheme.design,
      computedColors = ClerkMaterialTheme.computedColors,
    )
  }

  val baseColors = ClerkMaterialTheme.colors
  val elementColors = elementTheme.colors
  val mergedColors =
    ClerkThemeColors(
      ClerkColors(
        primary = elementColors?.primary ?: baseColors.primary,
        background = elementColors?.background ?: baseColors.background,
        input = elementColors?.input ?: baseColors.input,
        danger = elementColors?.danger ?: baseColors.danger,
        success = elementColors?.success ?: baseColors.success,
        warning = elementColors?.warning ?: baseColors.warning,
        foreground = elementColors?.foreground ?: baseColors.foreground,
        mutedForeground = elementColors?.mutedForeground ?: baseColors.mutedForeground,
        primaryForeground = elementColors?.primaryForeground ?: baseColors.primaryForeground,
        inputForeground = elementColors?.inputForeground ?: baseColors.inputForeground,
        neutral = elementColors?.neutral ?: baseColors.neutral,
        border = elementColors?.border ?: baseColors.border,
        ring = elementColors?.ring ?: baseColors.ring,
        muted = elementColors?.muted ?: baseColors.muted,
        shadow = elementColors?.shadow ?: baseColors.shadow,
      ),
    )

  val baseTypography = ClerkMaterialTheme.typography
  val elementTypography = elementTheme.typography
  val mergedTypography =
    Typography(
      displayLarge = elementTypography?.displayLarge ?: baseTypography.displayLarge,
      displayMedium = elementTypography?.displayMedium ?: baseTypography.displayMedium,
      displaySmall = elementTypography?.displaySmall ?: baseTypography.displaySmall,
      headlineLarge = elementTypography?.headlineLarge ?: baseTypography.headlineLarge,
      headlineMedium = elementTypography?.headlineMedium ?: baseTypography.headlineMedium,
      headlineSmall = elementTypography?.headlineSmall ?: baseTypography.headlineSmall,
      titleLarge = elementTypography?.titleLarge ?: baseTypography.titleLarge,
      titleMedium = elementTypography?.titleMedium ?: baseTypography.titleMedium,
      titleSmall = elementTypography?.titleSmall ?: baseTypography.titleSmall,
      bodyLarge = elementTypography?.bodyLarge ?: baseTypography.bodyLarge,
      bodyMedium = elementTypography?.bodyMedium ?: baseTypography.bodyMedium,
      bodySmall = elementTypography?.bodySmall ?: baseTypography.bodySmall,
      labelLarge = elementTypography?.labelLarge ?: baseTypography.labelLarge,
      labelMedium = elementTypography?.labelMedium ?: baseTypography.labelMedium,
      labelSmall = elementTypography?.labelSmall ?: baseTypography.labelSmall,
    )

  val baseDesign = ClerkMaterialTheme.design
  val elementDesign = elementTheme.design
  val mergedDesign =
    ClerkDesign(
      borderRadius = elementDesign?.borderRadius ?: baseDesign.borderRadius,
    )

  val baseComputedColors = ClerkMaterialTheme.computedColors
  val elementComputedColors = elementTheme.computedColors
  val mergedComputedColors =
    ComputedColors(
      primaryPressed = elementComputedColors?.primaryPressed ?: baseComputedColors.primaryPressed,
      border = elementComputedColors?.border ?: baseComputedColors.border,
      buttonBorder = elementComputedColors?.buttonBorder ?: baseComputedColors.buttonBorder,
      inputBorder = elementComputedColors?.inputBorder ?: baseComputedColors.inputBorder,
      inputBorderFocused =
        elementComputedColors?.inputBorderFocused ?: baseComputedColors.inputBorderFocused,
      dangerInputBorder =
        elementComputedColors?.dangerInputBorder ?: baseComputedColors.dangerInputBorder,
      dangerInputBorderFocused =
        elementComputedColors?.dangerInputBorderFocused
          ?: baseComputedColors.dangerInputBorderFocused,
      backgroundTransparent =
        elementComputedColors?.backgroundTransparent ?: baseComputedColors.backgroundTransparent,
      backgroundSuccess =
        elementComputedColors?.backgroundSuccess ?: baseComputedColors.backgroundSuccess,
      borderSuccess = elementComputedColors?.borderSuccess ?: baseComputedColors.borderSuccess,
      backgroundDanger =
        elementComputedColors?.backgroundDanger ?: baseComputedColors.backgroundDanger,
      borderDanger = elementComputedColors?.borderDanger ?: baseComputedColors.borderDanger,
      backgroundWarning =
        elementComputedColors?.backgroundWarning ?: baseComputedColors.backgroundWarning,
      borderWarning = elementComputedColors?.borderWarning ?: baseComputedColors.borderWarning,
    )

  return MergedElementTheme(
    colors = mergedColors,
    typography = mergedTypography,
    design = mergedDesign,
    computedColors = mergedComputedColors,
  )
}

/**
 * Internal data class holding merged theme values for an element.
 *
 * This is used internally by components to access theme values that have been merged with element
 * theme overrides.
 */
@Immutable
internal data class MergedElementTheme(
  val colors: ClerkThemeColors,
  val typography: Typography,
  val design: ClerkDesign,
  val computedColors: ComputedColors,
)
