@file:Suppress("LongMethod", "LongParameterList", "TooManyFunctions")

package com.clerk.ui.organizationprofile.members

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.Clerk
import com.clerk.api.network.model.userdata.PublicUserData
import com.clerk.api.organizations.Organization
import com.clerk.api.organizations.OrganizationInvitation
import com.clerk.api.organizations.OrganizationMembership
import com.clerk.api.organizations.OrganizationMembershipRequest
import com.clerk.api.organizations.Role
import com.clerk.ui.R
import com.clerk.ui.core.avatar.AvatarSize
import com.clerk.ui.core.avatar.AvatarType
import com.clerk.ui.core.avatar.AvatarView
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonConfiguration
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
import com.clerk.ui.core.dimens.dp0
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp18
import com.clerk.ui.core.dimens.dp2
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp4
import com.clerk.ui.core.dimens.dp48
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.core.extensions.withMediumWeight
import com.clerk.ui.core.input.ClerkTextField
import com.clerk.ui.core.menu.DropDownItem
import com.clerk.ui.core.menu.ItemMoreMenu
import com.clerk.ui.core.scaffold.ClerkThemedProfileScaffold
import com.clerk.ui.theme.ClerkMaterialTheme
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun OrganizationMembersView(
  organization: Organization,
  membership: OrganizationMembership?,
  onBackPressed: () -> Unit,
  modifier: Modifier = Modifier,
  initialTab: OrganizationMembersTab? = null,
  viewModel: OrganizationMembersViewModel = viewModel(),
) {
  val state by viewModel.state.collectAsState()

  LaunchedEffect(organization.id, membership?.id, initialTab) {
    viewModel.load(
      organization = organization,
      membership = membership,
      domainsEnabled = Clerk.organizationDomainsIsEnabled,
      initialTab = initialTab,
    )
  }

  ClerkThemedProfileScaffold(
    modifier = modifier,
    title = stringResource(R.string.members),
    horizontalPadding = dp0,
    onBackPressed = onBackPressed,
    errorMessage = state.errorMessage,
    content = {
      OrganizationMembersContent(
        organization = organization,
        viewerMembership = membership,
        state = state,
        actions =
          OrganizationMembersActions(
            onRetry = viewModel::retry,
            onSelectTab = viewModel::selectTab,
            onMemberSearchChanged = viewModel::setMemberQuery,
            onLoadMoreMembers = viewModel::loadMoreMembers,
            onLoadMoreInvitations = viewModel::loadMoreInvitations,
            onLoadMoreRequests = viewModel::loadMoreRequests,
            onSelectInviteRole = viewModel::selectInviteRole,
            onInviteInputSubmitted = viewModel::addInviteEmails,
            onRemoveInviteEmail = viewModel::removeInviteEmail,
            onSendInvitations = viewModel::sendInvitations,
            onUpdateMemberRole = viewModel::updateMemberRole,
            onRemoveMember = viewModel::removeMember,
            onRevokeInvitation = viewModel::revokeInvitation,
            onAcceptRequest = viewModel::acceptRequest,
            onRejectRequest = viewModel::rejectRequest,
          ),
      )
    },
  )
}

@Composable
internal fun OrganizationMembersContent(
  organization: Organization,
  viewerMembership: OrganizationMembership?,
  state: OrganizationMembersState,
  actions: OrganizationMembersActions,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = dp18, vertical = dp16),
    verticalArrangement = Arrangement.spacedBy(dp16),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    if (state.availableTabs.size > 1) {
      item {
        OrganizationMembersTabs(
          availableTabs = state.availableTabs,
          selectedTab = state.selectedTab,
          onSelectTab = actions.onSelectTab,
        )
      }
    }

    if (state.isLoadingInitial) {
      item { LoadingState() }
      return@LazyColumn
    }

    if (state.availableTabs.isEmpty()) {
      item { EmptyState(text = stringResource(R.string.no_members_sections_available)) }
      return@LazyColumn
    }

    when (state.selectedTab ?: state.availableTabs.firstOrNull()) {
      OrganizationMembersTab.Members ->
        membersTab(state = state, viewerMembership = viewerMembership, actions = actions)
      OrganizationMembersTab.Invitations ->
        invitationsTab(organization = organization, state = state, actions = actions)
      OrganizationMembersTab.Requests -> requestsTab(state = state, actions = actions)
      null -> item { EmptyState(text = stringResource(R.string.no_members_sections_available)) }
    }
  }
}

