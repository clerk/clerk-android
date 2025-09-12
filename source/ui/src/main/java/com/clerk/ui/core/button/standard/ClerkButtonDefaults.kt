package com.clerk.ui.core.button.standard

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.clerk.ui.core.common.dimens.dp12
import com.clerk.ui.core.common.dimens.dp4
import com.clerk.ui.theme.ClerkMaterialTheme

/** Contains default values for Clerk buttons. */
object ClerkButtonDefaults {

  /**
   * Creates a [ClerkButtonIcons] configuration for a button.
   *
   * @param trailingIconColor The color of the trailing icon.
   * @param leadingIconColor The color of the leading icon.
   * @param trailingIcon The drawable resource for the trailing icon.
   * @param leadingIcon The drawable resource for the leading icon.
   * @return A [ClerkButtonIcons] instance.
   */
  @Composable
  fun icons(
    trailingIconColor: Color = ClerkMaterialTheme.colors.primary,
    leadingIconColor: Color = ClerkMaterialTheme.colors.primary,
    @DrawableRes trailingIcon: Int? = null,
    @DrawableRes leadingIcon: Int? = null,
  ) =
    ClerkButtonIcons(
      trailingIconColor = trailingIconColor,
      leadingIconColor = leadingIconColor,
      trailingIcon = trailingIcon,
      leadingIcon = leadingIcon,
    )

  fun configuration(
    style: ClerkButtonConfig.ButtonStyle = ClerkButtonConfig.ButtonStyle.Primary,
    emphasis: ClerkButtonConfig.Emphasis = ClerkButtonConfig.Emphasis.High,
    size: ClerkButtonConfig.Size = ClerkButtonConfig.Size.Large,
  ) = ClerkButtonConfig(style = style, emphasis = emphasis, size = size)

  fun padding(horizontal: Dp = dp12, vertical: Dp = dp4) = ClerkButtonPadding(horizontal, vertical)
}

/**
 * Represents the icon configuration for a Clerk button.
 *
 * @property trailingIcon The drawable resource for the trailing icon.
 * @property leadingIcon The drawable resource for the leading icon.
 * @property trailingIconColor The color of the trailing icon.
 * @property leadingIconColor The color of the leading icon.
 */
data class ClerkButtonIcons(
  @DrawableRes val trailingIcon: Int?,
  @DrawableRes val leadingIcon: Int?,
  val trailingIconColor: Color,
  val leadingIconColor: Color,
)

data class ClerkButtonConfig(
  val style: ButtonStyle = ButtonStyle.Primary,
  val emphasis: Emphasis = Emphasis.High,
  val size: Size = Size.Large,
) {
  enum class Emphasis {
    None,
    Low,
    High,
  }

  enum class Size {
    Small,
    Large,
  }

  enum class ButtonStyle {
    Primary,
    Secondary,
    Negative,
  }
}

data class ClerkButtonPadding(val horizontal: Dp, val vertical: Dp)
