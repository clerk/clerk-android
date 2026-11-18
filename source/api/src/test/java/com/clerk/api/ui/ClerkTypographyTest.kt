package com.clerk.api.ui

import androidx.compose.ui.text.font.FontWeight
import kotlin.test.assertEquals
import org.junit.Test

class ClerkTypographyTest {

  @Test
  fun `named overrides preserve other defaults`() {
    val typography =
      ClerkTypography(
        displaySmall = ClerkTypographyDefaults.displaySmall.copy(fontWeight = FontWeight.SemiBold)
      )

    assertEquals(FontWeight.SemiBold, typography.displaySmall?.fontWeight)
    assertEquals(ClerkTypographyDefaults.headlineMedium, typography.headlineMedium)
    assertEquals(ClerkTypographyDefaults.labelSmall, typography.labelSmall)
  }
}
