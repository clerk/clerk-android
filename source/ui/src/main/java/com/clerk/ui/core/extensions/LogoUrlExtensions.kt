package com.clerk.ui.core.extensions

/**
 * Returns a new URL string for a dark mode variant of an image if [isInDarkMode] is true.
 *
 * This function assumes the original URL points to a `.png` file. It replaces the `.png` extension
 * with `-dark.png` to form the new URL. If [isInDarkMode] is false, it returns the original string
 * unchanged.
 *
 * Example: `"https://img.clerk.com/logo.png".withDarkVariant(true)` returns
 * `"https://img.clerk.com/logo-dark.png"` `"https://img.clerk.com/logo.png".withDarkVariant(false)`
 * returns `"https://img.clerk.com/logo.png"`
 *
 * @param isInDarkMode A boolean indicating if the dark mode variant should be used.
 * @return The modified URL string for dark mode, or the original string otherwise.
 */
internal fun String.withDarkVariant(isInDarkMode: Boolean): String {
  return if (isInDarkMode) {
    this.replace(".png", "-dark.png")
  } else {
    this
  }
}
