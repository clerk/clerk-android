package com.clerk.ui.util

import androidx.compose.ui.graphics.Color

// Color manipulation constants
private const val MIN_PERCENTAGE = 0f
private const val MAX_PERCENTAGE = 1f
private const val RGB_MAX_VALUE = 255
private const val RGB_MAX_VALUE_FLOAT = 255f

object ColorUtil {
  /**
   * Lightens a color by a given percentage.
   *
   * @param percentage The percentage to lighten (0.0 to 1.0, where 0.1 = 10% lighter)
   * @return A new Color that is lightened by the specified percentage
   */
  fun Color.lighten(percentage: Float): Color {
    // Ensure percentage is between 0 and 1
    val factor = percentage.coerceIn(MIN_PERCENTAGE, MAX_PERCENTAGE)

    // Convert to 0-255 range for calculation
    val r = (red * RGB_MAX_VALUE).toInt()
    val g = (green * RGB_MAX_VALUE).toInt()
    val b = (blue * RGB_MAX_VALUE).toInt()

    // Calculate how much room there is to lighten each channel
    val newR = (r + (RGB_MAX_VALUE - r) * factor).toInt()
    val newG = (g + (RGB_MAX_VALUE - g) * factor).toInt()
    val newB = (b + (RGB_MAX_VALUE - b) * factor).toInt()

    // Return new color with same alpha
    return Color(
      red = newR / RGB_MAX_VALUE_FLOAT,
      green = newG / RGB_MAX_VALUE_FLOAT,
      blue = newB / RGB_MAX_VALUE_FLOAT,
      alpha = alpha,
    )
  }
}

/**
 * Darkens a color by a given percentage.
 *
 * @param percentage The percentage to darken (0.0 to 1.0, where 0.1 = 10% darker)
 * @return A new Color that is darkened by the specified percentage
 */
fun Color.darken(percentage: Float): Color {
  // Ensure percentage is between 0 and 1
  val factor = percentage.coerceIn(MIN_PERCENTAGE, MAX_PERCENTAGE)

  // Convert to 0-255 range for calculation
  val r = (red * RGB_MAX_VALUE).toInt()
  val g = (green * RGB_MAX_VALUE).toInt()
  val b = (blue * RGB_MAX_VALUE).toInt()

  // Calculate darkened values by reducing each channel toward 0
  val newR = (r * (MAX_PERCENTAGE - factor)).toInt()
  val newG = (g * (MAX_PERCENTAGE - factor)).toInt()
  val newB = (b * (MAX_PERCENTAGE - factor)).toInt()

  // Return new color with same alpha
  return Color(
    red = newR / RGB_MAX_VALUE_FLOAT,
    green = newG / RGB_MAX_VALUE_FLOAT,
    blue = newB / RGB_MAX_VALUE_FLOAT,
    alpha = alpha,
  )
}
