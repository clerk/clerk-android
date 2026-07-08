package com.clerk.ui.core.button.social

import androidx.annotation.VisibleForTesting

internal const val MAX_SOCIAL_BUTTONS_PER_ROW = 5

/**
 * Evenly distributes social providers into rows using the same rules as Clerk's web components.
 *
 * If [lastUsedProvider] is present in [providers], it is separated into its own first row and the
 * remaining providers are distributed independently.
 */
@VisibleForTesting
internal fun <T> distributeSocialButtonsIntoRows(
  providers: List<T>,
  maxProvidersPerRow: Int = MAX_SOCIAL_BUTTONS_PER_ROW,
  lastUsedProvider: T? = null,
): SocialButtonRows<T> {
  if (providers.isEmpty()) {
    return SocialButtonRows(rows = emptyList(), lastUsedProviderPresent = false)
  }

  if (lastUsedProvider != null && providers.contains(lastUsedProvider)) {
    val remainingProviders = providers.filter { it != lastUsedProvider }
    if (remainingProviders.isEmpty()) {
      return SocialButtonRows(
        rows = listOf(listOf(lastUsedProvider)),
        lastUsedProviderPresent = true,
      )
    }

    val remainingRows =
      distributeSocialButtonsIntoRows(
        providers = remainingProviders,
        maxProvidersPerRow = maxProvidersPerRow,
        lastUsedProvider = null,
      )
    return SocialButtonRows(
      rows = listOf(listOf(lastUsedProvider)) + remainingRows.rows,
      lastUsedProviderPresent = true,
    )
  }

  if (providers.size <= maxProvidersPerRow) {
    return SocialButtonRows(rows = listOf(providers), lastUsedProviderPresent = false)
  }

  val rowCount = ceilDiv(providers.size, maxProvidersPerRow)
  val providersPerRow = ceilDiv(providers.size, rowCount)
  val rows = providers.chunked(providersPerRow)
  return SocialButtonRows(rows = rows, lastUsedProviderPresent = false)
}

@VisibleForTesting
internal data class SocialButtonRows<T>(
  val rows: List<List<T>>,
  val lastUsedProviderPresent: Boolean,
)

private fun ceilDiv(value: Int, divisor: Int): Int = (value + divisor - 1) / divisor
