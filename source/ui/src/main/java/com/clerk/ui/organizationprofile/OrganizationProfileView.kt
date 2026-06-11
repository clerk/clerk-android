@file:Suppress("LongParameterList")

package com.clerk.ui.organizationprofile

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.clerk.api.Clerk
import com.clerk.api.organizations.Organization
import com.clerk.api.organizations.OrganizationMembership
import com.clerk.api.ui.ClerkTheme
import com.clerk.telemetry.TelemetryEvents
import com.clerk.ui.core.composition.LocalTelemetryCollector
import com.clerk.ui.core.composition.TelemetryProvider
import com.clerk.ui.core.footer.DevelopmentModeWarningBox
import com.clerk.ui.organizationprofile.actions.OrganizationProfileActionConfirmationView
import com.clerk.ui.organizationprofile.actions.OrganizationProfileConfirmationAction
import com.clerk.ui.organizationprofile.custom.LocalOrganizationProfileCustomNavigator
import com.clerk.ui.organizationprofile.custom.OrganizationProfileCustomNavigator
import com.clerk.ui.organizationprofile.custom.OrganizationProfileCustomRouteNavKey
import com.clerk.ui.organizationprofile.custom.OrganizationProfileCustomRow
import com.clerk.ui.organizationprofile.custom.effectiveOrganizationProfileCustomRows
import com.clerk.ui.organizationprofile.domains.OrganizationVerifiedDomainsView
import com.clerk.ui.organizationprofile.invite.OrganizationInviteMembersView
import com.clerk.ui.organizationprofile.members.OrganizationMembersView
import com.clerk.ui.organizationprofile.root.OrganizationProfileAction
import com.clerk.ui.organizationprofile.root.OrganizationProfileRootView
import com.clerk.ui.organizationprofile.update.OrganizationProfileUpdateProfileView
import com.clerk.ui.theme.ClerkThemeOverrideProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.Serializable

/**
 * Organization profile view for managing the active organization.
 *
 * Custom rows are inserted into the root organization profile screen based on their placement; see
 * [com.clerk.ui.organizationprofile.custom.OrganizationProfileCustomRowPlacement]. When tapped, the
 * matching [customDestination] composable is rendered. Custom destinations participate in the
 * navigation back stack and survive activity recreation.
 *
 * @param clerkTheme Optional theme customization for the organization profile UI.
 * @param isDismissible Whether to show a top-level back affordance that calls [onDismiss].
 * @param customRows Custom rows to display on the profile root screen.
 * @param customDestination Composable that renders the destination for a given route key. The route
 *   key matches [OrganizationProfileCustomRow.routeKey] of the tapped row.
 * @param onDismiss Callback when the organization profile view is dismissed.
 * @param onComplete Callback when a destructive organization action completes and the profile can
 *   no longer be shown.
 */
@OptIn(ExperimentalAnimationApi::class)
@SuppressLint("ComposeUnstableReceiver")
@Composable
fun OrganizationProfileView(
  modifier: Modifier = Modifier,
  clerkTheme: ClerkTheme? = null,
  isDismissible: Boolean = true,
  customRows: List<OrganizationProfileCustomRow> = emptyList(),
  customDestination: (@Composable (String) -> Unit)? = null,
  onDismiss: () -> Unit = {},
  onComplete: () -> Unit = onDismiss,
) {
  ClerkThemeOverrideProvider(clerkTheme) {
    TelemetryProvider {
      val backStack = rememberNavBackStack(OrganizationProfileDestination.Root)
      val session by Clerk.sessionFlow.collectAsStateWithLifecycle()
      val user by Clerk.userFlow.collectAsStateWithLifecycle()
      val membership = Clerk.organizationMembership
      val organization = membership?.organization ?: Clerk.organization

      LaunchedEffect(Unit) { Clerk.refreshClient() }
      OrganizationProfileEffects(
        organizationId = organization?.id,
        onComplete = onComplete,
        hasOrganization = organization != null,
      )

      DevelopmentModeWarningBox(modifier = modifier.fillMaxSize()) {
        if (organization != null) {
          OrganizationProfileNavDisplay(
            modifier = Modifier.fillMaxSize(),
            backStack = backStack,
            organization = organization,
            membership = membership,
            isDismissible = isDismissible,
            customRows = customRows,
            customDestination = customDestination,
            onDismiss = onDismiss,
            onComplete = onComplete,
          )
        } else {
          Box(modifier = Modifier.fillMaxSize())
        }
      }

      LaunchedEffect(session?.id, user?.id) {
        if (Clerk.organizationMembership == null && Clerk.organization == null) onComplete()
      }
    }
  }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun OrganizationProfileNavDisplay(
  backStack: NavBackStack<NavKey>,
  organization: Organization,
  membership: OrganizationMembership?,
  isDismissible: Boolean,
  customRows: List<OrganizationProfileCustomRow>,
  customDestination: (@Composable (String) -> Unit)?,
  modifier: Modifier = Modifier,
  onDismiss: () -> Unit,
  onComplete: () -> Unit,
) {
  var membersRefreshKey by remember { mutableIntStateOf(0) }

  NavDisplay(
    modifier = modifier,
    backStack = backStack,
    onBack = { handleOrganizationProfileBack(backStack, isDismissible, onDismiss) },
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
      slideInHorizontally(initialOffsetX = { -distance }) togetherWith
        slideOutHorizontally(targetOffsetX = { distance })
    },
    entryProvider =
      entryProvider {
        organizationProfileEntries(
          backStack = backStack,
          organization = organization,
          membership = membership,
          isDismissible = isDismissible,
          onDismiss = onDismiss,
          membersRefreshKey = membersRefreshKey,
          onInviteMembersComplete = { membersRefreshKey += 1 },
          customRows = customRows,
          customDestination = customDestination,
          onComplete = onComplete,
        )
      },
  )
}

