package com.clerk.ui.core.extensions

import kotlin.test.Test
import kotlin.test.assertEquals

class LogoUrlExtensionsTest {

  @Test
  fun usesDarkVariantForSupportedMaskImagesInDarkMode() {
    listOf("apple", "github", "okx_wallet", "vercel", "x", "linear").forEach { logo ->
      assertEquals(
        "https://img.clerk.com/static/$logo-dark.png",
        "https://img.clerk.com/static/$logo.png".withDarkVariant(isInDarkMode = true),
      )
    }
  }

  @Test
  fun keepsUnsupportedLogosUnchangedInDarkMode() {
    assertEquals(
      "https://img.clerk.com/static/google.png",
      "https://img.clerk.com/static/google.png".withDarkVariant(isInDarkMode = true),
    )
  }

  @Test
  fun keepsSupportedLogosUnchangedInLightMode() {
    assertEquals(
      "https://img.clerk.com/static/github.png",
      "https://img.clerk.com/static/github.png".withDarkVariant(isInDarkMode = false),
    )
  }

  @Test
  fun preservesUrlSuffixWhenUsingDarkVariant() {
    assertEquals(
      "https://img.clerk.com/static/vercel-dark.png?width=64#icon",
      "https://img.clerk.com/static/vercel.png?width=64#icon".withDarkVariant(isInDarkMode = true),
    )
  }
}
