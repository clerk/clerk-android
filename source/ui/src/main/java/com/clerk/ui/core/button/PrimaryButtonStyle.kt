package com.clerk.ui.core.button

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.clerk.api.ui.ClerkColors
import com.clerk.api.ui.ClerkDesign
import com.clerk.api.ui.ClerkTypography
import com.clerk.ui.colors.ComputedColors

internal interface ClerkButtonStyle {

  val config: ClerkButtonConfig
  val clerkColors: ClerkColors

  val clerkTypography: ClerkTypography

  val clerkDesign: ClerkDesign

  val computedColors: ComputedColors

  val isPressed: Boolean

  @Composable fun textStyle(): TextStyle

  @Composable @Stable fun foregroundColor(): Color

  @Composable @Stable fun backgroundColor(): Color

  @Composable @Stable fun height(): Dp

  @Composable @Stable fun borderWidth(): Dp

  @Composable @Stable fun borderColor(): Color

  @Composable @Stable fun shape(): Shape

  @Stable val hasShadow: Boolean

  @Composable fun tokens(): ButtonStyleTokens
}

@Stable
class PrimaryButtonStyle(
  override val config: ClerkButtonConfig,
  override val clerkColors: ClerkColors,
  override val clerkTypography: ClerkTypography,
  override val clerkDesign: ClerkDesign,
  override val computedColors: ComputedColors,
  override val isPressed: Boolean = false,
) : ClerkButtonStyle {

  @Stable @Composable override fun textStyle(): TextStyle = tokens().textStyle

  @Stable @Composable override fun foregroundColor(): Color = tokens().foreground

  @Stable @Composable override fun backgroundColor(): Color = tokens().backgroundColor

  @Stable @Composable override fun height(): Dp = tokens().height

  @Stable @Composable override fun borderWidth(): Dp = tokens().borderWidth

  @Stable @Composable override fun borderColor(): Color = tokens().borderColor

  @Stable
  @Composable
  override fun shape(): Shape =
    RoundedCornerShape(tokens().cornerRadius) // Use theme.design.borderRadius equivalent

  @Stable
  override val hasShadow: Boolean
    get() =
      when (config.emphasis) {
        ClerkButtonConfig.Emphasis.None -> false
        ClerkButtonConfig.Emphasis.Low -> true
        ClerkButtonConfig.Emphasis.High -> true
      }

  // Aggregated, memoized tokens API for ergonomic usage and stability
  @Composable
  override fun tokens(): ButtonStyleTokens {
    val text =
      when (config.size) {
        ClerkButtonConfig.Size.Small -> clerkTypography.titleSmall!!

        ClerkButtonConfig.Size.Large -> clerkTypography.bodyLarge!!
      }
    val foreground =
      when (config.emphasis) {
        ClerkButtonConfig.Emphasis.None,
        ClerkButtonConfig.Emphasis.Low -> clerkColors.primary!!

        ClerkButtonConfig.Emphasis.High -> clerkColors.primaryForeground!!
      }

    val height =
      when (config.size) {
        ClerkButtonConfig.Size.Small -> 32.dp
        ClerkButtonConfig.Size.Large -> 48.dp
      }
    val backgroundColor =
      when (config.emphasis) {
        ClerkButtonConfig.Emphasis.None,
        ClerkButtonConfig.Emphasis.Low ->
          if (isPressed) clerkColors.muted!! else clerkColors.background!!
        ClerkButtonConfig.Emphasis.High ->
          if (isPressed) computedColors.primaryPressed else clerkColors.primary!!
      }
    val borderWidth =
      when (config.emphasis) {
        ClerkButtonConfig.Emphasis.None -> 0.dp
        ClerkButtonConfig.Emphasis.Low,
        ClerkButtonConfig.Emphasis.High -> 1.dp
      }

    val borderColor =
      when (config.emphasis) {
        ClerkButtonConfig.Emphasis.None -> Color.Transparent
        ClerkButtonConfig.Emphasis.Low,
        ClerkButtonConfig.Emphasis.High -> computedColors.buttonBorder
      }

    return remember(
      config.size,
      config.emphasis,
      clerkColors,
      clerkDesign,
      clerkTypography,
      isPressed,
    ) {
      ButtonStyleTokens(
        textStyle = text,
        foreground = foreground,
        height = height,
        backgroundColor = backgroundColor,
        borderWidth = borderWidth,
        borderColor = borderColor,
        cornerRadius = 8.dp,
      )
    }
  }
}

@Immutable
data class ButtonStyleTokens(
  val textStyle: TextStyle,
  val foreground: Color,
  val height: Dp,
  val backgroundColor: Color,
  val borderWidth: Dp,
  val borderColor: Color,
  val cornerRadius: Dp,
)
