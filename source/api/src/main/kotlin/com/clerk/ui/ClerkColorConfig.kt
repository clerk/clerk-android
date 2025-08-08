package com.clerk.ui

data class ClerkColorConfig(
  val primary: ThemeColor,
  val background: ThemeColor,
  val input: ThemeColor,
  val danger: ThemeColor,
  val success: ThemeColor,
  val warning: ThemeColor,
  val foreground: ThemeColor,
  val mutedForeground: ThemeColor,
  val primaryForeground: ThemeColor,
  val inputForeground: ThemeColor,
  val neutral: ThemeColor,
  val ring: ThemeColor,
  val muted: ThemeColor,
  val shadow: ThemeColor,
)

data class ThemeColor(val argb: Long) {
  constructor(argb: Int) : this(argb.toLong())

  constructor(
    alpha: Int = DEFAULT_ALPHA,
    red: Int,
    green: Int,
    blue: Int,
  ) : this(
    (alpha.toLong() shl ALPHA_SHIFT) or
      (red.toLong() shl RED_SHIFT) or
      (green.toLong() shl GREEN_SHIFT) or
      blue.toLong()
  )

  companion object {
    private const val DEFAULT_ALPHA = 0xFF
    private const val ALPHA_SHIFT = 24
    private const val RED_SHIFT = 16
    private const val GREEN_SHIFT = 8
  }
}
