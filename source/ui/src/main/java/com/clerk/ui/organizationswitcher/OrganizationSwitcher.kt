@file:Suppress("LongMethod")

package com.clerk.ui.organizationswitcher

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.Clerk
import com.clerk.api.organizations.Organization
import com.clerk.api.organizations.OrganizationMembership
import com.clerk.api.ui.ClerkTheme
import com.clerk.telemetry.TelemetryEvents
import com.clerk.ui.R
import com.clerk.ui.core.avatar.AvatarSize
import com.clerk.ui.core.avatar.AvatarType
import com.clerk.ui.core.avatar.AvatarView
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonConfiguration
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
import com.clerk.ui.core.composition.LocalTelemetryCollector
import com.clerk.ui.core.composition.TelemetryProvider
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.core.dimens.dp14
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp18
import com.clerk.ui.core.dimens.dp2
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp36
import com.clerk.ui.core.dimens.dp4
import com.clerk.ui.core.dimens.dp48
import com.clerk.ui.core.dimens.dp96
import com.clerk.ui.core.error.ClerkErrorSnackbar
import com.clerk.ui.core.extensions.withMediumWeight
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.ClerkThemeOverrideProvider
import com.clerk.ui.theme.DefaultColors
import com.clerk.ui.userbutton.UserButton
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.json.JsonNull

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
@SuppressLint("ComposeViewModelInjection")
@OptIn(ExperimentalMaterial3Api::class)
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
        val session by Clerk.sessionFlow.collectAsStateWithLifecycle()
        val user by Clerk.userFlow.collectAsStateWithLifecycle()
        val viewModel: OrganizationSwitcherViewModel = viewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        var showSheet by rememberSaveable { mutableStateOf(false) }
        val userMemberships = user?.organizationMemberships.orEmpty()
        val memberships = state.memberships.ifEmpty { userMemberships }
        val activeMembership = activeOrganizationMembership(user, session, state.memberships)
        val telemetry = LocalTelemetryCollector.current
        val shouldShow =
          shouldShowOrganizationSwitcher(
            hasUser = user != null,
            hasSession = session != null,
            hasMemberships = memberships.isNotEmpty(),
          )

        LaunchedEffect(user?.id, session?.id) {
          viewModel.reset()
          viewModel.load(user)
        }
        LaunchedEffect(user?.id) {
          if (user != null) telemetry.record(TelemetryEvents.viewDidAppear("OrganizationSwitcher"))
        }

        if (shouldShow) {
          Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dp12),
          ) {
            OrganizationSwitcherButton(
              modifier = Modifier.weight(1f),
              membership = activeMembership,
              isLoading = state.isLoading && activeMembership == null,
              onClick = { showSheet = true },
            )
            if (showUserButton) {
              UserButton(clerkTheme = clerkTheme)
            }
          }
        }

        if (showSheet) {
          val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
          ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = ClerkMaterialTheme.colors.background,
            contentColor = ClerkMaterialTheme.colors.foreground,
          ) {
            OrganizationSwitcherSheetContent(
              state = state,
              memberships = memberships.toImmutableList(),
              activeOrganizationId = session?.lastActiveOrganizationId,
              actions =
                OrganizationSwitcherSheetActions(
                  onLoadMore = { viewModel.loadMore(user) },
                  onSelect = { organizationId ->
                    viewModel.selectOrganization(
                      session = session,
                      organizationId = organizationId,
                    ) {
                      showSheet = false
                      onOrganizationChanged?.invoke()
                    }
                  },
                  onErrorShown = viewModel::clearError,
                ),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun OrganizationSwitcherButton(
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

@Composable
private fun OrganizationSwitcherSheetContent(
  state: OrganizationSwitcherState,
  memberships: ImmutableList<OrganizationMembership>,
  activeOrganizationId: String?,
  actions: OrganizationSwitcherSheetActions,
) {
  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(state.errorMessage) {
    state.errorMessage?.let { errorMessage ->
      snackbarHostState.showSnackbar(errorMessage)
      actions.onErrorShown()
    }
  }

  Box(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Text(
        modifier = Modifier.padding(horizontal = dp24, vertical = dp16),
        text = stringResource(R.string.switch_organization),
        style = ClerkMaterialTheme.typography.titleMedium.withMediumWeight(),
        color = ClerkMaterialTheme.colors.foreground,
      )
      HorizontalDivider(color = ClerkMaterialTheme.computedColors.border)

      if (state.isLoading && memberships.isEmpty()) {
        Box(
          modifier = Modifier.fillMaxWidth().padding(vertical = dp24),
          contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator()
        }
      } else {
        organizationSwitcherMemberships(memberships, activeOrganizationId).forEach { membership ->
          OrganizationMembershipRow(
            membership = membership,
            isCurrent = membership.organization.id == activeOrganizationId,
            isLoading = state.activeActionId == membership.organization.id,
            enabled = state.activeActionId == null,
            onClick = { actions.onSelect(membership.organization.id) },
          )
          HorizontalDivider(color = ClerkMaterialTheme.computedColors.border)
        }

        if (state.hasNextPage) {
          ClerkButton(
            modifier = Modifier.fillMaxWidth().padding(dp16),
            text = stringResource(R.string.load_more),
            isLoading = state.isLoadingMore,
            onClick = actions.onLoadMore,
            configuration =
              ClerkButtonDefaults.configuration(
                style = ClerkButtonConfiguration.ButtonStyle.Secondary,
                emphasis = ClerkButtonConfiguration.Emphasis.High,
              ),
          )
        } else {
          Spacer(modifier = Modifier.size(dp18))
        }
      }
    }

    Box(modifier = Modifier.align(Alignment.BottomCenter).padding(dp16)) {
      ClerkErrorSnackbar(snackbarHostState = snackbarHostState)
    }
  }
}

private data class OrganizationSwitcherSheetActions(
  val onLoadMore: () -> Unit,
  val onSelect: (String) -> Unit,
  val onErrorShown: () -> Unit,
)

@Composable
private fun OrganizationMembershipRow(
  membership: OrganizationMembership,
  isCurrent: Boolean,
  isLoading: Boolean,
  enabled: Boolean,
  onClick: () -> Unit,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clickable(enabled = enabled && !isCurrent && !isLoading, onClick = onClick)
        .padding(horizontal = dp24, vertical = dp14),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(dp12),
  ) {
    OrganizationAvatar(
      imageUrl = membership.organization.imageUrl,
      shape = ClerkMaterialTheme.shape,
      size = AvatarSize.LARGE,
    )
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dp4)) {
      Text(
        text = membership.organization.name,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = ClerkMaterialTheme.typography.bodyLarge.withMediumWeight(),
        color = ClerkMaterialTheme.colors.foreground,
      )
      Text(
        text = membership.roleName,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = ClerkMaterialTheme.typography.bodySmall,
        color = ClerkMaterialTheme.colors.mutedForeground,
      )
    }
    when {
      isLoading -> CircularProgressIndicator(modifier = Modifier.size(dp24), strokeWidth = dp2)
      isCurrent ->
        Icon(
          modifier = Modifier.size(dp24),
          painter = painterResource(R.drawable.ic_check),
          contentDescription = stringResource(R.string.current_organization),
          tint = ClerkMaterialTheme.colors.foreground,
        )
      else ->
        Icon(
          modifier = Modifier.size(dp24),
          painter = painterResource(R.drawable.ic_chevron_right),
          contentDescription = null,
          tint = ClerkMaterialTheme.colors.mutedForeground,
        )
    }
  }
}

