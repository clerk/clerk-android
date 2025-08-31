package com.clerk.ui.core.button.standard

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.clerk.ui.colors.ComputedColors
import com.clerk.ui.core.dimens.dp0
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
  val hasShadow: Boolean,
)

@Composable
internal fun buildButtonTokens(
  config: ClerkButtonConfig,
  computed: ComputedColors,
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
      ClerkButtonConfig.Emphasis.High ->
        !(isPressed && config.style == ClerkButtonConfig.ButtonStyle.Negative)
    }

  val borderWidth =
    when (config.emphasis) {
      ClerkButtonConfig.Emphasis.None -> dp0
      ClerkButtonConfig.Emphasis.Low,
      ClerkButtonConfig.Emphasis.High -> dp0
    }

  val borderColor =
    when (config.emphasis) {
      ClerkButtonConfig.Emphasis.None -> Color.Transparent
      ClerkButtonConfig.Emphasis.Low,
      ClerkButtonConfig.Emphasis.High -> computed.buttonBorder
    }

  val foreground = generateForeground(config, isPressed)
  val background = generateBackground(config, isPressed, computed)

  return ButtonStyleTokens(
    textStyle = text,
    foreground = foreground,
    height = height,
    backgroundColor = background,
    borderWidth = borderWidth,
    borderColor = borderColor,
    hasShadow = hasShadow,
  )
}

@Composable
private fun generateForeground(config: ClerkButtonConfig, isPressed: Boolean): Color =
  when (config.style) {
    ClerkButtonConfig.ButtonStyle.Primary ->
      when (config.emphasis) {
        ClerkButtonConfig.Emphasis.High -> MaterialTheme.colorScheme.onPrimary
        ClerkButtonConfig.Emphasis.Low,
        ClerkButtonConfig.Emphasis.None -> MaterialTheme.colorScheme.primary
      }

    ClerkButtonConfig.ButtonStyle.Secondary ->
      when (config.emphasis) {
        ClerkButtonConfig.Emphasis.None ->
          if (isPressed) MaterialTheme.colorScheme.onBackground
          else MaterialTheme.colorScheme.onSurfaceVariant
        ClerkButtonConfig.Emphasis.Low,
        ClerkButtonConfig.Emphasis.High -> MaterialTheme.colorScheme.onBackground
      }

    ClerkButtonConfig.ButtonStyle.Negative ->
      when (config.emphasis) {
        ClerkButtonConfig.Emphasis.High -> MaterialTheme.colorScheme.onPrimary
        ClerkButtonConfig.Emphasis.Low,
        ClerkButtonConfig.Emphasis.None -> MaterialTheme.colorScheme.error
      }
  }

@Composable
private fun generateBackground(
  config: ClerkButtonConfig,
  isPressed: Boolean,
  computed: ComputedColors,
): Color =
  when (config.style) {
    ClerkButtonConfig.ButtonStyle.Primary ->
      when (config.emphasis) {
        ClerkButtonConfig.Emphasis.High ->
          if (isPressed) computed.primaryPressed else MaterialTheme.colorScheme.primary
        ClerkButtonConfig.Emphasis.Low,
        ClerkButtonConfig.Emphasis.None ->
          if (isPressed) MaterialTheme.colorScheme.secondary
          else MaterialTheme.colorScheme.background
      }

    ClerkButtonConfig.ButtonStyle.Secondary ->
      when (config.emphasis) {
        ClerkButtonConfig.Emphasis.None,
        ClerkButtonConfig.Emphasis.Low,
        ClerkButtonConfig.Emphasis.High ->
          if (isPressed) MaterialTheme.colorScheme.secondary
          else MaterialTheme.colorScheme.background
      }

    ClerkButtonConfig.ButtonStyle.Negative ->
      when (config.emphasis) {
        ClerkButtonConfig.Emphasis.High ->
          if (isPressed) computed.backgroundDanger else MaterialTheme.colorScheme.error
        ClerkButtonConfig.Emphasis.Low,
        ClerkButtonConfig.Emphasis.None ->
          if (isPressed) computed.backgroundDanger else MaterialTheme.colorScheme.background
      }
  }
