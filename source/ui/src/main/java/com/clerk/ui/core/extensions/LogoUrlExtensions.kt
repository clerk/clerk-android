package com.clerk.ui.core.extensions

private val supportsMaskImage = setOf("apple", "github", "okx_wallet", "vercel", "x", "linear")

/**
 * Returns a new URL string for a dark mode variant of an image if [isInDarkMode] is true and the
 * logo supports dark mode masking.
 *
 * This function assumes the original URL points to a supported `.png` file. It replaces the `.png`
 * extension with `-dark.png` to form the new URL. If [isInDarkMode] is false, or the logo is not in
 * the supported mask list, it returns the original string unchanged.
 *
 * Example: `"https://img.clerk.com/logo.png".withDarkVariant(true)` returns
 * `"https://img.clerk.com/logo-dark.png"` `"https://img.clerk.com/logo.png".withDarkVariant(false)`
 * returns `"https://img.clerk.com/logo.png"`
 *
 * @param isInDarkMode A boolean indicating if the dark mode variant should be used.
 * @return The modified URL string for dark mode, or the original string otherwise.
 */
internal fun String.withDarkVariant(isInDarkMode: Boolean): String {
  if (!isInDarkMode || !supportsDarkModeMask()) return this

  val suffixStart = indexOfFirst { it == '?' || it == '#' }.takeUnless { it == -1 } ?: length
  return substring(0, suffixStart).removeSuffix(".png") + "-dark.png" + substring(suffixStart)
}

private fun String.supportsDarkModeMask(): Boolean {
  val path = substringBefore("?").substringBefore("#")
  val fileName = path.substringAfterLast("/")
  if (!fileName.endsWith(".png")) return false

  val logoSlug = fileName.removeSuffix(".png")
  return logoSlug in supportsMaskImage
}
