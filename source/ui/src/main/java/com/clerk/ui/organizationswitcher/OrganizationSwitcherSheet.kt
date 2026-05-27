@file:Suppress("LongParameterList")

package com.clerk.ui.organizationswitcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.clerk.api.organizations.OrganizationMembership
import com.clerk.api.user.User
import com.clerk.ui.R
import com.clerk.ui.core.avatar.AvatarSize
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp18
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp32
import com.clerk.ui.core.dimens.dp36
import com.clerk.ui.core.dimens.dp48
import com.clerk.ui.core.dimens.dp96
import com.clerk.ui.core.error.ClerkErrorSnackbar
import com.clerk.ui.core.extensions.withMediumWeight
import com.clerk.ui.organizationlist.OrganizationAccountListActions
import com.clerk.ui.organizationlist.OrganizationAccountListContent
import com.clerk.ui.organizationlist.OrganizationAccountListState
import com.clerk.ui.theme.ClerkMaterialTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OrganizationSwitcherModalSheet(
  destination: OrganizationSwitcherSheetDestination,
  state: OrganizationAccountListState,
  user: User?,
  activeMembership: OrganizationMembership?,
  activeOrganizationId: String?,
  showPersonalAccount: Boolean,
  showCreateOrganization: Boolean,
  onDismiss: () -> Unit,
  onShowAccountList: () -> Unit,
  onManageOrganization: ((OrganizationMembership) -> Unit)?,
  onErrorShown: () -> Unit,
  actions: OrganizationAccountListActions,
  modifier: Modifier = Modifier,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(
    modifier = modifier,
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = ClerkMaterialTheme.colors.background,
    contentColor = ClerkMaterialTheme.colors.foreground,
  ) {
    when (destination) {
      OrganizationSwitcherSheetDestination.Overview -> {
        if (activeMembership == null) {
          OrganizationSwitcherAccountListSheetContent(
            state = state,
            user = user,
            activeOrganizationId = activeOrganizationId,
            showPersonalAccount = showPersonalAccount,
            showCreateOrganization = showCreateOrganization,
            actions = actions,
            onErrorShown = onErrorShown,
          )
        } else {
          OrganizationSwitcherOverviewSheetContent(
            membership = activeMembership,
            onManageOrganization =
              onManageOrganization?.let { manageOrganization ->
                { manageOrganization(activeMembership) }
              },
            onSwitchAccount = onShowAccountList,
          )
        }
      }
      OrganizationSwitcherSheetDestination.AccountList ->
        OrganizationSwitcherAccountListSheetContent(
          state = state,
          user = user,
          activeOrganizationId = activeOrganizationId,
          showPersonalAccount = showPersonalAccount,
          showCreateOrganization = showCreateOrganization,
          actions = actions,
          onErrorShown = onErrorShown,
        )
    }
  }
}

@Composable
internal fun OrganizationSwitcherOverviewSheetContent(
  membership: OrganizationMembership,
  onManageOrganization: (() -> Unit)?,
  onSwitchAccount: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth().padding(horizontal = dp24),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Spacer(modifier = Modifier.size(dp16))
    OrganizationAvatar(
      imageUrl = membership.organization.imageUrl,
      shape = ClerkMaterialTheme.shape,
      size = AvatarSize.X_LARGE,
    )
    Spacer(modifier = Modifier.size(dp16))
    Text(
      text = membership.organization.name,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      style = ClerkMaterialTheme.typography.titleLarge.withMediumWeight(),
      color = ClerkMaterialTheme.colors.foreground,
    )
    Text(
      text = membership.roleName,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      style = ClerkMaterialTheme.typography.bodyMedium,
      color = ClerkMaterialTheme.colors.mutedForeground,
    )
    Spacer(modifier = Modifier.size(dp24))
    onManageOrganization?.let {
      OrganizationSwitcherActionRow(
        text = stringResource(R.string.manage_organization),
        icon = R.drawable.ic_cog,
        onClick = it,
      )
      Spacer(modifier = Modifier.size(dp12))
    }
    OrganizationSwitcherActionRow(
      text = stringResource(R.string.switch_account),
      icon = R.drawable.ic_switch,
      onClick = onSwitchAccount,
    )
    Spacer(modifier = Modifier.size(dp18))
  }
}

