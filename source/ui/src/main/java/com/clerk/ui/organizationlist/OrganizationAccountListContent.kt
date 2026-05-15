@file:Suppress("LongParameterList", "TooManyFunctions")

package com.clerk.ui.organizationlist

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.clerk.api.organizations.OrganizationSuggestion
import com.clerk.api.organizations.UserOrganizationInvitation
import com.clerk.api.user.User
import com.clerk.api.user.fullName
import com.clerk.ui.R
import com.clerk.ui.core.avatar.AvatarSize
import com.clerk.ui.core.avatar.AvatarType
import com.clerk.ui.core.avatar.AvatarView
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonConfiguration
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.core.dimens.dp14
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp18
import com.clerk.ui.core.dimens.dp2
import com.clerk.ui.core.dimens.dp20
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp4
import com.clerk.ui.core.dimens.dp48
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.core.extensions.withMediumWeight
import com.clerk.ui.core.footer.SecuredByClerkView
import com.clerk.ui.core.header.HeaderTextView
import com.clerk.ui.core.header.HeaderType
import com.clerk.ui.theme.ClerkMaterialTheme

internal data class OrganizationAccountListHeader(val title: String, val subtitle: String?)

internal data class OrganizationAccountListActions(
  val onRetryInitialLoad: () -> Unit,
  val onLoadMoreMemberships: () -> Unit,
  val onLoadMoreInvitations: () -> Unit,
  val onLoadMoreSuggestions: () -> Unit,
  val onSelectPersonalAccount: () -> Unit,
  val onSelectOrganization: (String) -> Unit,
  val onAcceptInvitation: (UserOrganizationInvitation) -> Unit,
  val onAcceptSuggestion: (OrganizationSuggestion) -> Unit,
  val onCreateOrganization: () -> Unit,
)

@Composable
internal fun OrganizationAccountListContent(
  state: OrganizationAccountListState,
  user: User?,
  activeOrganizationId: String?,
  header: OrganizationAccountListHeader?,
  showPersonalAccount: Boolean,
  showSelectedAccessory: Boolean,
  actions: OrganizationAccountListActions,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(horizontal = dp18, vertical = dp16),
  showSecuredByClerk: Boolean = true,
  showCreateOrganization: Boolean = true,
) {
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = contentPadding,
    verticalArrangement = Arrangement.spacedBy(dp12),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    header?.let { item { OrganizationAccountListHeaderView(header = it) } }

    initialLoadRetrySection(state = state, actions = actions)
    personalAccountSection(
      user = user,
      state = state,
      activeOrganizationId = activeOrganizationId,
      showPersonalAccount = showPersonalAccount,
      showSelectedAccessory = showSelectedAccessory,
      actions = actions,
    )
    membershipsSection(
      state = state,
      activeOrganizationId = activeOrganizationId,
      showSelectedAccessory = showSelectedAccessory,
      actions = actions,
    )
    invitationsSection(state = state, actions = actions)
    suggestionsSection(state = state, actions = actions)
    createOrganizationSection(
      state = state,
      showCreateOrganization = showCreateOrganization,
      actions = actions,
    )
    securedByClerkSection(showSecuredByClerk = showSecuredByClerk)
  }
}

private fun LazyListScope.initialLoadRetrySection(
  state: OrganizationAccountListState,
  actions: OrganizationAccountListActions,
) {
  if (state.initialLoadFailed) {
    item {
      LoadMoreButton(
        text = stringResource(R.string.try_again),
        isLoading = false,
        onClick = actions.onRetryInitialLoad,
      )
    }
  }
}

private fun LazyListScope.personalAccountSection(
  user: User?,
  state: OrganizationAccountListState,
  activeOrganizationId: String?,
  showPersonalAccount: Boolean,
  showSelectedAccessory: Boolean,
  actions: OrganizationAccountListActions,
) {
  if (showPersonalAccount && user != null) {
    item {
      PersonalAccountRow(
        user = user,
        isSelected = showSelectedAccessory && activeOrganizationId == null,
        isLoading = state.activeActionId == PERSONAL_ACCOUNT_ACTION_ID,
        onClick = actions.onSelectPersonalAccount,
      )
    }
  }
}

private fun LazyListScope.membershipsSection(
  state: OrganizationAccountListState,
  activeOrganizationId: String?,
  showSelectedAccessory: Boolean,
  actions: OrganizationAccountListActions,
) {
  items(count = state.memberships.size, key = { state.memberships[it].id }) { index ->
    val membership = state.memberships[index]
    OrganizationRow(
      name = membership.organization.name,
      imageUrl = membership.organization.imageUrl,
      subtitle = membership.roleName,
      isSelected = showSelectedAccessory && membership.organization.id == activeOrganizationId,
      isLoading = state.activeActionId == membership.organization.id,
      onClick = { actions.onSelectOrganization(membership.organization.id) },
    )
  }

  if (state.membershipsHasNextPage) {
    item {
      LoadMoreButton(
        isLoading = state.isLoadingMoreMemberships,
        onClick = actions.onLoadMoreMemberships,
      )
    }
  }
}

