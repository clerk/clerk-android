package com.clerk.ui.core

import androidx.annotation.DrawableRes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class ButtonComponentConfiguration(
  val emphasis: ButtonEmphasis = ButtonEmphasis.High,
  val style: ButtonStyle = ButtonStyle.Primary,
  val size: ButtonSize = ButtonSize.Large,
  val state: ButtonState = ButtonState.Default,
  @DrawableRes val iconStart: Int? = null,
  @DrawableRes val iconEnd: Int? = null,
)

enum class ButtonEmphasis {
  High,
  None,
  Low,
}

enum class ButtonStyle {
  Primary,
  Secondary,
  Negative,
}

enum class ButtonSize(val size: Dp) {
  Small(32.dp),
  Large(48.dp),
}

enum class ButtonState {
  Default,
  Pressed,
  Disabled,
}
