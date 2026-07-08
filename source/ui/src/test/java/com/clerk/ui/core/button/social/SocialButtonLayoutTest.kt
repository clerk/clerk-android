package com.clerk.ui.core.button.social

import kotlin.test.Test
import kotlin.test.assertEquals

class SocialButtonLayoutTest {

  @Test
  fun distributesFourProvidersAsTwoByTwoGrid() {
    val rows = distributeSocialProvidersIntoRows(listOf(1, 2, 3, 4))

    assertEquals(listOf(listOf(1, 2), listOf(3, 4)), rows)
  }

  @Test
  fun keepsThreeAndFiveProvidersInOneRow() {
    assertEquals(listOf(listOf(1, 2, 3)), distributeSocialProvidersIntoRows(listOf(1, 2, 3)))
    assertEquals(
      listOf(listOf(1, 2, 3, 4, 5)),
      distributeSocialProvidersIntoRows(listOf(1, 2, 3, 4, 5)),
    )
  }

  @Test
  fun balancesProviderRowsUsingWebDistribution() {
    assertEquals(
      listOf(listOf(1, 2, 3), listOf(4, 5, 6)),
      distributeSocialProvidersIntoRows((1..6).toList()),
    )
    assertEquals(
      listOf(listOf(1, 2, 3, 4), listOf(5, 6, 7)),
      distributeSocialProvidersIntoRows((1..7).toList()),
    )
    assertEquals(
      listOf(listOf(1, 2, 3, 4, 5), listOf(6, 7, 8, 9)),
      distributeSocialProvidersIntoRows((1..9).toList()),
    )
  }

  @Test
  fun returnsNoRowsForEmptyProviders() {
    assertEquals(emptyList(), distributeSocialProvidersIntoRows(emptyList<Int>()))
  }
}
