package com.clerk.ui.organizationswitcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.SubcomposeAsyncImage
import com.clerk.api.organizations.OrganizationMembership
import com.clerk.ui.R
import com.clerk.ui.core.avatar.AvatarSize
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonConfiguration
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
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
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OrganizationSwitcherSheet(
  state: OrganizationSwitcherState,
  memberships: ImmutableList<OrganizationMembership>,
  activeOrganizationId: String?,
  onDismiss: () -> Unit,
  actions: OrganizationSwitcherSheetActions,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = ClerkMaterialTheme.colors.background,
    contentColor = ClerkMaterialTheme.colors.foreground,
  ) {
    OrganizationSwitcherSheetContent(
      state = state,
      memberships = memberships,
      activeOrganizationId = activeOrganizationId,
      actions = actions,
    )
  }
}

@Composable
internal fun OrganizationSwitcherSheetContent(
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
        SheetFooter(state = state, onLoadMore = actions.onLoadMore)
      }
    }
    Box(modifier = Modifier.align(Alignment.BottomCenter).padding(dp16)) {
      ClerkErrorSnackbar(snackbarHostState = snackbarHostState)
    }
  }
}

@Composable
private fun SheetFooter(state: OrganizationSwitcherState, onLoadMore: () -> Unit) {
  if (state.hasNextPage) {
    Box(modifier = Modifier.fillMaxWidth().padding(dp16)) {
      ClerkButton(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(R.string.load_more),
        isLoading = state.isLoadingMore,
        onClick = onLoadMore,
        configuration =
          ClerkButtonDefaults.configuration(
            style = ClerkButtonConfiguration.ButtonStyle.Secondary,
            emphasis = ClerkButtonConfiguration.Emphasis.High,
          ),
      )
    }
  } else {
    Spacer(modifier = Modifier.size(dp18))
  }
}

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
internal fun OrganizationAvatar(imageUrl: String?, shape: Shape, size: AvatarSize) {
  Surface(
    modifier = Modifier.size(size.toOrganizationSwitcherDp()),
    shape = shape,
    color = ClerkMaterialTheme.colors.muted,
    border = BorderStroke(dp1, ClerkMaterialTheme.computedColors.border),
  ) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      if (imageUrl.isNullOrBlank()) {
        OrganizationAvatarPlaceholder()
      } else {
        SubcomposeAsyncImage(
          model = imageUrl,
          contentDescription = stringResource(R.string.logo),
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop,
          loading = { OrganizationAvatarPlaceholder() },
          error = { OrganizationAvatarPlaceholder() },
        )
      }
    }
  }
}

@Composable
private fun OrganizationAvatarPlaceholder() {
  Icon(
    painter = painterResource(R.drawable.ic_organization),
    contentDescription = null,
    tint = ClerkMaterialTheme.colors.foreground,
  )
}

private fun AvatarSize.toOrganizationSwitcherDp() =
  when (this) {
    AvatarSize.SMALL -> dp24
    AvatarSize.MEDIUM -> dp36
    AvatarSize.LARGE -> dp48
    AvatarSize.X_LARGE -> dp96
  }
