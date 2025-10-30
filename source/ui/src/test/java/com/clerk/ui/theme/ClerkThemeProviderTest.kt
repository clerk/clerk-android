package com.clerk.ui.theme

import androidx.compose.ui.graphics.Color
import com.clerk.api.ui.ClerkColors
import com.clerk.api.ui.ClerkTheme
import org.junit.Assert.assertEquals
import org.junit.Test

class ClerkThemeProviderTest {

  @Test
  fun resolveColors_usesDefaultPaletteWhenNoOverridesProvided_lightMode() {
    val resolved = resolveColors(theme = null, isDarkMode = false)

    assertEquals(DefaultColors.light.primary, resolved.primary)
    assertEquals(DefaultColors.light.background, resolved.background)
  }

  @Test
  fun resolveColors_appliesGlobalOverridesToBothModes() {
    val theme =
      ClerkTheme(colors = ClerkColors(primary = Color(0xFFFF00FF), background = Color(0xFF112233)))

    val lightResolved = resolveColors(theme = theme, isDarkMode = false)
    val darkResolved = resolveColors(theme = theme, isDarkMode = true)

    assertEquals(Color(0xFFFF00FF), lightResolved.primary)
    assertEquals(Color(0xFFFF00FF), darkResolved.primary)
    assertEquals(Color(0xFF112233), lightResolved.background)
    assertEquals(Color(0xFF112233), darkResolved.background)
  }

  @Test
  fun resolveColors_prefersModeOverridesWhenAvailable() {
    val theme =
      ClerkTheme(
        colors = ClerkColors(primary = Color(0xFF00FF00), background = Color(0xFFABCDEF)),
        darkColors = ClerkColors(primary = Color(0xFF123456)),
      )

    val lightResolved = resolveColors(theme = theme, isDarkMode = false)
    val darkResolved = resolveColors(theme = theme, isDarkMode = true)

    assertEquals(Color(0xFF00FF00), lightResolved.primary)
    assertEquals(Color(0xFF123456), darkResolved.primary)
    assertEquals(Color(0xFFABCDEF), lightResolved.background)
    assertEquals(Color(0xFFABCDEF), darkResolved.background)
  }

  @Test
  fun resolveColors_fallsBackToDefaultsWhenOverrideMissing() {
    val theme =
      ClerkTheme(lightColors = ClerkColors(primary = Color(0xFF212121)), darkColors = ClerkColors())

    val lightResolved = resolveColors(theme = theme, isDarkMode = false)
    val darkResolved = resolveColors(theme = theme, isDarkMode = true)

    assertEquals(Color(0xFF212121), lightResolved.primary)
    assertEquals(DefaultColors.dark.primary, darkResolved.primary)
    assertEquals(DefaultColors.light.background, lightResolved.background)
    assertEquals(DefaultColors.dark.background, darkResolved.background)
  }
}
