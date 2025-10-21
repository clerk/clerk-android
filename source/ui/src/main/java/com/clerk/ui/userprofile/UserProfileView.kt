package com.clerk.ui.userprofile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.clerk.ui.userprofile.account.UserProfileAccountView
import com.clerk.ui.userprofile.account.UserProfileAction
import com.clerk.ui.userprofile.security.UserProfileSecurityView
import com.clerk.ui.userprofile.update.UserProfileUpdateProfileView
import kotlinx.serialization.Serializable

@Composable
fun UserProfileView(modifier: Modifier = Modifier) {
  val backStack = rememberNavBackStack(UserProfileDestination.UserProfileAccount)
  NavDisplay(
    modifier = modifier,
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<UserProfileDestination.UserProfileAccount> { key ->
          UserProfileAccountView(
            onClick = {
              when (it) {
                UserProfileAction.Profile -> TODO()
                UserProfileAction.Security ->
                  backStack.add(UserProfileDestination.UserProfileSecurity)
              }
            },
            onBackPressed = { backStack.removeLastOrNull() },
            onClickEdit = { backStack.add(UserProfileDestination.UserProfileUpdate) },
          )
        }
        entry<UserProfileDestination.UserProfile> { key -> }
        entry<UserProfileDestination.UserProfileSecurity> { key -> UserProfileSecurityView() }
        entry<UserProfileDestination.UserProfileUpdate> { key ->
          UserProfileUpdateProfileView(onSuccess = { backStack.removeLastOrNull() })
        }
      },
  )
}

internal object UserProfileDestination {
  @Serializable data object UserProfileAccount : NavKey

  @Serializable data object UserProfile : NavKey

  @Serializable data object UserProfileSecurity : NavKey

  @Serializable data object UserProfileUpdate : NavKey
}
