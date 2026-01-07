package com.clerk.ui.theme.colors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min

private const val DARK_LUMINANCE_THRESHOLD = 0.5f

/**
 * Determines if this color is considered "dark" based on its luminance. A color is considered dark
 * if its luminance is below 0.5. This matches the iOS implementation for calculating pressed button
 * colors.
 */
internal val Color.isDark: Boolean
  get() = luminance() < DARK_LUMINANCE_THRESHOLD

private val LIGHT_SHADES = listOf("400", "300", "200", "150", "100", "50", "25")
private val DARK_SHADES = listOf("600", "700", "750", "800", "850", "900", "950")

// ——— Domain constants (magic numbers → names) ———
private const val OPAQUE = 1f
private const val ZERO = 0f
private const val ONE = 1f
private const val HALF = 0.5f
private const val TWO = 2f
private const val SIX = 6f

// Hue fractions commonly used in HSL → RGB conversion
private const val ONE_THIRD = 1f / 3f
private const val ONE_SIXTH = 1f / 6f
private const val TWO_THIRDS = 2f / 3f

// Lightness targets for your scale
private const val TARGET_L_25 = 0.97f
private const val TARGET_L_900 = 0.12f

private data class HSL(val h: Float, val s: Float, val l: Float)

private fun clamp01(x: Float): Float = max(ZERO, min(ONE, x))

// @Suppress("MagicNumber") // <- Optional: keep off if the constants above are enough.
private fun rgbToHsl(c: Color): HSL {
  val r = c.red
  val g = c.green
  val b = c.blue

  val maxv = max(r, max(g, b))
  val minv = min(r, min(g, b))
  val l = (maxv + minv) / TWO

  if (maxv == minv) return HSL(0f, 0f, l)

  val d = maxv - minv
  val s = if (l > HALF) d / (TWO - maxv - minv) else d / (maxv + minv)

  val h =
    when (maxv) {
      r -> (g - b) / d + (if (g < b) SIX else 0f)
      g -> (b - r) / d + 2f
      else -> (r - g) / d + 4f
    } / SIX

  return HSL(h, s, l)
}

// @Suppress("MagicNumber") // <- Optional suppression if your Detekt config is extra strict.
private fun hsla(h: Float, s: Float, l: Float, a: Float): Color {
  if (s == 0f) return Color(l, l, l, a)

  val q = if (l < HALF) l * (ONE + s) else l + s - l * s
  val p = TWO * l - q

  fun hue2rgb(t: Float): Float {
    var tt = t
    if (tt < ZERO) tt += ONE
    if (tt > ONE) tt -= ONE

    return when {
      tt < ONE_SIXTH -> p + (q - p) * SIX * tt
      tt < HALF -> q
      tt < TWO_THIRDS -> p + (q - p) * (TWO_THIRDS - tt) * SIX
      else -> p
    }
  }

  val r = hue2rgb(h + ONE_THIRD)
  val g = hue2rgb(h)
  val b = hue2rgb(h - ONE_THIRD)
  return Color(r, g, b, a)
}

internal fun Color.generateDangerPaletteHsl(): DangerPalette {
  val base = rgbToHsl(this)

  // Step sizes are derived from list lengths (no hidden numbers).
  val lightStep = (TARGET_L_25 - base.l) / LIGHT_SHADES.size
  val darkStep = (base.l - TARGET_L_900) / DARK_SHADES.size

  fun shade(name: String): Color =
    when (name) {
      "500" -> hsla(base.h, base.s, base.l, OPAQUE)
      in LIGHT_SHADES -> {
        val i = LIGHT_SHADES.indexOf(name)
        hsla(base.h, base.s, clamp01(base.l + (i + 1) * lightStep), OPAQUE)
      }
      in DARK_SHADES -> {
        val i = DARK_SHADES.indexOf(name)
        hsla(base.h, base.s, clamp01(base.l - (i + 1) * darkStep), OPAQUE)
      }
      else -> hsla(base.h, base.s, base.l, OPAQUE)
    }

  return DangerPalette(
    danger25 = shade("25"),
    danger50 = shade("50"),
    danger100 = shade("100"),
    danger150 = shade("150"),
    danger200 = shade("200"),
    danger300 = shade("300"),
    danger400 = shade("400"),
    danger500 = shade("500"),
    danger600 = shade("600"),
    danger700 = shade("700"),
    danger750 = shade("750"),
    danger800 = shade("800"),
    danger850 = shade("850"),
    danger900 = shade("900"),
    // Should align with your deepest target; tune TARGET_L_900 if you want #350808 precisely.
    danger950 = shade("950"),
  )
}
