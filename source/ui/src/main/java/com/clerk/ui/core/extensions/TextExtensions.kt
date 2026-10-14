package com.clerk.ui.core.extensions

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

internal fun TextStyle.withMediumWeight(): TextStyle {
  return this.copy(fontWeight = FontWeight.Medium)
}

internal fun TextStyle.withSemiBoldWeight(): TextStyle {
  return this.copy(fontWeight = FontWeight.SemiBold)
}