private fun LazyListScope.membersTab(
  state: OrganizationMembersState,
  viewerMembership: OrganizationMembership?,
  actions: OrganizationMembersActions,
) {
  item {
    ClerkTextField(
      modifier = Modifier.fillMaxWidth(),
      value = state.memberQuery,
      onValueChange = actions.onMemberSearchChanged,
      label = stringResource(R.string.search_members),
      leadingIcon = R.drawable.ic_search,
    )
  }

  if (state.hasRoleSetMigration) {
    item { NoticeText(text = stringResource(R.string.role_set_migration_notice)) }
  }

  if (state.members.isEmpty()) {
    item { EmptyState(text = stringResource(R.string.no_members_found)) }
  } else {
    items(state.members, key = { it.id }) { membership ->
      MemberRow(
        membership = membership,
        roles = state.roles,
        canManage = viewerMembership?.canManageMemberships == true,
        roleEditingEnabled = !state.hasRoleSetMigration,
        isLoading = state.activeMutationId == membership.id,
        onUpdateRole = { role -> actions.onUpdateMemberRole(membership, role) },
        onRemove = { actions.onRemoveMember(membership) },
      )
    }
  }

  if (state.membersHasNextPage) {
    item {
      LoadMoreButton(isLoading = state.isLoadingMoreMembers, onClick = actions.onLoadMoreMembers)
    }
  }
}