private fun LazyListScope.invitationsSection(
  state: OrganizationAccountListState,
  actions: OrganizationAccountListActions,
) {
  if (state.membershipsHasNextPage) return

  items(count = state.invitations.size, key = { state.invitations[it].id }) { index ->
    val invitation = state.invitations[index]
    val isAccepted =
      invitation.status == ACCEPTED_STATUS ||
        invitation.publicOrganizationData.id in state.acceptedInvitationOrganizationIds
    InvitationRow(
      invitation = invitation,
      isAccepted = isAccepted,
      isLoading = state.activeActionId == invitation.id,
      isSelecting = state.activeActionId == invitation.publicOrganizationData.id,
      onAccept = { actions.onAcceptInvitation(invitation) },
      onSelect = { actions.onSelectOrganization(invitation.publicOrganizationData.id) },
    )
  }

  if (state.invitationsHasNextPage) {
    item {
      LoadMoreButton(
        isLoading = state.isLoadingMoreInvitations,
        onClick = actions.onLoadMoreInvitations,
      )
    }
  }
}

private fun LazyListScope.suggestionsSection(
  state: OrganizationAccountListState,
  actions: OrganizationAccountListActions,
) {
  if (state.membershipsHasNextPage || state.invitationsHasNextPage) return

  items(count = state.suggestions.size, key = { state.suggestions[it].id }) { index ->
    val suggestion = state.suggestions[index]
    SuggestionRow(
      suggestion = suggestion,
      isLoading = state.activeActionId == suggestion.id,
      onAccept = { actions.onAcceptSuggestion(suggestion) },
    )
  }

  if (state.suggestionsHasNextPage) {
    item {
      LoadMoreButton(
        isLoading = state.isLoadingMoreSuggestions,
        onClick = actions.onLoadMoreSuggestions,
      )
    }
  }
}

private fun LazyListScope.createOrganizationSection(
  state: OrganizationAccountListState,
  showCreateOrganization: Boolean,
  actions: OrganizationAccountListActions,
) {
  if (state.canShowCreateOrganization(showCreateOrganization)) {
    item { CreateOrganizationRow(onClick = actions.onCreateOrganization) }
  }
}

private fun OrganizationAccountListState.canShowCreateOrganization(
  showCreateOrganization: Boolean
): Boolean {
  if (!showCreateOrganization) return false
  return hasLoadedInitialResources && !hasNextPage && canCreateOrganization
}

private fun LazyListScope.securedByClerkSection(showSecuredByClerk: Boolean) {
  if (showSecuredByClerk) {
    item {
      Spacer(modifier = Modifier.size(dp12))
      SecuredByClerkView()
    }
  }
}

@Composable
private fun OrganizationAccountListHeaderView(header: OrganizationAccountListHeader) {
  Column(
    modifier = Modifier.fillMaxWidth().padding(bottom = dp20),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(dp8),
  ) {
    HeaderTextView(text = header.title, type = HeaderType.Title)
    header.subtitle?.let { HeaderTextView(text = it, type = HeaderType.Subtitle) }
  }
}

@Composable
private fun PersonalAccountRow(
  user: User,
  isSelected: Boolean,
  isLoading: Boolean,
  onClick: () -> Unit,
) {
  OrganizationRow(
    name = user.displayName().ifBlank { stringResource(R.string.personal_account) },
    imageUrl = user.imageUrl,
    subtitle = user.primaryEmailAddress?.emailAddress ?: user.username,
    avatarType = AvatarType.USER,
    isSelected = isSelected,
    isLoading = isLoading,
    onClick = onClick,
  )
}

@Composable
private fun InvitationRow(
  invitation: UserOrganizationInvitation,
  isAccepted: Boolean,
  isLoading: Boolean,
  isSelecting: Boolean,
  onAccept: () -> Unit,
  onSelect: () -> Unit,
) {
  val data = invitation.publicOrganizationData
  OrganizationRow(
    name = data.name,
    imageUrl = data.imageUrl,
    subtitle = if (isAccepted) displayRoleName(invitation.role) else null,
    isLoading = isSelecting,
    onClick = if (isAccepted) onSelect else null,
    action = {
      if (!isAccepted) {
        PillActionButton(
          text = stringResource(R.string.join),
          isLoading = isLoading,
          onClick = onAccept,
        )
      }
    },
  )
}

@Composable
private fun SuggestionRow(
  suggestion: OrganizationSuggestion,
  isLoading: Boolean,
  onAccept: () -> Unit,
) {
  OrganizationRow(
    name = suggestion.publicOrganizationData.name,
    imageUrl = suggestion.publicOrganizationData.imageUrl,
    subtitle =
      if (suggestion.status == ACCEPTED_STATUS) stringResource(R.string.pending_approval) else null,
    action = {
      if (suggestion.status != ACCEPTED_STATUS) {
        PillActionButton(
          text = stringResource(R.string.request_to_join),
          isLoading = isLoading,
          onClick = onAccept,
        )
      }
    },
  )
}

