package com.clerk.api.ui

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.test.assertEquals
import org.junit.Test

class ClerkTypographyDefaultsTest {

  @Test
  fun `default returns Clerk typography defaults`() {
    val typography = ClerkTypographyDefaults.default()

    assertEquals(ClerkTypographyDefaults.displaySmall, typography.displaySmall)
    assertEquals(ClerkTypographyDefaults.headlineLarge, typography.headlineLarge)
    assertEquals(ClerkTypographyDefaults.bodyMedium, typography.bodyMedium)
    assertEquals(ClerkTypographyDefaults.labelSmall, typography.labelSmall)
  }

  @Test
  fun `default helper matches zero-arg constructor`() {
    assertEquals(ClerkTypography(), ClerkTypographyDefaults.default())
  }

  @Test
  fun `builder allows overriding individual slots`() {
    val typography =
      ClerkTypographyDefaults.typography {
        titleMedium = titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold)
      }

    assertEquals(18.sp, typography.titleMedium?.fontSize)
    assertEquals(FontWeight.Bold, typography.titleMedium?.fontWeight)
    // Ensure untouched styles remain at defaults.
    assertEquals(ClerkTypographyDefaults.displaySmall, typography.displaySmall)
    assertEquals(ClerkTypographyDefaults.bodySmall, typography.bodySmall)
  }
}