private fun LazyListScope.invitationsTab(
  organization: Organization,
  state: OrganizationMembersState,
  actions: OrganizationMembersActions,
) {
  item { InviteMembersComposer(organization = organization, state = state, actions = actions) }

  if (state.invitations.isEmpty()) {
    item { EmptyState(text = stringResource(R.string.no_pending_invitations)) }
  } else {
    items(state.invitations, key = { it.id }) { invitation ->
      InvitationRow(
        invitation = invitation,
        isLoading = state.activeMutationId == invitation.id,
        onRevoke = { actions.onRevokeInvitation(invitation) },
      )
    }
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

private fun LazyListScope.requestsTab(
  state: OrganizationMembersState,
  actions: OrganizationMembersActions,
) {
  if (state.requests.isEmpty()) {
    item { EmptyState(text = stringResource(R.string.no_membership_requests)) }
  } else {
    items(state.requests, key = { it.id }) { request ->
      MembershipRequestRow(
        request = request,
        isLoading = state.activeMutationId == request.id,
        onAccept = { actions.onAcceptRequest(request) },
        onReject = { actions.onRejectRequest(request) },
      )
    }
  }

  if (state.requestsHasNextPage) {
    item {
      LoadMoreButton(isLoading = state.isLoadingMoreRequests, onClick = actions.onLoadMoreRequests)
    }
  }
}

@Composable
private fun OrganizationMembersTabs(
  availableTabs: List<OrganizationMembersTab>,
  selectedTab: OrganizationMembersTab?,
  onSelectTab: (OrganizationMembersTab) -> Unit,
) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(dp8)) {
    availableTabs.forEach { tab ->
      val selected = tab == selectedTab
      Surface(
        modifier = Modifier.weight(1f).clickable { onSelectTab(tab) },
        shape = ClerkMaterialTheme.shape,
        color =
          if (selected) ClerkMaterialTheme.colors.foreground
          else ClerkMaterialTheme.colors.background,
        border = BorderStroke(dp1, ClerkMaterialTheme.computedColors.buttonBorder),
      ) {
        Text(
          modifier = Modifier.padding(horizontal = dp8, vertical = dp8),
          text = tab.label(),
          style = ClerkMaterialTheme.typography.bodySmall.withMediumWeight(),
          color =
            if (selected) ClerkMaterialTheme.colors.background
            else ClerkMaterialTheme.colors.foreground,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun MemberRow(
  membership: OrganizationMembership,
  roles: List<Role>,
  canManage: Boolean,
  roleEditingEnabled: Boolean,
  isLoading: Boolean,
  onUpdateRole: (String) -> Unit,
  onRemove: () -> Unit,
) {
  ListCard {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(dp12),
    ) {
      UserAvatar(publicUserData = membership.publicUserData)
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dp4)) {
        Text(
          text = membership.publicUserData.displayName(),
          style = ClerkMaterialTheme.typography.bodyMedium.withMediumWeight(),
          color = ClerkMaterialTheme.colors.foreground,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = membership.publicUserData?.identifier.orEmpty(),
          style = ClerkMaterialTheme.typography.bodySmall,
          color = ClerkMaterialTheme.colors.mutedForeground,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        if (canManage && roleEditingEnabled && roles.isNotEmpty()) {
          RoleDropdown(
            roles = roles,
            selectedRoleKey = membership.role,
            enabled = !isLoading,
            onSelectRole = onUpdateRole,
          )
        } else {
          Text(
            text = membership.roleName.ifBlank { membership.role },
            style = ClerkMaterialTheme.typography.bodySmall,
            color = ClerkMaterialTheme.colors.mutedForeground,
          )
        }
      }
      if (canManage) {
        MemberMoreMenu(enabled = !isLoading, onRemove = onRemove)
      }
    }
  }
}

@Composable
private fun InviteMembersComposer(
  organization: Organization,
  state: OrganizationMembersState,
  actions: OrganizationMembersActions,
) {
  var emailInput by rememberSaveable { mutableStateOf("") }
  val limitExceeded = state.inviteWouldExceedMembershipLimit(organization)

  ListCard {
    Column(verticalArrangement = Arrangement.spacedBy(dp12)) {
      Text(
        text = stringResource(R.string.invite_new_members),
        style = ClerkMaterialTheme.typography.bodyMedium.withMediumWeight(),
        color = ClerkMaterialTheme.colors.foreground,
      )
      RoleDropdown(
        roles = state.roles,
        selectedRoleKey = state.selectedInviteRoleKey,
        enabled = state.activeMutationId == null,
        onSelectRole = actions.onSelectInviteRole,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(dp8), verticalAlignment = Alignment.Top) {
        ClerkTextField(
          modifier = Modifier.weight(1f),
          value = emailInput,
          onValueChange = { emailInput = it },
          label = stringResource(R.string.enter_email_addresses),
          leadingIcon = R.drawable.ic_email,
        )
        ClerkButton(
          text = stringResource(R.string.add),
          isEnabled = emailInput.isNotBlank() && state.activeMutationId == null,
          configuration =
            ClerkButtonDefaults.configuration(
              style = ClerkButtonConfiguration.ButtonStyle.Secondary
            ),
          onClick = {
            actions.onInviteInputSubmitted(emailInput)
            emailInput = ""
          },
        )
      }
      EmailChips(emails = state.inviteEmails, onRemoveEmail = actions.onRemoveInviteEmail)
      MembershipLimitText(organization = organization, state = state, limitExceeded = limitExceeded)
      ClerkButton(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(R.string.send_invitations),
        isLoading = state.activeMutationId == INVITE_MUTATION_ID,
        isEnabled = state.canInvite && !limitExceeded,
        onClick = actions.onSendInvitations,
      )
    }
  }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun EmailChips(emails: List<String>, onRemoveEmail: (String) -> Unit) {
  if (emails.isEmpty()) return
  FlowRow(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(dp8),
    verticalArrangement = Arrangement.spacedBy(dp8),
  ) {
    emails.forEach { email ->
      Surface(
        shape = ClerkMaterialTheme.shape,
        color = ClerkMaterialTheme.colors.muted,
        onClick = { onRemoveEmail(email) },
      ) {
        Text(
          modifier = Modifier.padding(horizontal = dp12, vertical = dp8),
          text = email,
          style = ClerkMaterialTheme.typography.bodySmall,
          color = ClerkMaterialTheme.colors.foreground,
        )
      }
    }
  }
}

@Composable
private fun MembershipLimitText(
  organization: Organization,
  state: OrganizationMembersState,
  limitExceeded: Boolean,
) {
  if (organization.maxAllowedMemberships <= 0) return
  val remaining = state.remainingInviteSlots(organization)
  Text(
    text =
      if (limitExceeded) {
        stringResource(R.string.invite_limit_exceeded)
      } else {
        stringResource(R.string.membership_slots_remaining, remaining)
      },
    style = ClerkMaterialTheme.typography.bodySmall,
    color =
      if (limitExceeded) ClerkMaterialTheme.colors.danger
      else ClerkMaterialTheme.colors.mutedForeground,
  )
}

@Composable
private fun InvitationRow(
  invitation: OrganizationInvitation,
  isLoading: Boolean,
  onRevoke: () -> Unit,
) {
  ListCard {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(dp12),
    ) {
      UserAvatar(publicUserData = null)
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dp4)) {
        Text(
          text = invitation.emailAddress,
          style = ClerkMaterialTheme.typography.bodyMedium.withMediumWeight(),
          color = ClerkMaterialTheme.colors.foreground,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = invitation.role,
          style = ClerkMaterialTheme.typography.bodySmall,
          color = ClerkMaterialTheme.colors.mutedForeground,
        )
      }
      ItemMoreMenu(
        dropDownItems =
          listOf(
              DropDownItem(
                id = InvitationAction.Revoke,
                text = stringResource(R.string.revoke_invitation),
                danger = true,
                enabled = !isLoading,
              )
            )
            .toImmutableList(),
        onClick = { onRevoke() },
      )
    }
  }
}

@Composable
private fun MembershipRequestRow(
  request: OrganizationMembershipRequest,
  isLoading: Boolean,
  onAccept: () -> Unit,
  onReject: () -> Unit,
) {
  ListCard {
    Column(verticalArrangement = Arrangement.spacedBy(dp12)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dp12),
      ) {
        UserAvatar(publicUserData = request.publicUserData)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dp4)) {
          Text(
            text = request.publicUserData.displayName(),
            style = ClerkMaterialTheme.typography.bodyMedium.withMediumWeight(),
            color = ClerkMaterialTheme.colors.foreground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = request.publicUserData?.identifier.orEmpty(),
            style = ClerkMaterialTheme.typography.bodySmall,
            color = ClerkMaterialTheme.colors.mutedForeground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
      Row(horizontalArrangement = Arrangement.spacedBy(dp8)) {
        ClerkButton(
          modifier = Modifier.weight(1f),
          text = stringResource(R.string.reject),
          isEnabled = !isLoading,
          configuration =
            ClerkButtonDefaults.configuration(
              style = ClerkButtonConfiguration.ButtonStyle.Secondary
            ),
          onClick = onReject,
        )
        ClerkButton(
          modifier = Modifier.weight(1f),
          text = stringResource(R.string.accept),
          isLoading = isLoading,
          onClick = onAccept,
        )
      }
    }
  }
}

