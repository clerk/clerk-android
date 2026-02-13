package com.clerk.api.forceupdate

private val validVersionRegex = Regex("^\\d+(\\.\\d+)*$")

internal object AppVersionComparator {
  fun isValid(version: String): Boolean = validVersionRegex.matches(version)

  fun compare(current: String, minimum: String): Int? {
    if (!isValid(current) || !isValid(minimum)) {
      return null
    }

    val currentSegments = current.split(".").map { it.toInt() }
    val minimumSegments = minimum.split(".").map { it.toInt() }
    val maxSegments = maxOf(currentSegments.size, minimumSegments.size)

    repeat(maxSegments) { index ->
      val currentValue = currentSegments.getOrElse(index) { 0 }
      val minimumValue = minimumSegments.getOrElse(index) { 0 }

      if (currentValue != minimumValue) {
        return currentValue - minimumValue
      }
    }

    return 0
  }

  fun isSupported(current: String, minimum: String): Boolean? = compare(current, minimum)?.let { it >= 0 }
}
