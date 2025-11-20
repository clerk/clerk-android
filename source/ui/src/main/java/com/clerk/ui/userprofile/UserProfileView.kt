package com.clerk.ui.userprofile

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.clerk.api.ui.ClerkTheme
import com.clerk.telemetry.TelemetryEvents
import com.clerk.ui.auth.LocalTelemetryCollector
import com.clerk.ui.auth.TelemetryProvider
import com.clerk.ui.userprofile.account.UserProfileAccountView
import com.clerk.ui.userprofile.account.UserProfileAction
import com.clerk.ui.userprofile.detail.UserProfileDetailView
import com.clerk.ui.userprofile.security.MfaType
import com.clerk.ui.userprofile.security.Origin
import com.clerk.ui.userprofile.security.UserProfileSecurityView
import com.clerk.ui.userprofile.security.passkey.rename.UserProfilePasskeyRenameView
import com.clerk.ui.userprofile.update.UserProfileUpdateProfileView
import com.clerk.ui.userprofile.verify.Mode
import com.clerk.ui.userprofile.verify.UserProfileVerifyView
import com.clerk.ui.theme.ClerkThemeOverrideProvider
import kotlinx.serialization.Serializable

@SuppressLint("ComposeCompositionLocalUsage")
internal val LocalUserProfileState =
  staticCompositionLocalOf<UserProfileState> { error("No UserProfileState provided") }

@Composable
internal fun UserProfileStateProvider(
  backStack: NavBackStack<NavKey>,
  content: @Composable () -> Unit,
) {
  TelemetryProvider {
    val userProfileState = UserProfileState(backStack = backStack)
    CompositionLocalProvider(LocalUserProfileState provides userProfileState) { content() }
  }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun UserProfileView(modifier: Modifier = Modifier, clerkTheme: ClerkTheme? = null, onDismiss: () -> Unit) {
  ClerkThemeOverrideProvider(clerkTheme) {
    val backStack = rememberNavBackStack(UserProfileDestination.UserProfileAccount)
    UserProfileStateProvider(backStack) {
      val telemetry = LocalTelemetryCollector.current

      LaunchedEffect(Unit) { telemetry.record(TelemetryEvents.viewDidAppear("UserProfileView")) }

      NavDisplay(
        modifier = modifier,
        backStack = backStack,
        onBack = {
          if (backStack.size == 1) {
            onDismiss()
          } else {
            backStack.removeLastOrNull()
          }
        },
        transitionSpec = {
          val spec = tween<IntOffset>(durationMillis = 300)
          slideInHorizontally(animationSpec = spec, initialOffsetX = { it }) togetherWith
            slideOutHorizontally(animationSpec = spec, targetOffsetX = { -it })
        },
        popTransitionSpec = {
          val spec = tween<IntOffset>(durationMillis = 300)
          slideInHorizontally(animationSpec = spec, initialOffsetX = { -it }) togetherWith
            slideOutHorizontally(animationSpec = spec, targetOffsetX = { it })
        },
        predictivePopTransitionSpec = { distance ->
          // Use the provided distance to align with the system back gesture
          slideInHorizontally(initialOffsetX = { -distance }) togetherWith
            slideOutHorizontally(targetOffsetX = { distance })
        },
        entryProvider = entryProvider { UserProfileEntries(backStack, onDismiss) },
      )
    }
  }
}

@Composable
private fun EntryProviderScope<NavKey>.UserProfileEntries(
  backStack: NavBackStack<NavKey>,
  onDismiss: () -> Unit,
) {
  entry<UserProfileDestination.UserProfileAccount> {
    UserProfileAccountView(
      onClick = {
        when (it) {
          UserProfileAction.Profile -> backStack.add(UserProfileDestination.UserProfileDetail)

          UserProfileAction.Security -> backStack.add(UserProfileDestination.UserProfileSecurity)
        }
      },
      onBackPressed = {
        if (backStack.size == 1) {
          onDismiss()
        } else {
          backStack.removeLastOrNull()
        }
      },
      onClickEdit = { backStack.add(UserProfileDestination.UserProfileUpdate) },
    )
  }
  entry<UserProfileDestination.UserProfileSecurity> { UserProfileSecurityView() }

  entry<UserProfileDestination.UserProfileUpdate> { UserProfileUpdateProfileView() }

  entry<UserProfileDestination.RenamePasskeyView> { key ->
    UserProfilePasskeyRenameView(passkeyId = key.passkeyId, passkeyName = key.passkeyName)
  }

  entry<UserProfileDestination.VerifyView> { key -> UserProfileVerifyView(mode = key.mode) }

  entry<UserProfileDestination.UserProfileDetail> { UserProfileDetailView() }
}

internal object UserProfileDestination {
  @Serializable data object UserProfileAccount : NavKey

  @Serializable data object UserProfileSecurity : NavKey

  @Serializable data object UserProfileUpdate : NavKey

  @Serializable
  data class RenamePasskeyView(val passkeyId: String, val passkeyName: String) : NavKey

  @Serializable data class VerifyView(val mode: Mode) : NavKey

  @Serializable
  data class BackupCodeView(
    val origin: Origin = Origin.BackupCodes,
    val mfaType: MfaType = MfaType.BackupCodes,
    val codes: List<String>,
  ) : NavKey

  @Serializable data object UserProfileDetail : NavKey
}
