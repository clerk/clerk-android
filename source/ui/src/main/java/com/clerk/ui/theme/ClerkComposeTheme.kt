@file:SuppressLint("ComposeCompositionLocalUsage")

package com.clerk.ui.theme

import android.annotation.SuppressLint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.clerk.api.Clerk
import com.clerk.api.ui.ClerkColors
import com.clerk.api.ui.ClerkDesign
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.colors.ComputedColors
import com.materialkolor.ktx.darken
import com.materialkolor.ktx.lighten

// Theme constants
private const val PRIMARY_PRESSED_FACTOR = 0.06F
private const val BORDER_ALPHA_SUBTLE = 0.06F
private const val BUTTON_BORDER_ALPHA = 0.08F
private const val INPUT_BORDER_ALPHA = 0.11F
private const val INPUT_BORDER_FOCUSED_ALPHA = 0.28F
private const val DANGER_INPUT_BORDER_ALPHA = 0.53F
private const val DANGER_INPUT_BORDER_FOCUSED_ALPHA = 0.15F
private const val BACKGROUND_TRANSPARENT_ALPHA = 0.5F
private const val SUCCESS_BACKGROUND_ALPHA = 0.12F
private const val SUCCESS_BORDER_ALPHA = 0.77F
private const val DANGER_BACKGROUND_ALPHA = 0.12F
private const val DANGER_BORDER_ALPHA = 0.77F
private const val WARNING_BACKGROUND_ALPHA = 0.12F
private const val WARNING_BORDER_ALPHA = 0.77F

internal val LocalComposeColors =
  compositionLocalOf<ClerkColors> { error("ComposeColors not provided") }

internal val LocalComputedColors =
  compositionLocalOf<ComputedColors> { error("ComputedColors not provided") }

internal val LocalClerkDesign =
  compositionLocalOf<ClerkDesign> { error("ClerkDesign not provided") }

/**
 * Clerk Theming refers to the customization of your Clerk components to better reflect your
 * product's brand.
 *
 * Clerk components such as [com.clerk.ui.core.button.standard.ClerkButton] and
 * [com.clerk.ui.core.input.ClerkTextField] use values provided here when retrieving default values.
 * This theme system seamlessly integrates with Material3 theming while providing Clerk-specific
 * design tokens and computed colors.
 *
 * All values may be set by providing this component with a [ClerkTheme]. Use this to configure the
 * overall theme of elements within this ClerkMaterialTheme.
 *
 * Any values that are not set will inherit the current value from the theme, falling back to the
 * defaults if there is no parent ClerkMaterialTheme. This allows using a ClerkMaterialTheme at the
 * top of your application, and then separate ClerkMaterialTheme(s) for different screens / parts of
 * your UI, overriding only the parts of the theme definition that need to change.
 *
 * The theme automatically computes Material3 color schemes from Clerk colors and provides
 * additional computed color variants optimized for common UI patterns like pressed states, borders,
 * and semantic colors.
 *
 * ## Theme Access
 *
 * Use the [ClerkMaterialTheme] object to access theme values within your composables:
 * ```kotlin
 * @Composable
 * fun MyComponent() {
 *   ClerkMaterialTheme {
 *     val primary = ClerkMaterialTheme.colors.primary
 *     val pressedColor = ClerkMaterialTheme.computed.primaryPressed
 *     val borderRadius = ClerkMaterialTheme.design.cornerRadius
 *
 *     // Use the values in your component
 *     Box(
 *       modifier = Modifier.background(primary),
 *       content = content
 *     )
 *   }
 * }
 * ```
 *
 * ## Integration with Material3
 *
 * This theme provider automatically configures Material3's [MaterialTheme] with colors derived from
 * your Clerk theme, ensuring seamless integration between Clerk and Material components.
 *
 * @param clerkTheme The Clerk theme to apply. If null, uses the globally configured theme from
 *   [Clerk.customTheme] or system defaults.
 * @param content The composable content that will have access to the themed values.
 */
@Composable
internal fun ClerkMaterialTheme(
  clerkTheme: ClerkTheme? = Clerk.customTheme,
  content: @Composable () -> Unit,
) {
  ClerkThemeProvider(theme = clerkTheme) {
    val colors = ClerkThemeProviderAccess.colors
    val design = ClerkThemeProviderAccess.design

    val materialColors = computeColorScheme(colors)
    val computedColors = generateComputedColors(colors)

    CompositionLocalProvider(
      LocalComposeColors provides colors,
      LocalComputedColors provides computedColors,
      LocalClerkDesign provides design,
    ) {
      MaterialTheme(
        colorScheme = materialColors,
        typography = ClerkThemeProviderAccess.typography,
      ) {
        content()
      }
    }
  }
}

/**
 * Contains functions to access the current theme values provided at the call site's position in the
 * hierarchy.
 *
 * This object provides a clean API for accessing Clerk theme values, similar to how MaterialTheme
 * exposes its values. Use these properties within @Composable functions to access the current theme
 * configuration.
 */
object ClerkMaterialTheme {

  /**
   * Retrieves the current [ClerkColors] at the call site's position in the hierarchy.
   *
   * These are the base Clerk colors defined in the theme configuration, including primary,
   * background, foreground, and semantic colors like success, danger, and warning.
   */
  val colors: ClerkColors
    @Composable @ReadOnlyComposable get() = LocalComposeColors.current

  /**
   * Retrieves the current [Typography] at the call site's position in the hierarchy.
   *
   * This returns the Material3 Typography that has been configured for the Clerk theme, providing
   * consistent text styling across all components.
   */
  val typography: Typography
    @Composable get() = ClerkThemeProviderAccess.typography

