package com.clerk.ui

class ClerkFontsConfig(
  val largeTitle: Font,
  val title: Font,
  val title2: Font,
  val title3: Font,
  val headline: Font,
  val body: Font,
  val callout: Font,
  val subhead: Font,
  val footnote: Font,
  val caption: Font,
  val caption2: Font,
)

data class Font(val name: String, val size: Double)
