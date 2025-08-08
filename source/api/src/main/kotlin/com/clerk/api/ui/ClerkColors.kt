package com.clerk.api.ui

/**
 * A collection of themed colors used throughout Clerkʼs design system.
 *
 * Each property represents a semantic color token that can be consumed by the UI layer. Rather than
 * referencing hard-coded ARGB values directly in UI code, prefer using these semantic tokens so
 * that color definitions can be swapped out at runtime (e.g. for dark mode) or adjusted centrally
 * without touching all call-sites.
 */
data class ClerkColors(
  /** Main brand color used for primary actions and highlights. */
  val primary: ThemeColor,
  /** Default surface background. */
  val background: ThemeColor,
  /** Background for input fields such as `TextField`. */
  val input: ThemeColor,
  /** Color used to convey destructive or error states. */
  val danger: ThemeColor,
  /** Color used to convey success states. */
  val success: ThemeColor,
  /** Color used to convey warning states. */
  val warning: ThemeColor,
  /** Default foreground (text/icon) color. */
  val foreground: ThemeColor,
  /** A slightly subdued foreground color for secondary content. */
  val mutedForeground: ThemeColor,
  /** Foreground color that pairs with [primary]. */
  val primaryForeground: ThemeColor,
  /** Foreground color that pairs with [input]. */
  val inputForeground: ThemeColor,
  /** Neutral gray used for borders or separators. */
  val neutral: ThemeColor,
  /** Border color used for input fields and other elements. */
  val border: ThemeColor,
  /** Stroke color used for focus rings. */
  val ring: ThemeColor,
  /** Muted background color for minimal emphasis surfaces. */
  val muted: ThemeColor,
  /** Shadow color used when drawing elevation overlays. */
  val shadow: ThemeColor,
)

/**
 * Lightweight wrapper around an ARGB color expressed as a [Long].
 *
 * The inline constructors make it convenient to create a [ThemeColor] either from an `Int` ARGB
 * literal or by specifying individual red, green, blue, and optional alpha channel components.
 *
 * @param argb The packed ARGB color as a 32-bit integer.
 * @param alpha The alpha component `0x00..0xFF`.
 * @param red The red component `0x00..0xFF`.
 * @param green The green component `0x00..0xFF`.
 * @param blue The blue component `0x00..0xFF`.
 * @constructor Creates a [ThemeColor] from an ARGB packed [Int] value.
 * @constructor Creates a [ThemeColor] from individual channel components. If [alpha] is omitted it
 *   defaults to fully opaque (0xFF).
 */
data class ThemeColor(
  /** The packed ARGB color as a 32-bit integer. */
  val argb: Long
) {
  /** Creates a [ThemeColor] from an ARGB packed [Int] value. */
  constructor(argb: Int) : this(argb.toLong())

  /**
   * Creates a [ThemeColor] from individual channel components. If [alpha] is omitted it defaults to
   * fully opaque (0xFF).
   *
   * @param alpha The alpha component `0x00..0xFF`.
   * @param red The red component `0x00..0xFF`.
   * @param green The green component `0x00..0xFF`.
   * @param blue The blue component `0x00..0xFF`.
   */
  constructor(
    alpha: Int = DEFAULT_ALPHA,
    red: Int,
    green: Int,
    blue: Int,
  ) : this(
    (alpha.toLong() shl ALPHA_SHIFT) or
      (red.toLong() shl RED_SHIFT) or
      (green.toLong() shl GREEN_SHIFT) or
      blue.toLong()
  )

  companion object {
    private const val DEFAULT_ALPHA = 0xFF
    private const val ALPHA_SHIFT = 24
    private const val RED_SHIFT = 16
    private const val GREEN_SHIFT = 8
  }
}
