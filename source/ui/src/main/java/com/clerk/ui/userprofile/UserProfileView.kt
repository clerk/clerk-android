package com.clerk.ui.userprofile

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.clerk.ui.userprofile.account.UserProfileAccountView
import com.clerk.ui.userprofile.account.UserProfileAction
import com.clerk.ui.userprofile.mfa.UserProfileAddMfaView
import com.clerk.ui.userprofile.mfa.ViewType
import com.clerk.ui.userprofile.phone.UserProfileAddPhoneView
import com.clerk.ui.userprofile.security.BackupCodesView
import com.clerk.ui.userprofile.security.MfaType
import com.clerk.ui.userprofile.security.UserProfileSecurityView
import com.clerk.ui.userprofile.security.passkey.rename.UserProfilePasskeyRenameView
import com.clerk.ui.userprofile.security.password.PasswordAction
import com.clerk.ui.userprofile.security.password.UserProfileCurrentPasswordView
import com.clerk.ui.userprofile.security.password.UserProfileNewPasswordView
import com.clerk.ui.userprofile.update.UserProfileUpdateProfileView
import com.clerk.ui.userprofile.verify.Mode
import com.clerk.ui.userprofile.verify.UserProfileVerifyView
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.Serializable

@SuppressLint("ComposeCompositionLocalUsage")
internal val LocalUserProfileState =
  staticCompositionLocalOf<UserProfileState> { error("No UserProfileState provided") }

@Composable
internal fun UserProfileStateProvider(
  backStack: NavBackStack<NavKey>,
  content: @Composable () -> Unit,
) {
  val userProfileState = UserProfileState(backStack = backStack)
  CompositionLocalProvider(LocalUserProfileState provides userProfileState) { content() }
}

@Composable
fun UserProfileView(modifier: Modifier = Modifier) {
  val backStack = rememberNavBackStack(UserProfileDestination.UserProfileAccount)
  UserProfileStateProvider(backStack) {
    val userProfileState = LocalUserProfileState.current
    NavDisplay(
      modifier = modifier,
      backStack = backStack,
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
              onBackPressed = { userProfileState.navigateBack() },
              onClickEdit = { backStack.add(UserProfileDestination.UserProfileUpdate) },
            )
          }
          entry<UserProfileDestination.UserProfile> { key -> }
          entry<UserProfileDestination.UserProfileSecurity> { key -> UserProfileSecurityView() }

          entry<UserProfileDestination.UserProfileUpdate> { key -> UserProfileUpdateProfileView() }
          entry<UserProfileDestination.UpdatePasswordCurrent> { key ->
            UserProfileCurrentPasswordView(passwordAction = key.action)
          }
          entry<UserProfileDestination.UpdatePasswordNew> { key ->
            UserProfileNewPasswordView(
              currentPassword = key.currentPassword,
              passwordAction = key.passwordAction,
            )
          }
          entry<UserProfileDestination.RenamePasskeyView> { key ->
            UserProfilePasskeyRenameView(passkeyId = key.passkeyId, passkeyName = key.passkeyName)
          }
          entry<UserProfileDestination.AddMfaView> { key ->
            UserProfileAddMfaView(viewType = key.viewType)
          }
          entry<UserProfileDestination.AddPhoneView> { key -> UserProfileAddPhoneView() }

          entry<UserProfileDestination.VerifyView> { key -> UserProfileVerifyView(mode = key.mode) }
          entry<UserProfileDestination.BackupCodeView> { key ->
            BackupCodesView(codes = key.codes.toImmutableList())
          }
        },
    )
  }
}

internal object UserProfileDestination {
  @Serializable data object UserProfileAccount : NavKey

  @Serializable data object UserProfile : NavKey

  @Serializable data object UserProfileSecurity : NavKey

  @Serializable data object UserProfileUpdate : NavKey

  @Serializable data class UpdatePasswordCurrent(val action: PasswordAction) : NavKey

  @Serializable
  data class RenamePasskeyView(val passkeyId: String, val passkeyName: String) : NavKey

  @Serializable
  data class UpdatePasswordNew(
    val currentPassword: String? = null,
    val passwordAction: PasswordAction,
  ) : NavKey

  @Serializable data class AddMfaView(val viewType: ViewType) : NavKey

  @Serializable data object AddPhoneView : NavKey

  @Serializable data class VerifyView(val mode: Mode) : NavKey

  @Serializable
  data class BackupCodeView(val mfaType: MfaType = MfaType.BackupCodes, val codes: List<String>) :
    NavKey
}
