package com.clerk.snapshot.userprofile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.clerk.base.BaseSnapshotTest
import com.clerk.ui.userprofile.UserProfileDestination
import com.clerk.ui.userprofile.userProfileEntries
import kotlin.test.Test
import kotlin.test.assertNotNull

class UserProfileBackupCodeDestinationSnapshotTest : BaseSnapshotTest() {

  @Test
  fun backupCodeDestination_rendersWithClerkTheme() {
    val destination = UserProfileDestination.BackupCodeView(codes = listOf("backup-code"))
    val backStack =
      NavBackStack<NavKey>(
        UserProfileDestination.UserProfileAccount,
        UserProfileDestination.UserProfileSecurity,
        destination,
      )
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
    val entry = assertNotNull(provider(destination))

    paparazzi.snapshot {
      Box(modifier = Modifier.size(width = 412.dp, height = 915.dp)) { entry.Content() }
    }
  }
}
