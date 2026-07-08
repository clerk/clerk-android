package com.clerk.ui.core.button.social

internal const val MAX_SOCIAL_PROVIDERS_PER_ROW = 5

private const val TWO_BY_TWO_PROVIDER_COUNT = 4

internal fun <T> distributeSocialProvidersIntoRows(
  providers: List<T>,
  maxProvidersPerRow: Int = MAX_SOCIAL_PROVIDERS_PER_ROW,
): List<List<T>> {
  require(maxProvidersPerRow > 0) { "maxProvidersPerRow must be greater than 0" }

  return when {
    providers.isEmpty() -> emptyList()
    providers.size == TWO_BY_TWO_PROVIDER_COUNT && maxProvidersPerRow >= 2 -> providers.chunked(2)
    providers.size <= maxProvidersPerRow -> listOf(providers)
    else -> {
      val rowCount = providers.size.ceilDiv(maxProvidersPerRow)
      val providersPerRow = providers.size.ceilDiv(rowCount)
      providers.chunked(providersPerRow)
    }
  }
}

private fun Int.ceilDiv(other: Int): Int = (this + other - 1) / other
