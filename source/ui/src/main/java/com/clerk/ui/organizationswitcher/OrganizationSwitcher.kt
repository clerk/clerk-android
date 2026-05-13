package com.clerk.ui.organizationswitcher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.Clerk
import com.clerk.api.organizations.OrganizationMembership
import com.clerk.api.ui.ClerkTheme
import com.clerk.telemetry.TelemetryEvents
import com.clerk.ui.R
import com.clerk.ui.core.avatar.AvatarSize
import com.clerk.ui.core.composition.LocalTelemetryCollector
import com.clerk.ui.core.composition.TelemetryProvider
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.core.dimens.dp2
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.ClerkThemeOverrideProvider
import com.clerk.ui.userbutton.UserButton
import kotlinx.collections.immutable.toImmutableList

/**
 * Self-contained active organization switcher.
 *
 * Drop this into a top app bar or page header. When the signed-in user has organization
 * memberships, it renders the active organization and opens a membership picker for switching the
 * active organization on the current session.
 *
 * @param clerkTheme Optional theme customization for the switcher UI.
 * @param showUserButton When `true` (default), render [UserButton] on the trailing edge to match
 *   Clerk's mobile prebuilt header pattern.
 * @param onOrganizationChanged Optional callback invoked after a successful organization switch.
 */
@Composable
fun OrganizationSwitcher(
  modifier: Modifier = Modifier,
  clerkTheme: ClerkTheme? = null,
  showUserButton: Boolean = true,
  onOrganizationChanged: (() -> Unit)? = null,
) {
  ClerkThemeOverrideProvider(clerkTheme) {
    ClerkMaterialTheme {
      TelemetryProvider {
        OrganizationSwitcherImpl(
          modifier = modifier,
          clerkTheme = clerkTheme,
          showUserButton = showUserButton,
          onOrganizationChanged = onOrganizationChanged,
        )
      }
    }
  }
}

@Composable
internal fun OrganizationSwitcherImpl(
  modifier: Modifier = Modifier,
  clerkTheme: ClerkTheme? = null,
  showUserButton: Boolean = true,
  onOrganizationChanged: (() -> Unit)? = null,
  viewModel: OrganizationSwitcherViewModel = viewModel(),
) {
  val session by Clerk.sessionFlow.collectAsStateWithLifecycle()
  val user by Clerk.userFlow.collectAsStateWithLifecycle()
  val state by viewModel.state.collectAsStateWithLifecycle()
  var showSheet by rememberSaveable { mutableStateOf(false) }

  val userMemberships = user?.organizationMemberships.orEmpty()
  val memberships =
    remember(state.memberships, userMemberships) {
      state.memberships.ifEmpty { userMemberships }.toImmutableList()
    }
  val activeMembership = activeOrganizationMembership(user, session, state.memberships)
  val telemetry = LocalTelemetryCollector.current

  LaunchedEffect(user?.id, session?.id) {
    viewModel.reset()
    viewModel.load(user)
  }
  LaunchedEffect(user?.id) {
    if (user != null) telemetry.record(TelemetryEvents.viewDidAppear("OrganizationSwitcher"))
  }

  val shouldShow =
    shouldShowOrganizationSwitcher(
      hasUser = user != null,
      hasSession = session != null,
      hasMemberships = memberships.isNotEmpty(),
    )

  if (shouldShow) {
    OrganizationSwitcherHeader(
      modifier = modifier,
      activeMembership = activeMembership,
      isLoading = state.isLoading && activeMembership == null,
      showUserButton = showUserButton,
      clerkTheme = clerkTheme,
      onClick = { showSheet = true },
    )
  }

  if (showSheet) {
    OrganizationSwitcherSheet(
      state = state,
      memberships = memberships,
      activeOrganizationId = session?.lastActiveOrganizationId,
      onDismiss = { showSheet = false },
      actions =
        OrganizationSwitcherSheetActions(
          onLoadMore = { viewModel.loadMore(user) },
          onSelect = { organizationId ->
            viewModel.selectOrganization(session = session, organizationId = organizationId) {
              showSheet = false
              onOrganizationChanged?.invoke()
            }
          },
          onErrorShown = viewModel::clearError,
        ),
    )
  }
}

@Composable
private fun OrganizationSwitcherHeader(
  activeMembership: OrganizationMembership?,
  isLoading: Boolean,
  showUserButton: Boolean,
  clerkTheme: ClerkTheme?,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(dp12),
  ) {
    OrganizationSwitcherButton(
      modifier = Modifier.weight(1f),
      membership = activeMembership,
      isLoading = isLoading,
      onClick = onClick,
    )
    if (showUserButton) {
      UserButton(clerkTheme = clerkTheme)
    }
  }
}

@Composable
internal fun OrganizationSwitcherButton(
  membership: OrganizationMembership?,
  isLoading: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val openOrganizationSwitcherDescription = stringResource(R.string.open_organization_switcher)
  Row(
    modifier =
      modifier.clickable(enabled = !isLoading, onClick = onClick).semantics {
        contentDescription = openOrganizationSwitcherDescription
      },
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(dp12),
  ) {
    OrganizationAvatar(
      imageUrl = membership?.organization?.imageUrl,
      shape = ClerkMaterialTheme.shape,
      size = AvatarSize.MEDIUM,
    )
    Text(
      modifier = Modifier.weight(1f, fill = false),
      text = membership?.organization?.name ?: stringResource(R.string.select_organization),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      style =
        ClerkMaterialTheme.typography.headlineLarge.copy(
          fontSize = 34.sp,
          lineHeight = 41.sp,
          letterSpacing = 0.4.sp,
          fontWeight = FontWeight.Bold,
        ),
      color = ClerkMaterialTheme.colors.foreground,
    )
    if (isLoading) {
      CircularProgressIndicator(modifier = Modifier.size(dp24), strokeWidth = dp2)
    } else {
      Icon(
        modifier = Modifier.size(dp24),
        painter = painterResource(R.drawable.ic_chevron_down),
        contentDescription = null,
        tint = ClerkMaterialTheme.colors.mutedForeground,
      )
    }
  }
}

internal data class OrganizationSwitcherSheetActions(
  val onLoadMore: () -> Unit,
  val onSelect: (String) -> Unit,
  val onErrorShown: () -> Unit,
)
