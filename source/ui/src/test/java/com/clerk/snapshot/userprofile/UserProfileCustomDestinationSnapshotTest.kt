package com.clerk.snapshot.userprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.clerk.base.BaseSnapshotTest
import com.clerk.ui.R
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.userprofile.UserProfileDestination
import com.clerk.ui.userprofile.custom.CustomRouteNavKey
import com.clerk.ui.userprofile.custom.LocalUserProfileCustomNavigator
import com.clerk.ui.userprofile.custom.UserProfileCustomNavigator
import com.clerk.ui.userprofile.custom.UserProfileCustomRow
import com.clerk.ui.userprofile.custom.UserProfileRowIcon
import com.clerk.ui.userprofile.userProfileEntries
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UserProfileCustomDestinationSnapshotTest : BaseSnapshotTest() {

  @Test
  fun customDestination_preservesHeaderAboveFullSizeContent() {
    snapshotCustomDestination(emptyList())
  }

  @Test
  fun customDestination_showsMatchingRowTitle() {
    snapshotCustomDestination(
      listOf(
        UserProfileCustomRow("billing", "Billing", UserProfileRowIcon.Resource(R.drawable.ic_user)),
        UserProfileCustomRow(
          "billing/details",
          "Billing details",
          UserProfileRowIcon.Resource(R.drawable.ic_user),
        ),
      )
    )
  }

  private fun snapshotCustomDestination(customRows: List<UserProfileCustomRow>) {
    val destination = CustomRouteNavKey("billing/details")
    val backStack =
      NavBackStack<NavKey>(
        UserProfileDestination.UserProfileAccount,
        CustomRouteNavKey("billing"),
        destination,
      )
    var navigator: UserProfileCustomNavigator? = null
    val provider = entryProvider {
      userProfileEntries(
        backStack = backStack,
        isDismissible = false,
        onDismiss = { error("Custom page back must not dismiss the profile") },
        customRows = customRows,
        customDestination = { routeKey ->
          navigator = LocalUserProfileCustomNavigator.current
          Box(modifier = Modifier.fillMaxSize().background(ClerkMaterialTheme.colors.muted)) {
            Text(text = routeKey)
          }
        },
        onSwitchAccount = {},
        onAddAccount = {},
      )
    }
    val entry = assertNotNull(provider(destination))

    paparazzi.snapshot {
      Box(modifier = Modifier.size(width = 412.dp, height = 915.dp)) { entry.Content() }
    }

    assertNotNull(navigator).navigateBack()
    assertEquals(
      listOf(UserProfileDestination.UserProfileAccount, CustomRouteNavKey("billing")),
      backStack,
    )
  }
}