@Composable
private fun RoleDropdown(
  roles: List<Role>,
  selectedRoleKey: String?,
  enabled: Boolean,
  onSelectRole: (String) -> Unit,
) {
  var expanded by rememberSaveable { mutableStateOf(false) }
  val selectedRole = roles.firstOrNull { it.key == selectedRoleKey } ?: roles.firstOrNull()

  Surface(
    modifier = Modifier.clickable(enabled = enabled && roles.isNotEmpty()) { expanded = true },
    shape = ClerkMaterialTheme.shape,
    color = ClerkMaterialTheme.colors.background,
    border = BorderStroke(dp1, ClerkMaterialTheme.computedColors.buttonBorder),
    shadowElevation = dp2,
  ) {
    Box {
      Text(
        modifier = Modifier.padding(horizontal = dp12, vertical = dp8),
        text = selectedRole?.name ?: stringResource(R.string.no_roles_available),
        style = ClerkMaterialTheme.typography.bodySmall,
        color =
          if (enabled) ClerkMaterialTheme.colors.foreground
          else ClerkMaterialTheme.colors.mutedForeground,
      )
      DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        roles.forEach { role ->
          DropdownMenuItem(
            text = { Text(role.name) },
            onClick = {
              expanded = false
              onSelectRole(role.key)
            },
          )
        }
      }
    }
  }
}