  /**
   * Retrieves the current [ClerkDesign] at the call site's position in the hierarchy.
   *
   * This provides access to design tokens like corner radius, spacing, and other layout properties
   * that define the visual structure of Clerk components.
   */
  val design: ClerkDesign
    @Composable @ReadOnlyComposable get() = LocalClerkDesign.current

  /**
   * Retrieves a [RoundedCornerShape] computed from the current design's border radius.
   *
   * This provides a convenient way to access a consistent rounded corner shape based on the theme's
   * configured border radius, commonly used for buttons, cards, and input fields.
   */
  val shape: RoundedCornerShape
    @Composable @ReadOnlyComposable get() = RoundedCornerShape(design.borderRadius)

  /**
   * Retrieves the current [ComputedColors] at the call site's position in the hierarchy.
   *
   * These are derived color variants calculated from the base Clerk colors, optimized for common UI
   * patterns like pressed states, focus indicators, borders, and semantic backgrounds. These colors
   * automatically adapt to light/dark themes and provide consistent visual feedback across
   * components.
   *
   * Examples include:
   * - `primaryPressed` - Primary color in pressed state
   * - `inputBorderFocused` - Border color for focused input fields
   * - `backgroundSuccess` - Background color for success states
   * - `dangerInputBorder` - Border color for error/danger states
   */
  val computed: ComputedColors
    @Composable @ReadOnlyComposable get() = LocalComputedColors.current
}

/**
 * Generates computed color variants from the base Clerk colors.
 *
 * This function creates derived colors that are commonly needed in UI components, such as pressed
 * states, various border opacities, and semantic color backgrounds. The computed colors
 * automatically adapt to the current theme (light/dark) and provide consistent visual patterns.
 *
 * @param colors The base ClerkColors to derive variants from.
 * @return A [ComputedColors] object containing all the derived color variants.
 */
@Composable
private fun generateComputedColors(colors: ClerkColors): ComputedColors {

  val computed =
    ComputedColors(
      primaryPressed =
        if (isSystemInDarkTheme())
          colors.primary?.lighten(PRIMARY_PRESSED_FACTOR) ?: Color.Transparent
        else colors.primary?.darken(PRIMARY_PRESSED_FACTOR) ?: Color.Transparent,
      border = colors.border?.copy(alpha = BORDER_ALPHA_SUBTLE) ?: Color.Transparent,
      buttonBorder = colors.border?.copy(alpha = BUTTON_BORDER_ALPHA) ?: Color.Transparent,
      inputBorder = colors.border?.copy(alpha = INPUT_BORDER_ALPHA) ?: Color.Transparent,
      inputBorderFocused =
        colors.ring?.copy(alpha = INPUT_BORDER_FOCUSED_ALPHA) ?: Color.Transparent,
      dangerInputBorder =
        colors.danger?.copy(alpha = DANGER_INPUT_BORDER_ALPHA) ?: Color.Transparent,
      dangerInputBorderFocused =
        colors.danger?.copy(alpha = DANGER_INPUT_BORDER_FOCUSED_ALPHA) ?: Color.Transparent,
      backgroundTransparent =
        colors.background?.copy(alpha = BACKGROUND_TRANSPARENT_ALPHA) ?: Color.Transparent,
      backgroundSuccess =
        colors.success?.copy(alpha = SUCCESS_BACKGROUND_ALPHA) ?: Color.Transparent,
      borderSuccess = colors.success?.copy(alpha = SUCCESS_BORDER_ALPHA) ?: Color.Transparent,
      backgroundDanger = colors.danger?.copy(alpha = DANGER_BACKGROUND_ALPHA) ?: Color.Transparent,
      borderDanger = colors.danger?.copy(alpha = DANGER_BORDER_ALPHA) ?: Color.Transparent,
      backgroundWarning =
        colors.warning?.copy(alpha = WARNING_BACKGROUND_ALPHA) ?: Color.Transparent,
      borderWarning = colors.warning?.copy(alpha = WARNING_BORDER_ALPHA) ?: Color.Transparent,
    )
  return computed
}

/**
 * Computes a Material3 ColorScheme from Clerk colors.
 *
 * This function maps Clerk's semantic color system to Material3's color roles, ensuring that
 * standard Material components work seamlessly within Clerk's themed environment while maintaining
 * visual consistency.
 *
 * @param colors The ClerkColors to map to Material3 color roles.
 * @return A [ColorScheme] configured for the current theme (light/dark).
 */
@Composable
private fun computeColorScheme(colors: ClerkColors): ColorScheme {

  return if (isSystemInDarkTheme()) {
    darkColorScheme(
      primary = colors.primary!!,
      background = colors.background!!,
      surface = colors.input!!,
      error = colors.danger!!,
      onPrimary = colors.primaryForeground!!,
      onBackground = colors.foreground!!,
      onSurface = colors.inputForeground!!,
      outline = colors.border!!,
      secondary = colors.muted!!,
      tertiary = colors.neutral!!,
      onSurfaceVariant = colors.mutedForeground!!,
    )
  } else {
    lightColorScheme(
      primary = colors.primary!!,
      background = colors.background!!,
      surface = colors.input!!,
      error = colors.danger!!,
      onPrimary = colors.primaryForeground!!,
      onBackground = colors.foreground!!,
      onSurface = colors.inputForeground!!,
      outline = colors.border!!,
      secondary = colors.muted!!,
      tertiary = colors.neutral!!,
      onSurfaceVariant = colors.mutedForeground!!,
    )
  }
}
