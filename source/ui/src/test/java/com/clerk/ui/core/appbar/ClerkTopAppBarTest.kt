package com.clerk.ui.core.appbar

import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ClerkTopAppBarTest {

  @Test
  fun internalBackActionTakesPrecedenceOverHostAction() {
    val internalBackAction = {}
    val hostBackAction = {}

    val resolvedAction =
      resolveBackAction(
        hasBackButton = true,
        usesHostBackAction = true,
        onBackPressed = internalBackAction,
        hostBackAction = hostBackAction,
      )

    assertSame(internalBackAction, resolvedAction)
  }

  @Test
  fun rootScreenUsesHostBackActionWhenInternalBackButtonIsHidden() {
    val hostBackAction = {}

    val resolvedAction =
      resolveBackAction(
        hasBackButton = false,
        usesHostBackAction = true,
        onBackPressed = {},
        hostBackAction = hostBackAction,
      )

    assertSame(hostBackAction, resolvedAction)
  }

  @Test
  fun nonRootScreenIgnoresHostBackActionWhenInternalBackButtonIsHidden() {
    val resolvedAction =
      resolveBackAction(
        hasBackButton = false,
        usesHostBackAction = false,
        onBackPressed = {},
        hostBackAction = {},
      )

    assertNull(resolvedAction)
  }
}
