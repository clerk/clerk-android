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
): ButtonStyleTokens {
  val text =
    when (config.size) {
      ClerkButtonConfiguration.Size.Small -> ClerkMaterialTheme.typography.titleSmall
      ClerkButtonConfiguration.Size.Large -> ClerkMaterialTheme.typography.titleMedium
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
      ClerkButtonConfiguration.Emphasis.High -> ClerkMaterialTheme.computedColors.buttonBorder
    }

  val foreground = generateForeground(config, isPressed)
  val background = generateBackground(config, isPressed)

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
private fun generateForeground(config: ClerkButtonConfiguration, isPressed: Boolean): Color =
  when (config.style) {
    ClerkButtonConfiguration.ButtonStyle.Primary ->
      when (config.emphasis) {
        ClerkButtonConfiguration.Emphasis.High -> ClerkMaterialTheme.colors.primaryForeground
        ClerkButtonConfiguration.Emphasis.Low,
        ClerkButtonConfiguration.Emphasis.None -> ClerkMaterialTheme.colors.primary
      }

    ClerkButtonConfiguration.ButtonStyle.Secondary ->
      when (config.emphasis) {
        ClerkButtonConfiguration.Emphasis.None ->
          if (isPressed) ClerkMaterialTheme.colors.foreground
          else ClerkMaterialTheme.colors.mutedForeground
        ClerkButtonConfiguration.Emphasis.Low,
        ClerkButtonConfiguration.Emphasis.High -> ClerkMaterialTheme.colors.foreground
      }

    ClerkButtonConfiguration.ButtonStyle.Negative ->
      when (config.emphasis) {
        ClerkButtonConfiguration.Emphasis.High -> ClerkMaterialTheme.colors.primaryForeground
        ClerkButtonConfiguration.Emphasis.Low,
        ClerkButtonConfiguration.Emphasis.None -> ClerkMaterialTheme.colors.danger
      }
  }

@Composable
private fun generateBackground(config: ClerkButtonConfiguration, isPressed: Boolean): Color =
  when (config.style) {
    ClerkButtonConfiguration.ButtonStyle.Primary ->
      when (config.emphasis) {
        ClerkButtonConfiguration.Emphasis.High ->
          if (isPressed) ClerkMaterialTheme.computedColors.primaryPressed
          else ClerkMaterialTheme.colors.primary
        ClerkButtonConfiguration.Emphasis.Low,
        ClerkButtonConfiguration.Emphasis.None ->
          if (isPressed) ClerkMaterialTheme.colors.muted else ClerkMaterialTheme.colors.background
      }

    ClerkButtonConfiguration.ButtonStyle.Secondary ->
      when (config.emphasis) {
        ClerkButtonConfiguration.Emphasis.None,
        ClerkButtonConfiguration.Emphasis.Low,
        ClerkButtonConfiguration.Emphasis.High ->
          if (isPressed) ClerkMaterialTheme.colors.muted else ClerkMaterialTheme.colors.background
      }

    ClerkButtonConfiguration.ButtonStyle.Negative ->
      when (config.emphasis) {
        ClerkButtonConfiguration.Emphasis.High ->
          if (isPressed) ClerkMaterialTheme.computedColors.backgroundDanger
          else ClerkMaterialTheme.colors.danger
        ClerkButtonConfiguration.Emphasis.Low,
        ClerkButtonConfiguration.Emphasis.None ->
          if (isPressed) ClerkMaterialTheme.computedColors.backgroundDanger
          else ClerkMaterialTheme.colors.background
      }
  }