private fun handleOrganizationProfileBack(
  backStack: NavBackStack<NavKey>,
  isDismissible: Boolean,
  onDismiss: () -> Unit,
) {
  if (backStack.size == 1) {
    if (isDismissible) onDismiss()
  } else {
    backStack.removeLastOrNull()
  }
}

@Composable
private fun OrganizationProfileEffects(
  organizationId: String?,
  hasOrganization: Boolean,
  onComplete: () -> Unit,
) {
  val telemetry = LocalTelemetryCollector.current
  LaunchedEffect(organizationId) {
    if (hasOrganization) {
      telemetry.record(TelemetryEvents.viewDidAppear("OrganizationProfileView"))
    } else {
      onComplete()
    }
  }
}

@Suppress("LongMethod", "LongParameterList")
private fun EntryProviderScope<NavKey>.organizationProfileEntries(
  backStack: NavBackStack<NavKey>,
  organization: Organization,
  membership: OrganizationMembership?,
  isDismissible: Boolean,
  onDismiss: () -> Unit,
  membersRefreshKey: Int,
  onInviteMembersComplete: () -> Unit,
  customRows: List<OrganizationProfileCustomRow>,
  customDestination: (@Composable (String) -> Unit)?,
  onComplete: () -> Unit,
) {
  entry<OrganizationProfileDestination.Root> {
    OrganizationProfileRootView(
      organization = organization,
      membership = membership,
      isDismissible = isDismissible,
      onBackPressed = onDismiss,
      onUpdateProfile = { backStack.add(OrganizationProfileDestination.UpdateProfile) },
      onAction = { action -> backStack.add(action.destination) },
      customRows =
        effectiveOrganizationProfileCustomRows(
            customRows,
            hasDestination = customDestination != null,
          )
          .toImmutableList(),
      onCustomRowClick =
        if (customDestination != null) {
          { routeKey -> backStack.add(OrganizationProfileCustomRouteNavKey(routeKey)) }
        } else {
          {}
        },
    )
  }

  entry<OrganizationProfileDestination.Members> {
    OrganizationMembersView(
      organization = organization,
      membership = membership,
      onBackPressed = { backStack.removeLastOrNull() },
      refreshKey = membersRefreshKey,
      onInviteMembers = { backStack.add(OrganizationProfileDestination.InviteMembers) },
    )
  }

  entry<OrganizationProfileDestination.InviteMembers> {
    OrganizationInviteMembersView(
      organization = organization,
      onComplete = {
        onInviteMembersComplete()
        backStack.removeLastOrNull()
      },
    )
  }

  entry<OrganizationProfileDestination.VerifiedDomains> {
    OrganizationVerifiedDomainsView(
      organization = organization,
      membership = membership,
      onBackPressed = { backStack.removeLastOrNull() },
    )
  }

  entry<OrganizationProfileDestination.UpdateProfile> {
    OrganizationProfileUpdateProfileView(
      organization = organization,
      onBackPressed = { backStack.removeLastOrNull() },
    )
  }

  entry<OrganizationProfileDestination.LeaveOrganization> {
    OrganizationProfileActionConfirmationView(
      action = OrganizationProfileConfirmationAction.LeaveOrganization,
      organization = organization,
      membership = membership,
      onBackPressed = { backStack.removeLastOrNull() },
      onSuccess = onComplete,
    )
  }

  entry<OrganizationProfileDestination.DeleteOrganization> {
    OrganizationProfileActionConfirmationView(
      action = OrganizationProfileConfirmationAction.DeleteOrganization,
      organization = organization,
      membership = membership,
      onBackPressed = { backStack.removeLastOrNull() },
      onSuccess = onComplete,
    )
  }

  entry<OrganizationProfileCustomRouteNavKey> { key ->
    if (customDestination != null) {
      val navigator =
        remember(backStack) {
          OrganizationProfileCustomNavigator(
            pushAction = { routeKey ->
              backStack.add(OrganizationProfileCustomRouteNavKey(routeKey))
            },
            popToRootAction = {
              while (backStack.size > 1) {
                backStack.removeLastOrNull()
              }
            },
            navigateBackAction = { if (backStack.size > 1) backStack.removeLastOrNull() },
          )
        }
      CompositionLocalProvider(LocalOrganizationProfileCustomNavigator provides navigator) {
        customDestination(key.routeKey)
      }
    } else {
      LaunchedEffect(Unit) { backStack.removeLastOrNull() }
    }
  }
}

private val OrganizationProfileAction.destination: OrganizationProfileDestination
  get() =
    when (this) {
      OrganizationProfileAction.Members -> OrganizationProfileDestination.Members
      OrganizationProfileAction.VerifiedDomains -> OrganizationProfileDestination.VerifiedDomains
      OrganizationProfileAction.UpdateProfile -> OrganizationProfileDestination.UpdateProfile
      OrganizationProfileAction.LeaveOrganization ->
        OrganizationProfileDestination.LeaveOrganization
      OrganizationProfileAction.DeleteOrganization ->
        OrganizationProfileDestination.DeleteOrganization
    }

internal sealed interface OrganizationProfileDestination : NavKey {
  @Serializable data object Root : OrganizationProfileDestination

  @Serializable data object Members : OrganizationProfileDestination

  @Serializable data object InviteMembers : OrganizationProfileDestination

  @Serializable data object VerifiedDomains : OrganizationProfileDestination

  @Serializable data object UpdateProfile : OrganizationProfileDestination

  @Serializable data object LeaveOrganization : OrganizationProfileDestination

  @Serializable data object DeleteOrganization : OrganizationProfileDestination
}
