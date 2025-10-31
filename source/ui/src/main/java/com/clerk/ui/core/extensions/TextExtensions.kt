package com.clerk.ui.core.extensions

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

/** Workaround since the emphasized text values don't seem to be publicly available. */
internal fun TextStyle.withMediumWeight(): TextStyle {
  return this.copy(fontWeight = FontWeight.Medium)
}
