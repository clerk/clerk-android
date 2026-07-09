package com.clerk.ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClerkHostedNavigationTest {

  @Test
  fun `canGoBack reflects depth`() {
    val hostedNavigation = ClerkHostedNavigation()

    assertFalse(hostedNavigation.canGoBack)

    hostedNavigation.depth = 2

    assertEquals(2, hostedNavigation.depth)
    assertTrue(hostedNavigation.canGoBack)
  }

  @Test
  fun `pop routes to registered handler`() {
    val hostedNavigation = ClerkHostedNavigation()
    val pops = mutableListOf<Boolean>()
    hostedNavigation.popHandler = { toRoot -> pops.add(toRoot) }

    hostedNavigation.pop()
    hostedNavigation.popToRoot()

    assertEquals(listOf(false, true), pops)
  }

  @Test
  fun `pop is a no-op without a registered handler`() {
    val hostedNavigation = ClerkHostedNavigation()

    hostedNavigation.pop()
    hostedNavigation.popToRoot()
  }

  @Test
  fun `clearing the handler stops routing pops`() {
    val hostedNavigation = ClerkHostedNavigation()
    val pops = mutableListOf<Boolean>()
    hostedNavigation.popHandler = { toRoot -> pops.add(toRoot) }
    hostedNavigation.popHandler = null

    hostedNavigation.pop()

    assertTrue(pops.isEmpty())
  }
}
