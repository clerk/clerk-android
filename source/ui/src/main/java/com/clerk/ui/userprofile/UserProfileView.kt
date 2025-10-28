package com.clerk.ui.userprofile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.clerk.ui.core.navigation.pop
import com.clerk.ui.userprofile.account.UserProfileAccountView
import com.clerk.ui.userprofile.account.UserProfileAction
import com.clerk.ui.userprofile.security.UserProfileSecurityView
import com.clerk.ui.userprofile.security.password.PasswordAction
import com.clerk.ui.userprofile.security.password.UserProfileCurrentPasswordView
import com.clerk.ui.userprofile.security.password.UserProfileNewPasswordView
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
        entry<UserProfileDestination.UserProfileSecurity> { key ->
          UserProfileSecurityView(
            onAction = { action ->
              if (action == PasswordAction.Add) {
                backStack.add(UserProfileDestination.UpdatePasswordNew(passwordAction = action))
              } else {
                backStack.add(UserProfileDestination.UpdatePasswordCurrent(action))
              }
            }
          )
        }
        entry<UserProfileDestination.UserProfileUpdate> { key ->
          UserProfileUpdateProfileView(onSuccess = { backStack.removeLastOrNull() })
        }
        entry<UserProfileDestination.UpdatePasswordCurrent> { key ->
          UserProfileCurrentPasswordView(
            passwordAction = key.action,
            onNext = { currentPassword ->
              backStack.add(
                UserProfileDestination.UpdatePasswordNew(
                  currentPassword = currentPassword,
                  passwordAction = key.action,
                )
              )
            },
          )
        }
        entry<UserProfileDestination.UpdatePasswordNew> { key ->
          UserProfileNewPasswordView(
            currentPassword = key.currentPassword,
            passwordAction = key.passwordAction,
            onSuccess = { backStack.pop(2) },
          )
        }
      },
  )
}

internal object UserProfileDestination {
  @Serializable data object UserProfileAccount : NavKey

  @Serializable data object UserProfile : NavKey

  @Serializable data object UserProfileSecurity : NavKey

  @Serializable data object UserProfileUpdate : NavKey

  @Serializable data class UpdatePasswordCurrent(val action: PasswordAction) : NavKey

  @Serializable
  data class UpdatePasswordNew(
    val currentPassword: String? = null,
    val passwordAction: PasswordAction,
  ) : NavKey
}
