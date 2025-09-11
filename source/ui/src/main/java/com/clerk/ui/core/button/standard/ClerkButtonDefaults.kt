package com.clerk.ui.core.button.standard

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color

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
  fun icons(
    trailingIconColor: Color = Color.Unspecified,
    leadingIconColor: Color = Color.Unspecified,
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
