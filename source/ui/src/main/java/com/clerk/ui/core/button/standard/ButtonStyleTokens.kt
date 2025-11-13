package com.clerk.ui.core.button.standard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.clerk.ui.core.dimens.dp0
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.dimens.dp32
import com.clerk.ui.core.dimens.dp48
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.MergedElementTheme

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
  config: ClerkButtonConfiguration,
  isPressed: Boolean,
  theme: MergedElementTheme,
): ButtonStyleTokens {
  val text =
    when (config.size) {
      ClerkButtonConfiguration.Size.Small -> theme.typography.titleSmall
      ClerkButtonConfiguration.Size.Large -> theme.typography.titleMedium
    }

  val height =
    when (config.size) {
      ClerkButtonConfiguration.Size.Small -> dp32
      ClerkButtonConfiguration.Size.Large -> dp48
    }

  val hasShadow =
    when (config.emphasis) {
      ClerkButtonConfiguration.Emphasis.None -> false
      ClerkButtonConfiguration.Emphasis.Low -> !isPressed
      ClerkButtonConfiguration.Emphasis.High -> !isPressed
    }

  val borderWidth =
    when (config.emphasis) {
      ClerkButtonConfiguration.Emphasis.None -> dp0
      ClerkButtonConfiguration.Emphasis.Low,
      ClerkButtonConfiguration.Emphasis.High -> dp1
    }

  val borderColor =
    when (config.emphasis) {
      ClerkButtonConfiguration.Emphasis.None -> Color.Transparent
      ClerkButtonConfiguration.Emphasis.Low,
      ClerkButtonConfiguration.Emphasis.High -> theme.computedColors.buttonBorder
    }

  val foreground = generateForeground(config, isPressed, theme)
  val background = config.backgroundColorOverride ?: generateBackground(config, isPressed, theme)

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
private fun generateForeground(config: ClerkButtonConfiguration, isPressed: Boolean, theme: MergedElementTheme): Color =
  when (config.style) {
    ClerkButtonConfiguration.ButtonStyle.Primary ->
      when (config.emphasis) {
        ClerkButtonConfiguration.Emphasis.High -> theme.colors.primaryForeground
        ClerkButtonConfiguration.Emphasis.Low,
        ClerkButtonConfiguration.Emphasis.None -> theme.colors.primary
      }

    ClerkButtonConfiguration.ButtonStyle.Secondary ->
      when (config.emphasis) {
        ClerkButtonConfiguration.Emphasis.None ->
          if (isPressed) theme.colors.foreground
          else theme.colors.mutedForeground
        ClerkButtonConfiguration.Emphasis.Low,
        ClerkButtonConfiguration.Emphasis.High -> theme.colors.foreground
      }

    ClerkButtonConfiguration.ButtonStyle.Negative ->
      when (config.emphasis) {
        ClerkButtonConfiguration.Emphasis.High -> theme.colors.primaryForeground
        ClerkButtonConfiguration.Emphasis.Low,
        ClerkButtonConfiguration.Emphasis.None -> theme.colors.danger
      }
  }

@Composable
private fun generateBackground(config: ClerkButtonConfiguration, isPressed: Boolean, theme: MergedElementTheme): Color =
  when (config.style) {
    ClerkButtonConfiguration.ButtonStyle.Primary ->
      when (config.emphasis) {
        ClerkButtonConfiguration.Emphasis.High ->
          if (isPressed) theme.computedColors.primaryPressed
          else theme.colors.primary
        ClerkButtonConfiguration.Emphasis.Low,
        ClerkButtonConfiguration.Emphasis.None ->
          if (isPressed) theme.colors.muted else theme.colors.background
      }

    ClerkButtonConfiguration.ButtonStyle.Secondary ->
      when (config.emphasis) {
        ClerkButtonConfiguration.Emphasis.None,
        ClerkButtonConfiguration.Emphasis.Low,
        ClerkButtonConfiguration.Emphasis.High ->
          if (isPressed) theme.colors.muted else theme.colors.background
      }

    ClerkButtonConfiguration.ButtonStyle.Negative ->
      when (config.emphasis) {
        ClerkButtonConfiguration.Emphasis.High ->
          if (isPressed) theme.computedColors.backgroundDanger
          else theme.colors.danger
        ClerkButtonConfiguration.Emphasis.Low,
        ClerkButtonConfiguration.Emphasis.None ->
          if (isPressed) theme.computedColors.backgroundDanger
          else theme.colors.background
      }
  }
