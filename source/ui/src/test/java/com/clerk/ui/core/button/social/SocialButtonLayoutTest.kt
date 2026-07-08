package com.clerk.ui.core.button.social

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SocialButtonLayoutTest {
  @Test
  fun returnsEmptyRowsWhenNoProvidersAreAvailable() {
    val result = distributeSocialButtonsIntoRows(providers = emptyList<Int>())

    assertEquals(emptyList(), result.rows)
    assertFalse(result.lastUsedProviderPresent)
  }

  @Test
  fun distributesProvidersEvenlyAcrossRows() {
    val result = distributeSocialButtonsIntoRows(providers = listOf(1, 2, 3, 4, 5, 6))

    assertEquals(listOf(listOf(1, 2, 3), listOf(4, 5, 6)), result.rows)
    assertFalse(result.lastUsedProviderPresent)
  }

  @Test
  fun separatesLastUsedProviderIntoFirstRow() {
    val result =
      distributeSocialButtonsIntoRows(providers = listOf(1, 2, 3, 4, 5), lastUsedProvider = 3)

    assertEquals(listOf(listOf(3), listOf(1, 2, 4, 5)), result.rows)
    assertTrue(result.lastUsedProviderPresent)
  }

  @Test
  fun leavesProvidersTogetherWhenWithinRowLimitAndLastUsedProviderIsAbsent() {
    val result = distributeSocialButtonsIntoRows(providers = listOf(1, 2, 3, 4, 5))

    assertEquals(listOf(listOf(1, 2, 3, 4, 5)), result.rows)
    assertFalse(result.lastUsedProviderPresent)
  }
}
