package com.clerk.ui.navigation

import com.clerk.api.FrameworkIntegrationApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(FrameworkIntegrationApi::class)
class ClerkEmbeddedNavigationTest {

  @Test
  fun `canGoBack reflects depth`() {
    val embeddedNavigation = ClerkEmbeddedNavigation()

    assertFalse(embeddedNavigation.canGoBack)

    embeddedNavigation.depth = 2

    assertEquals(2, embeddedNavigation.depth)
    assertTrue(embeddedNavigation.canGoBack)
  }

  @Test
  fun `pop routes to registered handler`() {
    val embeddedNavigation = ClerkEmbeddedNavigation()
    val pops = mutableListOf<Boolean>()
    embeddedNavigation.popHandler = { toRoot -> pops.add(toRoot) }

    embeddedNavigation.pop()
    embeddedNavigation.popToRoot()

    assertEquals(listOf(false, true), pops)
  }

  @Test
  fun `pop is a no-op without a registered handler`() {
    val embeddedNavigation = ClerkEmbeddedNavigation()

    embeddedNavigation.pop()
    embeddedNavigation.popToRoot()
  }

  @Test
  fun `clearing the handler stops routing pops`() {
    val embeddedNavigation = ClerkEmbeddedNavigation()
    val pops = mutableListOf<Boolean>()
    embeddedNavigation.popHandler = { toRoot -> pops.add(toRoot) }
    embeddedNavigation.popHandler = null

    embeddedNavigation.pop()

    assertTrue(pops.isEmpty())
  }
}
