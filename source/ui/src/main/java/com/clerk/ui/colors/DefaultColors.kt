package com.clerk.ui.colors

import androidx.compose.ui.graphics.Color
import com.clerk.api.ui.ClerkColors

object DefaultColors {

  val clerk =
    ClerkColors(
      primary = Color(color = 0xFF6C47FF),
      background = Color(0xFFFFFFFF),
      input = Color.White,
      danger = Color(0xFFDC2626),
      success = Color(0xFF22C543),
      warning = Color(0xFFF36B16),
      foreground = Color(0xFF212126),
      mutedForeground = Color(0xFF747686),
      inputForeground = Color(0xFF2B2B34),
      neutral = Color(0xFFF9F9F9),
      border = Color(0xFF2B2B34),
      shadow = Color(0xFF2B2B34),
      ring = Color(0xFF6C47FF),
      muted = Color(0xFFF9F9F9),
    )
}
