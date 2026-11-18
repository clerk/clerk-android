package com.clerk.api.ui

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.test.assertEquals
import org.junit.Test

class ClerkTypographyDefaultsTest {

  @Test
  fun `default helper matches zero-arg constructor`() {
    assertEquals(ClerkTypography(), ClerkTypographyDefaults.default())
  }

  @Test
  fun `builder allows overriding individual slots`() {
    val typography = ClerkTypographyDefaults.typography {
      titleMedium = titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }

    assertEquals(18.sp, typography.titleMedium?.fontSize)
    assertEquals(FontWeight.Bold, typography.titleMedium?.fontWeight)
    assertEquals(ClerkTypographyDefaults.displaySmall, typography.displaySmall)
    assertEquals(ClerkTypographyDefaults.bodySmall, typography.bodySmall)
  }
}
