package com.clerk.ui.core.extensions

private val supportsMaskImage = setOf("apple", "github", "okx_wallet", "vercel", "x", "linear")

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
