package com.clerk.ui.core.button.standard

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.core.dimens.dp4

/** Contains default values for Clerk buttons. */
object ClerkButtonDefaults {

  /**
   * Creates a [ClerkButtonIcons] configuration for a button.
   *
   * @param trailingIconColor The color of the trailing icon. If null, uses the current theme's
   *   primary color.
   * @param leadingIconColor The color of the leading icon. If null, uses the current theme's
   *   primary color.
   * @param trailingIcon The drawable resource for the trailing icon.
   * @param leadingIcon The drawable resource for the leading icon.
   * @return A [ClerkButtonIcons] instance.
   */
  @Composable
  fun icons(
    trailingIconColor: Color? = null,
    leadingIconColor: Color? = null,
    @DrawableRes trailingIcon: Int? = null,
    @DrawableRes leadingIcon: Int? = null,
  ) =
    ClerkButtonIcons(
      trailingIconColor = trailingIconColor,
      leadingIconColor = leadingIconColor,
      trailingIcon = trailingIcon,
      leadingIcon = leadingIcon,
    )

  /**
   * Creates a [ClerkButtonConfiguration] with the specified style, emphasis, and size.
   *
   * @param style The visual style of the button (e.g., Primary, Secondary).
   * @param emphasis The prominence of the button (e.g., High, Low).
   * @param size The size of the button (e.g., Large, Small).
   * @param backgroundColorOverride an optional background color override
   * @return A [ClerkButtonConfiguration] instance.
   */
  fun configuration(
    style: ClerkButtonConfiguration.ButtonStyle = ClerkButtonConfiguration.ButtonStyle.Primary,
    emphasis: ClerkButtonConfiguration.Emphasis = ClerkButtonConfiguration.Emphasis.High,
    size: ClerkButtonConfiguration.Size = ClerkButtonConfiguration.Size.Large,
    backgroundColorOverride: Color? = null,
  ) =
    ClerkButtonConfiguration(
      style = style,
      emphasis = emphasis,
      size = size,
      backgroundColorOverride = backgroundColorOverride,
    )

  /**
   * Creates a [ClerkButtonPadding] configuration for a button.
   *
   * @param horizontal The horizontal padding.
   * @param vertical The vertical padding.
   * @return A [ClerkButtonPadding] instance.
   */
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
  @field:DrawableRes val trailingIcon: Int?,
  @field:DrawableRes val leadingIcon: Int?,
  val trailingIconColor: Color?,
  val leadingIconColor: Color?,
)

/**
 * Data class holding the configuration for a [ClerkButton].
 *
 * @param style The visual style of the button.
 * @param emphasis The prominence of the button.
 * @param size The size of the button.
 * @param backgroundColorOverride an optional background color override
 */
data class ClerkButtonConfiguration(
  val style: ButtonStyle = ButtonStyle.Primary,
  val emphasis: Emphasis = Emphasis.High,
  val size: Size = Size.Large,
  val backgroundColorOverride: Color? = null,
) {
  /** Defines the visual prominence of the button. */
  enum class Emphasis {
    None,
    Low,
    High,
  }

  /** Defines the size of the button, affecting its height and text style. */
  enum class Size {
    Small,
    Large,
  }

  /** Defines the color palette and overall style of the button. */
  enum class ButtonStyle {
    Primary,
    Secondary,
    Negative,
  }
}

/**
 * Represents the padding configuration for a Clerk button.
 *
 * @property horizontal The horizontal padding.
 * @property vertical The vertical padding.
 */
data class ClerkButtonPadding(val horizontal: Dp, val vertical: Dp)