@Composable
private fun OrganizationAvatar(imageUrl: String?, shape: Shape, size: AvatarSize) {
  Surface(
    modifier = Modifier.size(size.toOrganizationSwitcherDp()),
    shape = shape,
    color = ClerkMaterialTheme.colors.muted,
    border = BorderStroke(dp1, ClerkMaterialTheme.computedColors.border),
  ) {
    AvatarView(
      imageUrl = imageUrl,
      size = size,
      shape = shape,
      avatarType = AvatarType.ORGANIZATION,
    )
  }
}

private fun AvatarSize.toOrganizationSwitcherDp() =
  when (this) {
    AvatarSize.SMALL -> dp24
    AvatarSize.MEDIUM -> dp36
    AvatarSize.LARGE -> dp48
    AvatarSize.X_LARGE -> dp96
  }

@PreviewLightDark
@Composable
private fun OrganizationSwitcherButtonPreview() {
  OrganizationSwitcherPreviewTheme {
    OrganizationSwitcherButton(
      modifier = Modifier.fillMaxWidth().padding(dp24),
      membership = previewOrganizationMembership(),
      isLoading = false,
      onClick = {},
    )
  }
}

@PreviewLightDark
@Composable
private fun OrganizationSwitcherSheetContentPreview() {
  val memberships =
    listOf(
        previewOrganizationMembership(
          organizationId = "org_acme",
          organizationName = "Acme Inc.",
          roleName = "Admin",
        ),
        previewOrganizationMembership(
          organizationId = "org_mosaic",
          organizationName = "Mosaic Labs",
          roleName = "Member",
        ),
        previewOrganizationMembership(
          organizationId = "org_clerk",
          organizationName = "Clerk",
          roleName = "Owner",
        ),
      )
      .toImmutableList()

  OrganizationSwitcherPreviewTheme {
    OrganizationSwitcherSheetContent(
      state = OrganizationSwitcherState(membershipsTotalCount = memberships.size),
      memberships = memberships,
      activeOrganizationId = "org_acme",
      actions = OrganizationSwitcherSheetActions(onLoadMore = {}, onSelect = {}, onErrorShown = {}),
    )
  }
}

@Composable
private fun OrganizationSwitcherPreviewTheme(content: @Composable () -> Unit) {
  ClerkMaterialTheme(clerkTheme = ClerkTheme(colors = DefaultColors.clerk)) {
    Box(modifier = Modifier.fillMaxWidth().background(ClerkMaterialTheme.colors.background)) {
      content()
    }
  }
}

private fun previewOrganizationMembership(
  organizationId: String = "org_acme",
  organizationName: String = "Acme Inc.",
  roleName: String = "Admin",
): OrganizationMembership {
  return OrganizationMembership(
    id = "mem_$organizationId",
    publicMetadata = JsonNull,
    role = "org:${roleName.lowercase()}",
    roleName = roleName,
    organization =
      Organization(
        id = organizationId,
        name = organizationName,
        slug = organizationName.lowercase().replace(" ", "-"),
        imageUrl = "",
        maxAllowedMemberships = 0,
        adminDeleteEnabled = true,
        createdAt = 1,
        updatedAt = 1,
        publicMetadata = JsonNull,
      ),
    createdAt = 1,
    updatedAt = 1,
  )
}
