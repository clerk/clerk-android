package com.clerk.ui.core.button

import androidx.annotation.DrawableRes

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

data class ClerkButtonIconConfig(
  @DrawableRes val leadingIcon: Int? = null,
  @DrawableRes val trailingIcon: Int? = null,
)
