package com.clerk.ui.userprofile

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.clerk.api.Clerk
import com.clerk.api.ui.ClerkTheme
import com.clerk.telemetry.TelemetryEvents
import com.clerk.ui.auth.AuthView
import com.clerk.ui.core.composition.LocalTelemetryCollector
import com.clerk.ui.core.footer.DevelopmentModeWarningBox
import com.clerk.ui.theme.ClerkThemeOverrideProvider
import com.clerk.ui.userprofile.account.UserProfileAccountSwitcherSheet
import com.clerk.ui.userprofile.account.UserProfileAccountView
import com.clerk.ui.userprofile.account.UserProfileAction
import com.clerk.ui.userprofile.custom.CustomRouteNavKey
import com.clerk.ui.userprofile.custom.UserProfileCustomRow
import com.clerk.ui.userprofile.custom.effectiveCustomRows
import kotlinx.collections.immutable.toImmutableList

/**
 * Registers Clerk's user profile screens in a host-owned
 * [NavDisplay][androidx.navigation3.ui.NavDisplay], so the profile takes part in the host's own
 * navigation: its transitions, predictive back, and back stack.
 *
 * Push [ClerkUserProfileRoute] onto [backStack] to open the profile. Clerk pushes its own internal
 * keys above it as the user navigates, and removes its keys (never the host's) when the flow ends.
 *
 * ```kotlin
 * val backStack = rememberNavBackStack(Home)
 *
 * NavDisplay(
 *   backStack = backStack,
 *   entryProvider =
 *     entryProvider {
 *       entry<Home> { HomeScreen(onOpenProfile = { backStack.add(ClerkUserProfileRoute) }) }
 *       clerkUserProfileEntries(backStack)
 *     },
 * )
 * ```
 *
 * This is the host-owned counterpart of [UserProfileView], which renders a self-contained profile
 * with its own navigation container.
 *
 * @param backStack The host's back stack, which must be the one rendered by the `NavDisplay` these
 *   entries are registered in.
 * @param clerkTheme Optional theme customization for the user profile UI.
 * @param customRows Custom rows to display on the profile account screen.
 * @param customDestination Composable that renders the destination for a given route key. The route
 *   key matches [UserProfileCustomRow.routeKey] of the tapped row.
 * @param onAddAccount Optional callback for hosting the add-account auth flow outside the profile.
 */
@Suppress("LongParameterList")
fun EntryProviderScope<NavKey>.clerkUserProfileEntries(
  backStack: NavBackStack<NavKey>,
  clerkTheme: ClerkTheme? = null,
  customRows: List<UserProfileCustomRow> = emptyList(),
  customDestination: (@Composable (String) -> Unit)? = null,
  onAddAccount: (() -> Unit)? = null,
) {
  val exitProfile = { backStack.exitClerkUserProfile() }
  val decorate: @Composable (@Composable () -> Unit) -> Unit = { content ->
    HostEntryChrome(
      clerkTheme = clerkTheme,
      backStack = backStack,
      onClearBackStack = exitProfile,
      content = content,
    )
  }

  entry<ClerkUserProfileRoute> {
    decorate {
      UserProfileRootEntry(
        backStack = backStack,
        customRows = customRows,
        customDestination = customDestination,
        onAddAccount = onAddAccount,
        onExit = exitProfile,
      )
    }
  }

  userProfileChildEntries(
    backStack = backStack,
    customDestination = customDestination,
    popToRoot = { backStack.popToClerkUserProfileRoot() },
    navigateBack = {
      if (backStack.lastOrNull() != ClerkUserProfileRoute) backStack.removeLastOrNull()
    },
    decorate = decorate,
  )
}

/** Applies per-entry the providers the self-contained profile applies around its NavDisplay. */
@Composable
private fun HostEntryChrome(
  clerkTheme: ClerkTheme?,
  backStack: NavBackStack<NavKey>,
  onClearBackStack: () -> Unit,
  content: @Composable () -> Unit,
) {
  ClerkThemeOverrideProvider(clerkTheme) {
    UserProfileStateProvider(backStack, onClearBackStack = onClearBackStack) {
      DevelopmentModeWarningBox(modifier = Modifier.fillMaxSize()) { content() }
    }
  }
}

@Suppress("LongMethod")
@Composable
private fun UserProfileRootEntry(
  backStack: NavBackStack<NavKey>,
  customRows: List<UserProfileCustomRow>,
  customDestination: (@Composable (String) -> Unit)?,
  onAddAccount: (() -> Unit)?,
  onExit: () -> Unit,
) {
  val telemetry = LocalTelemetryCollector.current
  val user by Clerk.userFlow.collectAsStateWithLifecycle()
  var showAccountSwitcher by rememberSaveable { mutableStateOf(false) }
  var showAuth by rememberSaveable { mutableStateOf(false) }
  val showAddAccountAuth = {
    showAccountSwitcher = false
    onAddAccount?.invoke() ?: run { showAuth = true }
  }

  LaunchedEffect(Unit) {
    telemetry.record(TelemetryEvents.viewDidAppear("UserProfileView"))
    Clerk.refreshClient()
  }
  LaunchedEffect(user?.id, showAuth) {
    if (user == null && !showAuth) {
      onExit()
    }
  }

  if (showAuth) {
    // The add-account flow replaces the profile entry entirely, so it keeps Clerk's own
    // navigation chrome. Hosts that want to own this flow can pass onAddAccount instead.
    AuthView(
      modifier = Modifier.fillMaxSize(),
      preferGoogleOneTap = false,
      onDismiss = { showAuth = false },
      onAuthComplete = { showAuth = false },
    )
  } else {
    UserProfileAccountView(
      onClick = {
        when (it) {
          UserProfileAction.Profile -> backStack.add(UserProfileDestination.UserProfileDetail)

          UserProfileAction.Security -> backStack.add(UserProfileDestination.UserProfileSecurity)

          UserProfileAction.SwitchAccount -> showAccountSwitcher = true

          UserProfileAction.AddAccount -> showAddAccountAuth()

          UserProfileAction.SignOut -> Unit
        }
      },
      onBackPressed = onExit,
      isDismissible = false,
      showsBackButton = true,
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

    if (showAccountSwitcher) {
      UserProfileAccountSwitcherSheet(
        onDismissRequest = { showAccountSwitcher = false },
        onAddAccount = showAddAccountAuth,
      )
    }
  }
}

/** Removes the profile's keys from the host stack, including [ClerkUserProfileRoute]. */
private fun NavBackStack<NavKey>.exitClerkUserProfile() {
  val rootIndex = indexOfLast { it == ClerkUserProfileRoute }
  if (rootIndex == -1) return
  while (size > rootIndex) {
    removeLastOrNull()
  }
}

/** Pops the profile's pushed keys so [ClerkUserProfileRoute] is back on top. */
private fun NavBackStack<NavKey>.popToClerkUserProfileRoot() {
  val rootIndex = indexOfLast { it == ClerkUserProfileRoute }
  if (rootIndex == -1) return
  while (size - 1 > rootIndex) {
    removeLastOrNull()
  }
}
