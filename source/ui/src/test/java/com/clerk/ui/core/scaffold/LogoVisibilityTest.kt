package com.clerk.ui.core.scaffold

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogoVisibilityTest {

  @Test
  fun shouldShowInstanceLogoReturnsFalseWhenHasLogoIsFalse() {
    assertFalse(
      shouldShowInstanceLogo(hasLogo = false, organizationLogoUrl = "https://example.com/logo.png")
    )
  }

  @Test
  fun shouldShowInstanceLogoReturnsFalseWhenLogoUrlIsNull() {
    assertFalse(shouldShowInstanceLogo(hasLogo = true, organizationLogoUrl = null))
  }

  @Test
  fun shouldShowInstanceLogoReturnsFalseWhenLogoUrlIsBlank() {
    assertFalse(shouldShowInstanceLogo(hasLogo = true, organizationLogoUrl = "   "))
  }

  @Test
  fun shouldShowInstanceLogoReturnsTrueWhenHasLogoAndUrlArePresent() {
    assertTrue(
      shouldShowInstanceLogo(hasLogo = true, organizationLogoUrl = "https://example.com/logo.png")
    )
  }
}
