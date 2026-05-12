package com.clerk.ui.userprofile.account

import com.clerk.ui.userprofile.custom.UserProfileRow
import kotlin.test.Test
import kotlin.test.assertEquals

class UserProfileAccountViewTest {

  @Test
  fun accountBuiltInRows_includesAddAccountWithOneSession() {
    assertEquals(
      listOf(UserProfileRow.AddAccount, UserProfileRow.SignOut),
      accountBuiltInRows(sessionCount = 1),
    )
  }

  @Test
  fun accountBuiltInRows_includesSwitchAccountWithMultipleSessions() {
    assertEquals(
      listOf(UserProfileRow.SwitchAccount, UserProfileRow.AddAccount, UserProfileRow.SignOut),
      accountBuiltInRows(sessionCount = 2),
    )
  }

  @Test
  fun accountBuiltInRows_hidesMultiSessionRowsWhenDisabled() {
    assertEquals(
      listOf(UserProfileRow.SignOut),
      accountBuiltInRows(sessionCount = 2, multiSessionModeIsEnabled = false),
    )
  }
}