@Composable
internal fun OrganizationSwitcherAccountListSheetContent(
  state: OrganizationAccountListState,
  user: User?,
  activeOrganizationId: String?,
  showPersonalAccount: Boolean,
  showCreateOrganization: Boolean,
  actions: OrganizationAccountListActions,
  onErrorShown: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(state.errorMessage) {
    state.errorMessage?.let { errorMessage ->
      snackbarHostState.showSnackbar(errorMessage)
      onErrorShown()
    }
  }

  Box(modifier = modifier.fillMaxWidth()) {
    Column(modifier = Modifier.fillMaxWidth()) {
      SheetTitle(text = stringResource(R.string.switch_account))
      HorizontalDivider(color = ClerkMaterialTheme.computedColors.border)
      if (state.isLoading && !state.hasLoadedInitialResources) {
        Box(
          modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp).padding(vertical = dp24),
          contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator()
        }
      } else {
        OrganizationAccountListContent(
          modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
          state = state,
          user = user,
          activeOrganizationId = activeOrganizationId,
          header = null,
          showPersonalAccount = showPersonalAccount,
          showSelectedAccessory = true,
          actions = actions,
          contentPadding = PaddingValues(horizontal = dp16, vertical = dp16),
          showSecuredByClerk = false,
          showCreateOrganization = showCreateOrganization,
        )
      }
    }
    Box(modifier = Modifier.align(Alignment.BottomCenter).padding(dp16)) {
      ClerkErrorSnackbar(snackbarHostState = snackbarHostState)
    }
  }
}

@Composable
private fun SheetTitle(text: String) {
  Text(
    modifier = Modifier.padding(horizontal = dp24, vertical = dp16),
    text = text,
    style = ClerkMaterialTheme.typography.titleMedium.withMediumWeight(),
    color = ClerkMaterialTheme.colors.foreground,
  )
}

@Composable
private fun OrganizationSwitcherActionRow(text: String, icon: Int, onClick: () -> Unit) {
  Surface(
    modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    shape = ClerkMaterialTheme.shape,
    color = ClerkMaterialTheme.colors.background,
    border = BorderStroke(dp1, ClerkMaterialTheme.computedColors.border),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(dp16),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(dp12),
    ) {
      Box(modifier = Modifier.size(dp36), contentAlignment = Alignment.Center) {
        Icon(
          modifier = Modifier.size(dp24),
          painter = painterResource(icon),
          contentDescription = null,
          tint = ClerkMaterialTheme.colors.mutedForeground,
        )
      }
      Text(
        modifier = Modifier.weight(1f),
        text = text,
        style = ClerkMaterialTheme.typography.bodyLarge.withMediumWeight(),
        color = ClerkMaterialTheme.colors.foreground,
      )
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
        OrganizationAvatarPlaceholder(size = size)
      } else {
        SubcomposeAsyncImage(
          model = imageUrl,
          contentDescription = stringResource(R.string.logo),
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop,
          loading = { OrganizationAvatarPlaceholder(size = size) },
          error = { OrganizationAvatarPlaceholder(size = size) },
        )
      }
    }
  }
}

@Composable
private fun OrganizationAvatarPlaceholder(size: AvatarSize) {
  Icon(
    modifier = Modifier.size(size.toOrganizationSwitcherIconDp()),
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

private fun AvatarSize.toOrganizationSwitcherIconDp() =
  when (this) {
    AvatarSize.SMALL -> dp12
    AvatarSize.MEDIUM -> dp24
    AvatarSize.LARGE -> dp32
    AvatarSize.X_LARGE -> dp48
  }
