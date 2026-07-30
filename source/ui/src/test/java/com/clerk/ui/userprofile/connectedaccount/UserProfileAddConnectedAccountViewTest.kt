package com.clerk.ui.userprofile.connectedaccount

import kotlin.test.Test
import kotlin.test.assertEquals

class UserProfileAddConnectedAccountViewTest {

  @Test
  fun successResetsStateAndClosesSheet() {
    val events = mutableListOf<String>()

    handleAddConnectedAccountState(
      state = AddConnectedAccountViewModel.State.Success,
      resetState = { events += "reset" },
      onClosePressed = { events += "close" },
    )

    assertEquals(listOf("reset", "close"), events)
  }

  @Test
  fun nonSuccessStateKeepsSheetOpen() {
    val events = mutableListOf<String>()

    handleAddConnectedAccountState(
      state = AddConnectedAccountViewModel.State.Loading,
      resetState = { events += "reset" },
      onClosePressed = { events += "close" },
    )

    assertEquals(emptyList(), events)
  }
}