@Composable
private fun CreateOrganizationRow(onClick: () -> Unit) {
  OrganizationRow(
    name = stringResource(R.string.create_organization),
    imageUrl = null,
    leadingIcon = R.drawable.ic_plus,
    onClick = onClick,
  )
}

@Composable
private fun OrganizationRow(
  name: String,
  imageUrl: String?,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  leadingIcon: Int? = null,
  avatarType: AvatarType = AvatarType.ORGANIZATION,
  isSelected: Boolean = false,
  isLoading: Boolean = false,
  onClick: (() -> Unit)? = null,
  action: @Composable (() -> Unit)? = null,
) {
  val shape = ClerkMaterialTheme.shape
  Surface(
    modifier =
      Modifier.fillMaxWidth()
        .then(
          if (onClick != null && !isSelected) Modifier.clickable(onClick = onClick) else Modifier
        )
        .then(modifier),
    shape = shape,
    color = ClerkMaterialTheme.colors.background,
    border = BorderStroke(dp1, ClerkMaterialTheme.computedColors.border),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(dp16),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(dp12),
    ) {
      OrganizationAccountAvatar(
        imageUrl = imageUrl,
        leadingIcon = leadingIcon,
        shape = shape,
        avatarType = avatarType,
      )
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dp4)) {
        Text(
          text = name,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          style = ClerkMaterialTheme.typography.bodyLarge.withMediumWeight(),
          color = ClerkMaterialTheme.colors.foreground,
        )
        subtitle?.let {
          Text(
            text = it,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = ClerkMaterialTheme.typography.bodySmall,
            color = ClerkMaterialTheme.colors.mutedForeground,
          )
        }
      }
      OrganizationRowAccessory(
        isLoading = isLoading,
        action = action,
        isSelected = isSelected,
        isClickable = onClick != null,
      )
    }
  }
}

@Composable
private fun OrganizationRowAccessory(
  isLoading: Boolean,
  action: @Composable (() -> Unit)?,
  isSelected: Boolean,
  isClickable: Boolean,
) {
  when {
    isLoading -> CircularProgressIndicator(modifier = Modifier.size(dp24), strokeWidth = dp2)
    action != null -> action()
    isSelected ->
      Icon(
        painter = painterResource(R.drawable.ic_check),
        contentDescription = stringResource(R.string.current_organization),
        tint = ClerkMaterialTheme.colors.foreground,
      )
    isClickable ->
      Icon(
        painter = painterResource(R.drawable.ic_chevron_right),
        contentDescription = null,
        tint = ClerkMaterialTheme.colors.mutedForeground,
      )
  }
}

@Composable
private fun OrganizationAccountAvatar(
  imageUrl: String?,
  leadingIcon: Int?,
  shape: Shape,
  avatarType: AvatarType,
) {
  Box(modifier = Modifier.size(dp48), contentAlignment = Alignment.Center) {
    if (leadingIcon == null) {
      AvatarView(
        imageUrl = imageUrl,
        size = AvatarSize.LARGE,
        shape = shape,
        avatarType = avatarType,
      )
    } else {
      Surface(
        modifier = Modifier.fillMaxSize(),
        shape = shape,
        color = ClerkMaterialTheme.colors.muted,
        border = BorderStroke(dp1, ClerkMaterialTheme.computedColors.border),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            painter = painterResource(leadingIcon),
            contentDescription = null,
            tint = ClerkMaterialTheme.colors.mutedForeground,
          )
        }
      }
    }
  }
}

@Composable
internal fun PillActionButton(text: String, isLoading: Boolean = false, onClick: () -> Unit) {
  Surface(
    modifier = Modifier.clickable(enabled = !isLoading, onClick = onClick),
    shape = ClerkMaterialTheme.shape,
    color = ClerkMaterialTheme.colors.background,
    border = BorderStroke(dp1, ClerkMaterialTheme.computedColors.buttonBorder),
    shadowElevation = dp2,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = dp14, vertical = dp8),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(dp8),
    ) {
      if (isLoading) {
        CircularProgressIndicator(modifier = Modifier.size(dp14), strokeWidth = dp2)
      }
      Text(
        text = text,
        style = ClerkMaterialTheme.typography.bodySmall.withMediumWeight(),
        color = ClerkMaterialTheme.colors.foreground,
      )
    }
  }
}

@Composable
private fun LoadMoreButton(
  isLoading: Boolean,
  onClick: () -> Unit,
  text: String = stringResource(R.string.load_more),
) {
  ClerkButton(
    modifier = Modifier.fillMaxWidth(),
    text = text,
    isLoading = isLoading,
    onClick = onClick,
    configuration =
      ClerkButtonDefaults.configuration(
        style = ClerkButtonConfiguration.ButtonStyle.Secondary,
        emphasis = ClerkButtonConfiguration.Emphasis.High,
      ),
  )
}

private fun User.displayName(): String {
  return fullName()
    .ifBlank { username.orEmpty() }
    .ifBlank { primaryEmailAddress?.emailAddress.orEmpty() }
}

private fun displayRoleName(role: String): String {
  return role.removePrefix("org:").replace("_", " ").replaceFirstChar { it.titlecase() }
}

private const val ACCEPTED_STATUS = "accepted"
