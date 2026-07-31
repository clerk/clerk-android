package com.clerk.ui.userprofile

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.clerk.ui.userprofile.security.Origin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UserProfileNavigationTest {

  @Test
  fun backupCodeDestination_isRegistered() {
    val backStack = NavBackStack<NavKey>(UserProfileDestination.UserProfileAccount)
    val destination = UserProfileDestination.BackupCodeView(codes = listOf("backup-code"))
    val provider = entryProvider {
      userProfileEntries(
        backStack = backStack,
        isDismissible = true,
        onDismiss = {},
        customRows = emptyList(),
        customDestination = null,
        onSwitchAccount = {},
        onAddAccount = {},
      )
    }

    assertNotNull(provider(destination))
  }

  @Test
  fun dismissBackupCodes_fromRegeneration_returnsToSecurity() {
    val backStack =
      NavBackStack<NavKey>(
        UserProfileDestination.UserProfileAccount,
        UserProfileDestination.UserProfileSecurity,
        UserProfileDestination.BackupCodeView(codes = listOf("backup-code")),
      )

    dismissBackupCodes(backStack = backStack, origin = Origin.BackupCodes)

    assertEquals(
      listOf(UserProfileDestination.UserProfileAccount, UserProfileDestination.UserProfileSecurity),
      backStack,
    )
  }

  @Test
  fun dismissBackupCodes_fromAuthenticatorSetup_skipsVerification() {
    val backStack =
      NavBackStack<NavKey>(
        UserProfileDestination.UserProfileAccount,
        UserProfileDestination.UserProfileSecurity,
        TestVerificationDestination,
        UserProfileDestination.BackupCodeView(
          origin = Origin.AuthenticatorApp,
          codes = listOf("backup-code"),
        ),
      )

    dismissBackupCodes(backStack = backStack, origin = Origin.AuthenticatorApp)

    assertEquals(
      listOf(UserProfileDestination.UserProfileAccount, UserProfileDestination.UserProfileSecurity),
      backStack,
    )
  }

  private data object TestVerificationDestination : NavKey
}
