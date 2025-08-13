package com.clerk.ui.core.button

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.clerk.api.ui.ClerkColors
import com.clerk.api.ui.ClerkDesign
import com.clerk.api.ui.ClerkTypography
import com.clerk.ui.colors.ComputedColors

@Immutable
data class ButtonStyleTokens(
  val textStyle: TextStyle,
  val foreground: Color,
  val height: Dp,
  val backgroundColor: Color,
  val borderWidth: Dp,
  val borderColor: Color,
  val cornerRadius: Dp,
  val hasShadow: Boolean,
)

@Composable
fun buildButtonTokens(
  style: ButtonStyle,
  config: ClerkButtonConfig,
  colors: ClerkColors,
  computed: ComputedColors,
  typography: ClerkTypography,
  design: ClerkDesign,
  isPressed: Boolean,
): ButtonStyleTokens {
  val text =
    when (config.size) {
      ClerkButtonConfig.Size.Small -> typography.titleSmall!!
      ClerkButtonConfig.Size.Large -> typography.bodyLarge!!
    }

  val height =
    when (config.size) {
      ClerkButtonConfig.Size.Small -> 32.dp
      ClerkButtonConfig.Size.Large -> 48.dp
    }

  val hasShadow =
    when (config.emphasis) {
      ClerkButtonConfig.Emphasis.None -> false
      ClerkButtonConfig.Emphasis.Low,
      ClerkButtonConfig.Emphasis.High -> true
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
      ClerkButtonConfig.Emphasis.High -> computed.buttonBorder
    }

  val foreground = generateForeground(style, config, colors, isPressed)
  val background = generateBackground(style, config, colors, isPressed, computed)

  return ButtonStyleTokens(
    textStyle = text,
    foreground = foreground,
    height = height,
    backgroundColor = background,
    borderWidth = borderWidth,
    borderColor = borderColor,
    cornerRadius = design.borderRadius,
    hasShadow = hasShadow,
  )
}

private fun generateForeground(
  style: ButtonStyle,
  config: ClerkButtonConfig,
  colors: ClerkColors,
  isPressed: Boolean,
): Color =
  when (style) {
    ButtonStyle.Primary ->
      when (config.emphasis) {
        ClerkButtonConfig.Emphasis.High -> colors.primaryForeground!!
        ClerkButtonConfig.Emphasis.Low,
        ClerkButtonConfig.Emphasis.None -> colors.primary!!
      }

    ButtonStyle.Secondary ->
      when (config.emphasis) {
        ClerkButtonConfig.Emphasis.None ->
          if (isPressed) colors.foreground!! else colors.mutedForeground!!
        ClerkButtonConfig.Emphasis.Low,
        ClerkButtonConfig.Emphasis.High -> colors.foreground!!
      }

    ButtonStyle.Negative ->
      when (config.emphasis) {
        ClerkButtonConfig.Emphasis.High -> colors.primaryForeground!!
        ClerkButtonConfig.Emphasis.Low,
        ClerkButtonConfig.Emphasis.None -> colors.danger!!
      }
  }

private fun generateBackground(
  style: ButtonStyle,
  config: ClerkButtonConfig,
  colors: ClerkColors,
  isPressed: Boolean,
  computed: ComputedColors,
): Color =
  when (style) {
    ButtonStyle.Primary ->
      when (config.emphasis) {
        ClerkButtonConfig.Emphasis.High ->
          if (isPressed) computed.primaryPressed else colors.primary!!
        ClerkButtonConfig.Emphasis.Low,
        ClerkButtonConfig.Emphasis.None -> if (isPressed) colors.muted!! else colors.background!!
      }

    ButtonStyle.Secondary ->
      when (config.emphasis) {
        ClerkButtonConfig.Emphasis.None,
        ClerkButtonConfig.Emphasis.Low,
        ClerkButtonConfig.Emphasis.High -> if (isPressed) colors.muted!! else colors.background!!
      }

    ButtonStyle.Negative ->
      when (config.emphasis) {
        ClerkButtonConfig.Emphasis.High ->
          if (isPressed) computed.backgroundDanger else colors.danger!!
        ClerkButtonConfig.Emphasis.Low,
        ClerkButtonConfig.Emphasis.None ->
          if (isPressed) computed.backgroundDanger else colors.background!!
      }
  }