@Composable
private fun MemberMoreMenu(enabled: Boolean, onRemove: () -> Unit) {
  ItemMoreMenu(
    dropDownItems =
      listOf(
          DropDownItem(
            id = MemberAction.Remove,
            text = stringResource(R.string.remove_member),
            danger = true,
            enabled = enabled,
          )
        )
        .toImmutableList(),
    onClick = { onRemove() },
  )
}

@Composable
private fun LoadMoreButton(isLoading: Boolean, onClick: () -> Unit) {
  ClerkButton(
    modifier = Modifier.fillMaxWidth(),
    text = stringResource(R.string.load_more),
    isLoading = isLoading,
    configuration =
      ClerkButtonDefaults.configuration(style = ClerkButtonConfiguration.ButtonStyle.Secondary),
    onClick = onClick,
  )
}

@Composable
private fun LoadingState() {
  Box(modifier = Modifier.fillMaxWidth().padding(dp24), contentAlignment = Alignment.Center) {
    CircularProgressIndicator()
  }
}

@Composable
private fun EmptyState(text: String) {
  Text(
    modifier = Modifier.fillMaxWidth().padding(vertical = dp24),
    text = text,
    style = ClerkMaterialTheme.typography.bodyMedium,
    color = ClerkMaterialTheme.colors.mutedForeground,
  )
}

@Composable
private fun NoticeText(text: String) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = ClerkMaterialTheme.shape,
    color = ClerkMaterialTheme.colors.warning.copy(alpha = 0.08f),
    border = BorderStroke(dp1, ClerkMaterialTheme.colors.warning.copy(alpha = 0.2f)),
  ) {
    Text(
      modifier = Modifier.padding(dp12),
      text = text,
      style = ClerkMaterialTheme.typography.bodySmall,
      color = ClerkMaterialTheme.colors.warning,
    )
  }
}

@Composable
private fun ListCard(content: @Composable () -> Unit) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = ClerkMaterialTheme.shape,
    color = ClerkMaterialTheme.colors.background,
    border = BorderStroke(dp1, ClerkMaterialTheme.computedColors.border),
    shadowElevation = dp2,
  ) {
    Column(modifier = Modifier.padding(dp16), verticalArrangement = Arrangement.spacedBy(dp12)) {
      content()
    }
  }
}

@Composable
private fun UserAvatar(publicUserData: PublicUserData?) {
  AvatarView(
    imageUrl = publicUserData?.imageUrl?.takeIf { it.isNotBlank() },
    size = AvatarSize.LARGE,
    shape = ClerkMaterialTheme.shape,
    avatarType = AvatarType.USER,
    modifier = Modifier.size(dp48),
  )
}

@Composable
private fun OrganizationMembersTab.label(): String {
  return when (this) {
    OrganizationMembersTab.Members -> stringResource(R.string.members)
    OrganizationMembersTab.Invitations -> stringResource(R.string.invitations)
    OrganizationMembersTab.Requests -> stringResource(R.string.requests)
  }
}

private fun PublicUserData?.displayName(): String {
  if (this == null) return ""
  return listOfNotNull(firstName, lastName).joinToString(" ").ifBlank { identifier }
}

private enum class MemberAction {
  Remove
}

private enum class InvitationAction {
  Revoke
}

private const val INVITE_MUTATION_ID = "invite"
