package com.clerk.ui.userprofile

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.clerk.api.Clerk
import com.clerk.api.ui.ClerkTheme
import com.clerk.telemetry.TelemetryEvents
import com.clerk.ui.auth.AuthView
import com.clerk.ui.core.composition.LocalTelemetryCollector
import com.clerk.ui.core.composition.TelemetryProvider
import com.clerk.ui.theme.ClerkThemeOverrideProvider
import com.clerk.ui.userprofile.account.UserProfileAccountSwitcherSheet
import com.clerk.ui.userprofile.account.UserProfileAccountView
import com.clerk.ui.userprofile.account.UserProfileAction
import com.clerk.ui.userprofile.custom.CustomRouteNavKey
import com.clerk.ui.userprofile.custom.LocalUserProfileCustomNavigator
import com.clerk.ui.userprofile.custom.UserProfileCustomNavigator
import com.clerk.ui.userprofile.custom.UserProfileCustomRow
import com.clerk.ui.userprofile.custom.effectiveCustomRows
import com.clerk.ui.userprofile.detail.UserProfileDetailView
import com.clerk.ui.userprofile.security.MfaType
import com.clerk.ui.userprofile.security.Origin
import com.clerk.ui.userprofile.security.UserProfileSecurityView
import com.clerk.ui.userprofile.security.passkey.rename.UserProfilePasskeyRenameView
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
  TelemetryProvider {
    val userProfileState = UserProfileState(backStack = backStack)
    CompositionLocalProvider(LocalUserProfileState provides userProfileState) { content() }
  }
}

/**
 * User profile view for managing account settings, security, and profile information.
 *
 * Custom rows are inserted into the account screen based on their
 * [UserProfileCustomRowPlacement][com.clerk.ui.userprofile.custom.UserProfileCustomRowPlacement].
 * When tapped, the matching [customDestination] composable is rendered. Custom destinations
 * participate in the navigation back stack and survive activity recreation (e.g. rotation).
 *
 * @param clerkTheme Optional theme customization for the user profile UI.
 * @param customRows Custom rows to display on the profile account screen.
 * @param customDestination Composable that renders the destination for a given route key. The route
 *   key matches [UserProfileCustomRow.routeKey] of the tapped row.
 * @param onDismiss Callback when the user profile view is dismissed.
 */
@OptIn(ExperimentalAnimationApi::class)
@SuppressLint("ComposeUnstableReceiver")
@Suppress("LongMethod")
@Composable
fun UserProfileView(
  clerkTheme: ClerkTheme? = null,
  customRows: List<UserProfileCustomRow> = emptyList(),
  customDestination: (@Composable (String) -> Unit)? = null,
  onDismiss: () -> Unit = {},
) {
  ClerkThemeOverrideProvider(clerkTheme) {
    val backStack = rememberNavBackStack(UserProfileDestination.UserProfileAccount)
    val user by Clerk.userFlow.collectAsStateWithLifecycle()
    var showAccountSwitcher by rememberSaveable { mutableStateOf(false) }
    var showAuth by rememberSaveable { mutableStateOf(false) }
    UserProfileStateProvider(backStack) {
      val telemetry = LocalTelemetryCollector.current

      LaunchedEffect(Unit) { telemetry.record(TelemetryEvents.viewDidAppear("UserProfileView")) }
      LaunchedEffect(user?.id, showAuth) {
        if (user == null && !showAuth) {
          onDismiss()
        }
      }

      NavDisplay(
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
        entryProvider =
          entryProvider {
            userProfileEntries(
              backStack = backStack,
              onDismiss = onDismiss,
              customRows = customRows,
              customDestination = customDestination,
              onSwitchAccount = { showAccountSwitcher = true },
              onAddAccount = { showAuth = true },
            )
          },
      )

      if (showAccountSwitcher) {
        UserProfileAccountSwitcherSheet(
          onDismissRequest = { showAccountSwitcher = false },
          onAddAccount = { showAuth = true },
        )
      }

      if (showAuth) {
        AuthDialog(onDismiss = { showAuth = false })
      }
    }
  }
}

@Suppress("LongMethod", "LongParameterList")
private fun EntryProviderScope<NavKey>.userProfileEntries(
  backStack: NavBackStack<NavKey>,
  onDismiss: () -> Unit,
  customRows: List<UserProfileCustomRow>,
  customDestination: (@Composable (String) -> Unit)?,
  onSwitchAccount: () -> Unit,
  onAddAccount: () -> Unit,
) {
  entry<UserProfileDestination.UserProfileAccount> {
    UserProfileAccountView(
      onClick = {
        when (it) {
          UserProfileAction.Profile -> backStack.add(UserProfileDestination.UserProfileDetail)

          UserProfileAction.Security -> backStack.add(UserProfileDestination.UserProfileSecurity)

          UserProfileAction.SwitchAccount -> onSwitchAccount()

          UserProfileAction.AddAccount -> onAddAccount()

          UserProfileAction.SignOut -> Unit
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
      customRows =
        effectiveCustomRows(customRows, hasDestination = customDestination != null)
          .toImmutableList(),
      onCustomRowClick =
        if (customDestination != null) {
          { routeKey -> backStack.add(CustomRouteNavKey(routeKey)) }
        } else {
          {}
        },
    )
  }
  entry<UserProfileDestination.UserProfileSecurity> { UserProfileSecurityView() }

  entry<UserProfileDestination.UserProfileUpdate> { UserProfileUpdateProfileView() }

  entry<UserProfileDestination.RenamePasskeyView> { key ->
    UserProfilePasskeyRenameView(passkeyId = key.passkeyId, passkeyName = key.passkeyName)
  }

  entry<UserProfileDestination.VerifyView> { key -> UserProfileVerifyView(mode = key.mode) }

  entry<UserProfileDestination.UserProfileDetail> { UserProfileDetailView() }

  // Always register the entry so that a restored CustomRouteNavKey does not crash the graph.
  // If no destination is provided, pop back to the profile root.
  entry<CustomRouteNavKey> { key ->
    if (customDestination != null) {
      val navigator =
        remember(backStack) {
          UserProfileCustomNavigator(
            pushAction = { routeKey -> backStack.add(CustomRouteNavKey(routeKey)) },
            popToRootAction = {
              while (backStack.size > 1) {
                backStack.removeLastOrNull()
              }
            },
            navigateBackAction = { if (backStack.size > 1) backStack.removeLastOrNull() },
          )
        }
      CompositionLocalProvider(LocalUserProfileCustomNavigator provides navigator) {
        customDestination(key.routeKey)
      }
    } else {
      LaunchedEffect(Unit) { backStack.removeLastOrNull() }
    }
  }
}

@Composable
private fun AuthDialog(onDismiss: () -> Unit) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
  ) {
    AuthView(modifier = Modifier.fillMaxSize(), onAuthComplete = onDismiss)
  }
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
