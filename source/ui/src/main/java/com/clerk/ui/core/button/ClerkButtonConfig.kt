package com.clerk.ui.core.button

data class ClerkButtonConfig(val emphasis: Emphasis = Emphasis.High, val size: Size = Size.Large) {
  enum class Emphasis {
    None,
    Low,
    High,
  }

  enum class Size {
    Small,
    Large,
  }
}

enum class ButtonStyle {
  Primary,
  Secondary,
  Negative,
}
