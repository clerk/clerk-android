package com.clerk.ui

import androidx.compose.ui.graphics.Color
import com.clerk.ui.util.ColorUtil.lighten
import com.clerk.ui.util.darken
import org.junit.Assert.assertEquals
import org.junit.Test

class ColorUtilsTest {

  @Test
  fun `lighten should lighten black color correctly`() {
    val black = Color.Black
    val lightened = black.lighten(0.5f)

    // Black lightened by 50% should be gray (128/255 = 0.502)
    assertEquals(0.502f, lightened.red, 0.01f)
    assertEquals(0.502f, lightened.green, 0.01f)
    assertEquals(0.502f, lightened.blue, 0.01f)
    assertEquals(1f, lightened.alpha, 0f) // Alpha should remain unchanged
  }

  @Test
  fun `lighten should not change white color`() {
    val white = Color.White
    val lightened = white.lighten(0.5f)

    // White cannot be lightened further
    assertEquals(1f, lightened.red, 0f)
    assertEquals(1f, lightened.green, 0f)
    assertEquals(1f, lightened.blue, 0f)
    assertEquals(1f, lightened.alpha, 0f)
  }

  @Test
  fun `lighten should handle zero percentage`() {
    val red = Color.Red
    val lightened = red.lighten(0f)

    // 0% lightening should return original color
    assertEquals(red.red, lightened.red, 0f)
    assertEquals(red.green, lightened.green, 0f)
    assertEquals(red.blue, lightened.blue, 0f)
    assertEquals(red.alpha, lightened.alpha, 0f)
  }

  @Test
  fun `lighten should handle full percentage`() {
    val red = Color.Red
    val lightened = red.lighten(1f)

    // 100% lightening should produce white
    assertEquals(1f, lightened.red, 0f)
    assertEquals(1f, lightened.green, 0f)
    assertEquals(1f, lightened.blue, 0f)
    assertEquals(red.alpha, lightened.alpha, 0f)
  }

  @Test
  fun `lighten should clamp percentage above 1`() {
    val red = Color.Red
    val lightened = red.lighten(1.5f) // Above 1.0

    // Should be clamped to 1.0, producing white
    assertEquals(1f, lightened.red, 0f)
    assertEquals(1f, lightened.green, 0f)
    assertEquals(1f, lightened.blue, 0f)
  }

  @Test
  fun `lighten should clamp negative percentage`() {
    val red = Color.Red
    val lightened = red.lighten(-0.5f) // Negative

    // Should be clamped to 0.0, producing original color
    assertEquals(red.red, lightened.red, 0f)
    assertEquals(red.green, lightened.green, 0f)
    assertEquals(red.blue, lightened.blue, 0f)
  }

  @Test
  fun `lighten should preserve alpha channel`() {
    val semiTransparentRed = Color(1f, 0f, 0f, 0.5f)
    val lightened = semiTransparentRed.lighten(0.5f)

    // Alpha should remain unchanged (allowing small tolerance for RGB conversion precision)
    assertEquals(0.5f, lightened.alpha, 0.01f)
  }

  @Test
  fun `darken should darken white color correctly`() {
    val white = Color.White
    val darkened = white.darken(0.5f)

    // White darkened by 50% should be gray (127.5/255 ≈ 0.5)
    assertEquals(0.5f, darkened.red, 0.01f)
    assertEquals(0.5f, darkened.green, 0.01f)
    assertEquals(0.5f, darkened.blue, 0.01f)
    assertEquals(1f, darkened.alpha, 0f) // Alpha should remain unchanged
  }

  @Test
  fun `darken should not change black color`() {
    val black = Color.Black
    val darkened = black.darken(0.5f)

    // Black cannot be darkened further
    assertEquals(0f, darkened.red, 0f)
    assertEquals(0f, darkened.green, 0f)
    assertEquals(0f, darkened.blue, 0f)
    assertEquals(1f, darkened.alpha, 0f)
  }

  @Test
  fun `darken should handle zero percentage`() {
    val red = Color.Red
    val darkened = red.darken(0f)

    // 0% darkening should return original color
    assertEquals(red.red, darkened.red, 0f)
    assertEquals(red.green, darkened.green, 0f)
    assertEquals(red.blue, darkened.blue, 0f)
    assertEquals(red.alpha, darkened.alpha, 0f)
  }

  @Test
  fun `darken should handle full percentage`() {
    val red = Color.Red
    val darkened = red.darken(1f)

    // 100% darkening should produce black
    assertEquals(0f, darkened.red, 0f)
    assertEquals(0f, darkened.green, 0f)
    assertEquals(0f, darkened.blue, 0f)
    assertEquals(red.alpha, darkened.alpha, 0f)
  }

  @Test
  fun `darken should clamp percentage above 1`() {
    val red = Color.Red
    val darkened = red.darken(1.5f) // Above 1.0

    // Should be clamped to 1.0, producing black
    assertEquals(0f, darkened.red, 0f)
    assertEquals(0f, darkened.green, 0f)
    assertEquals(0f, darkened.blue, 0f)
  }

  @Test
  fun `darken should clamp negative percentage`() {
    val red = Color.Red
    val darkened = red.darken(-0.5f) // Negative

    // Should be clamped to 0.0, producing original color
    assertEquals(red.red, darkened.red, 0f)
    assertEquals(red.green, darkened.green, 0f)
    assertEquals(red.blue, darkened.blue, 0f)
  }

  @Test
  fun `darken should preserve alpha channel`() {
    val semiTransparentRed = Color(1f, 0f, 0f, 0.5f)
    val darkened = semiTransparentRed.darken(0.5f)

    // Alpha should remain unchanged (allowing small tolerance for RGB conversion precision)
    assertEquals(0.5f, darkened.alpha, 0.01f)
  }

  @Test
  fun `lighten then darken should move in expected direction`() {
    val originalColor = Color(0.5f, 0.5f, 0.5f, 1f)
    val percentage = 0.3f

    // Lighten should make it brighter
    val lightened = originalColor.lighten(percentage)
    assert(lightened.red > originalColor.red)
    assert(lightened.green > originalColor.green)
    assert(lightened.blue > originalColor.blue)

    // Then darken should make it darker than lightened (though not necessarily back to original)
    val backToDark = lightened.darken(percentage)
    assert(backToDark.red < lightened.red)
    assert(backToDark.green < lightened.green)
    assert(backToDark.blue < lightened.blue)

    // Alpha should be preserved throughout
    assertEquals(originalColor.alpha, lightened.alpha, 0f)
    assertEquals(originalColor.alpha, backToDark.alpha, 0f)
  }

  @Test
  fun `lighten should work correctly with mid-range colors`() {
    val gray = Color(0.5f, 0.5f, 0.5f, 1f)
    val lightened = gray.lighten(0.4f)

    // Gray (128) lightened by 40% should be (128 + (255-128)*0.4) = 178.8 ≈ 0.701
    assertEquals(0.701f, lightened.red, 0.01f)
    assertEquals(0.701f, lightened.green, 0.01f)
    assertEquals(0.701f, lightened.blue, 0.01f)
  }

  @Test
  fun `darken should work correctly with mid-range colors`() {
    val gray = Color(0.5f, 0.5f, 0.5f, 1f)
    val darkened = gray.darken(0.4f)

    // Gray (128) darkened by 40% should be (128 * 0.6) = 76.8 ≈ 0.301
    assertEquals(0.301f, darkened.red, 0.01f)
    assertEquals(0.301f, darkened.green, 0.01f)
    assertEquals(0.301f, darkened.blue, 0.01f)
  }
}
