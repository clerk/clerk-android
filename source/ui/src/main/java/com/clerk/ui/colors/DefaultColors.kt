package com.clerk.ui.colors

import androidx.compose.ui.graphics.Color
import com.clerk.api.ui.ClerkColors

object DefaultColors {

  val clerk =
    ClerkColors(
      primary = Color(color = 0xFF6C47FF),
      background = Color(0xFFFFFFFF),
      input = Color(0xFFDC2626),
      danger = Color(0xFF22C543),
      success = Color(0xFFF36B16),
      warning = Color(0xFF212126),
      foreground = Color(0xFF747686),
      mutedForeground = Color(0xFF2B2B34),
      primaryForeground = Color(0xFFF9F9F9),
    )
}
