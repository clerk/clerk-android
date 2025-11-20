package com.clerk.api.ui

import androidx.compose.ui.text.font.FontWeight
import kotlin.test.assertEquals
import org.junit.Test

class ClerkTypographyTest {

  @Test
  fun `zero-arg constructor returns Clerk defaults`() {
    val typography = ClerkTypography()

    assertEquals(ClerkTypographyDefaults.displaySmall, typography.displaySmall)
    assertEquals(ClerkTypographyDefaults.headlineLarge, typography.headlineLarge)
    assertEquals(ClerkTypographyDefaults.bodySmall, typography.bodySmall)
  }

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
