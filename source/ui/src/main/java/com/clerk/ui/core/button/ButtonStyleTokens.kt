package com.clerk.ui.core.button

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.clerk.api.ui.ClerkDesign
import com.clerk.ui.colors.ComputedColors
import com.clerk.ui.core.dimens.dp0
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.dimens.dp32
import com.clerk.ui.core.dimens.dp48

@Immutable
internal data class ButtonStyleTokens(
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
internal fun buildButtonTokens(
  style: ButtonStyle,
  config: ClerkButtonConfig,
  computed: ComputedColors,
  design: ClerkDesign,
  isPressed: Boolean,
): ButtonStyleTokens {
  val text =
    when (config.size) {
      ClerkButtonConfig.Size.Small -> MaterialTheme.typography.titleSmall
      ClerkButtonConfig.Size.Large -> MaterialTheme.typography.titleMedium
    }

  val height =
    when (config.size) {
      ClerkButtonConfig.Size.Small -> dp32
      ClerkButtonConfig.Size.Large -> dp48
    }

  val hasShadow =
    when (config.emphasis) {
      ClerkButtonConfig.Emphasis.None -> false
      ClerkButtonConfig.Emphasis.Low,
      ClerkButtonConfig.Emphasis.High -> true
    }

  val borderWidth =
    when (config.emphasis) {
      ClerkButtonConfig.Emphasis.None -> dp0
      ClerkButtonConfig.Emphasis.Low,
      ClerkButtonConfig.Emphasis.High -> dp1
    }

  val borderColor =
    when (config.emphasis) {
      ClerkButtonConfig.Emphasis.None -> Color.Transparent
      ClerkButtonConfig.Emphasis.Low,
      ClerkButtonConfig.Emphasis.High -> computed.buttonBorder
    }

  val foreground = generateForeground(style, config, isPressed)
  val background = generateBackground(style, config, isPressed, computed)

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

@Composable
private fun generateForeground(
  style: ButtonStyle,
  config: ClerkButtonConfig,
  isPressed: Boolean,
): Color =
  when (style) {
    ButtonStyle.Primary ->
      when (config.emphasis) {
        ClerkButtonConfig.Emphasis.High -> MaterialTheme.colorScheme.onPrimary
        ClerkButtonConfig.Emphasis.Low,
        ClerkButtonConfig.Emphasis.None -> MaterialTheme.colorScheme.primary
      }

    ButtonStyle.Secondary ->
      when (config.emphasis) {
        ClerkButtonConfig.Emphasis.None ->
          if (isPressed) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.onSurfaceVariant
        ClerkButtonConfig.Emphasis.Low,
        ClerkButtonConfig.Emphasis.High -> MaterialTheme.colorScheme.onBackground
      }

    ButtonStyle.Negative ->
      when (config.emphasis) {
        ClerkButtonConfig.Emphasis.High -> MaterialTheme.colorScheme.onPrimary
        ClerkButtonConfig.Emphasis.Low,
        ClerkButtonConfig.Emphasis.None -> MaterialTheme.colorScheme.error
      }
  }

@Composable
private fun generateBackground(
  style: ButtonStyle,
  config: ClerkButtonConfig,
  isPressed: Boolean,
  computed: ComputedColors,
): Color =
  when (style) {
    ButtonStyle.Primary ->
      when (config.emphasis) {
        ClerkButtonConfig.Emphasis.High ->
          if (isPressed) computed.primaryPressed else MaterialTheme.colorScheme.primary
        ClerkButtonConfig.Emphasis.Low,
        ClerkButtonConfig.Emphasis.None ->
          if (isPressed) MaterialTheme.colorScheme.secondary
          else MaterialTheme.colorScheme.background
      }

    ButtonStyle.Secondary ->
      when (config.emphasis) {
        ClerkButtonConfig.Emphasis.None,
        ClerkButtonConfig.Emphasis.Low,
        ClerkButtonConfig.Emphasis.High ->
          if (isPressed) MaterialTheme.colorScheme.secondary
          else MaterialTheme.colorScheme.background
      }

    ButtonStyle.Negative ->
      when (config.emphasis) {
        ClerkButtonConfig.Emphasis.High ->
          if (isPressed) computed.backgroundDanger else MaterialTheme.colorScheme.error
        ClerkButtonConfig.Emphasis.Low,
        ClerkButtonConfig.Emphasis.None ->
          if (isPressed) computed.backgroundDanger else MaterialTheme.colorScheme.background
      }
  }
