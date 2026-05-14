@file:Suppress("LongMethod", "LongParameterList", "TooManyFunctions")

package com.clerk.ui.organizationprofile.members

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import com.clerk.ui.core.dimens.dp32
import com.clerk.ui.core.dimens.dp36
import com.clerk.ui.core.dimens.dp4
import com.clerk.ui.core.dimens.dp48
import com.clerk.ui.core.dimens.dp6
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.core.extensions.withMediumWeight
import com.clerk.ui.core.input.ClerkTextField
import com.clerk.ui.core.menu.DropDownItem
import com.clerk.ui.core.menu.ItemMoreMenu
import com.clerk.ui.core.scaffold.ClerkThemedProfileScaffold
import com.clerk.ui.organizationprofile.invite.parseInviteEmailAddresses
import com.clerk.ui.theme.ClerkMaterialTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.collections.immutable.ImmutableList
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
    onErrorShown = viewModel::clearError,
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
    contentPadding = PaddingValues(vertical = dp16),
    verticalArrangement = Arrangement.spacedBy(dp16),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    if (state.availableTabs.size > 1) {
      item {
        OrganizationMembersTabs(
          modifier = Modifier.fillMaxWidth().padding(horizontal = dp18),
          availableTabs = state.availableTabs.toImmutableList(),
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
      modifier = Modifier.fillMaxWidth().padding(horizontal = dp18),
      value = state.memberQuery,
      onValueChange = actions.onMemberSearchChanged,
      label = stringResource(R.string.search),
      leadingIcon = R.drawable.ic_search,
    )
  }

  if (state.hasRoleSetMigration) {
    item {
      NoticeText(
        modifier = Modifier.fillMaxWidth().padding(horizontal = dp18),
        text = stringResource(R.string.role_set_migration_notice),
      )
    }
  }

  if (state.members.isEmpty()) {
    item {
      EmptyState(
        modifier = Modifier.padding(horizontal = dp18),
        text = stringResource(R.string.no_members_found),
      )
    }
  } else {
    item {
      MemberList(
        members = state.members,
        viewerMembership = viewerMembership,
        roles = state.roles,
        canManage = viewerMembership?.canManageMemberships == true,
        roleEditingEnabled = !state.hasRoleSetMigration,
        activeMutationId = state.activeMutationId,
        onUpdateMemberRole = actions.onUpdateMemberRole,
        onRemoveMember = actions.onRemoveMember,
      )
    }
  }

  if (state.membersHasNextPage) {
    item {
      LoadMoreButton(
        modifier = Modifier.padding(horizontal = dp18),
        isLoading = state.isLoadingMoreMembers,
        onClick = actions.onLoadMoreMembers,
      )
    }
  }
}

private fun LazyListScope.invitationsTab(
  organization: Organization,
  state: OrganizationMembersState,
  actions: OrganizationMembersActions,
) {
  item {
    InviteMembersComposer(
      modifier = Modifier.padding(horizontal = dp24),
      organization = organization,
      state = state,
      actions = actions,
    )
  }

  if (state.invitations.isEmpty()) {
    item {
      EmptyState(
        modifier = Modifier.padding(horizontal = dp18),
        text = stringResource(R.string.no_pending_invitations),
      )
    }
  } else {
    items(state.invitations, key = { it.id }) { invitation ->
      InvitationRow(
        modifier = Modifier.padding(horizontal = dp18),
        invitation = invitation,
        isLoading = state.activeMutationId == invitation.id,
        onRevoke = { actions.onRevokeInvitation(invitation) },
      )
    }
  }

  if (state.invitationsHasNextPage) {
    item {
      LoadMoreButton(
        modifier = Modifier.padding(horizontal = dp18),
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
    item {
      EmptyState(
        modifier = Modifier.padding(horizontal = dp18),
        text = stringResource(R.string.no_membership_requests),
      )
    }
  } else {
    items(state.requests, key = { it.id }) { request ->
      MembershipRequestRow(
        modifier = Modifier.padding(horizontal = dp18),
        request = request,
        isLoading = state.activeMutationId == request.id,
        onAccept = { actions.onAcceptRequest(request) },
        onReject = { actions.onRejectRequest(request) },
      )
    }
  }

  if (state.requestsHasNextPage) {
    item {
      LoadMoreButton(
        modifier = Modifier.padding(horizontal = dp18),
        isLoading = state.isLoadingMoreRequests,
        onClick = actions.onLoadMoreRequests,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrganizationMembersTabs(
  availableTabs: ImmutableList<OrganizationMembersTab>,
  selectedTab: OrganizationMembersTab?,
  modifier: Modifier = Modifier,
  onSelectTab: (OrganizationMembersTab) -> Unit,
) {
  val selected = selectedTab ?: availableTabs.firstOrNull()
  val colors =
    SegmentedButtonDefaults.colors(
      activeContainerColor = ClerkMaterialTheme.colors.muted,
      activeContentColor = ClerkMaterialTheme.colors.foreground,
      activeBorderColor = ClerkMaterialTheme.computedColors.buttonBorder,
      inactiveContainerColor = ClerkMaterialTheme.colors.background,
      inactiveContentColor = ClerkMaterialTheme.colors.mutedForeground,
      inactiveBorderColor = ClerkMaterialTheme.computedColors.buttonBorder,
    )

  SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
    availableTabs.forEachIndexed { index, tab ->
      SegmentedButton(
        selected = tab == selected,
        onClick = { onSelectTab(tab) },
        shape = SegmentedButtonDefaults.itemShape(index = index, count = availableTabs.size),
        colors = colors,
        icon = {},
        label = { Text(text = tab.label(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
      )
    }
  }
}

@Composable
private fun MemberList(
  members: List<OrganizationMembership>,
  viewerMembership: OrganizationMembership?,
  roles: List<Role>,
  canManage: Boolean,
  roleEditingEnabled: Boolean,
  activeMutationId: String?,
  onUpdateMemberRole: (OrganizationMembership, String) -> Unit,
  onRemoveMember: (OrganizationMembership) -> Unit,
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    members.forEachIndexed { index, membership ->
      MemberRow(
        membership = membership,
        isViewer = membership.isSameMembership(viewerMembership),
        roles = roles,
        canManage = canManage,
        roleEditingEnabled = roleEditingEnabled,
        showTopDivider = index == 0,
        isLoading = activeMutationId == membership.id,
        onUpdateRole = { role -> onUpdateMemberRole(membership, role) },
        onRemove = { onRemoveMember(membership) },
      )
    }
  }
}

@Composable
private fun MemberRow(
  membership: OrganizationMembership,
  isViewer: Boolean,
  roles: List<Role>,
  canManage: Boolean,
  roleEditingEnabled: Boolean,
  showTopDivider: Boolean,
  isLoading: Boolean,
  onUpdateRole: (String) -> Unit,
  onRemove: () -> Unit,
) {
  Surface(modifier = Modifier.fillMaxWidth(), color = ClerkMaterialTheme.colors.background) {
    Column(modifier = Modifier.fillMaxWidth()) {
      if (showTopDivider) MemberDivider()
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = dp24, vertical = dp16),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(dp16),
      ) {
        MemberAvatar(publicUserData = membership.publicUserData)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dp4)) {
          if (isViewer) {
            CurrentMemberBadge()
          }
          Text(
            text = membership.publicUserData.displayName(),
            style = ClerkMaterialTheme.typography.bodyLarge,
            color = ClerkMaterialTheme.colors.foreground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          MemberDetails(membership = membership, isViewer = isViewer)
        }
        if (canManage) {
          MemberMoreMenu(
            currentRole = membership.role,
            roles = roles,
            roleEditingEnabled = roleEditingEnabled,
            enabled = !isLoading,
            onUpdateRole = onUpdateRole,
            onRemove = onRemove,
          )
        }
      }
      MemberDivider()
    }
  }
}

@Composable
private fun CurrentMemberBadge() {
  Surface(
    shape = ClerkMaterialTheme.shape,
    color = ClerkMaterialTheme.colors.background,
    border = BorderStroke(dp1, ClerkMaterialTheme.computedColors.buttonBorder),
  ) {
    Text(
      modifier = Modifier.padding(horizontal = dp6),
      text = stringResource(R.string.you),
      style = ClerkMaterialTheme.typography.bodySmall,
      color = ClerkMaterialTheme.colors.mutedForeground,
      maxLines = 1,
    )
  }
}

@Composable
private fun MemberDetails(membership: OrganizationMembership, isViewer: Boolean) {
  val identifier = membership.publicUserData?.identifier.orEmpty()
  val joinedLabel =
    stringResource(R.string.joined_date, formattedMembershipDate(membership.createdAt))
  if (identifier.isNotBlank()) {
    MemberDetailText(text = identifier)
  }

  if (isViewer) {
    MemberDetailText(
      text = stringResource(R.string.member_role_joined, membership.displayRole(), joinedLabel),
      useSmallText = true,
    )
  } else {
    MemberDetailText(text = membership.displayRole())
    MemberDetailText(text = joinedLabel)
  }
}

@Composable
private fun MemberDetailText(text: String, useSmallText: Boolean = false) {
  Text(
    text = text,
    style =
      if (useSmallText) ClerkMaterialTheme.typography.bodySmall
      else ClerkMaterialTheme.typography.bodyMedium,
    color = ClerkMaterialTheme.colors.mutedForeground,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
  )
}

@Composable
private fun MemberDivider() {
  HorizontalDivider(color = ClerkMaterialTheme.computedColors.border.copy(alpha = 0.06f))
}

@Composable
private fun MemberAvatar(publicUserData: PublicUserData?) {
  AvatarView(
    imageUrl = publicUserData?.imageUrl?.takeIf { it.isNotBlank() },
    size = AvatarSize.MEDIUM,
    shape = CircleShape,
    avatarType = AvatarType.USER,
    modifier = Modifier.size(dp36),
  )
}

@Composable
private fun InviteMembersComposer(
  organization: Organization,
  state: OrganizationMembersState,
  actions: OrganizationMembersActions,
  modifier: Modifier = Modifier,
) {
  var emailInput by rememberSaveable { mutableStateOf("") }
  val submittedEmails = (state.inviteEmails + parseInviteEmailAddresses(emailInput)).distinct()
  val limitExceeded = submittedEmails.size > state.remainingInviteSlots(organization)
  val canSend =
    state.selectedInviteRoleKey != null &&
      submittedEmails.isNotEmpty() &&
      state.activeMutationId == null &&
      !limitExceeded

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(dp24)) {
    Text(
      text = stringResource(R.string.enter_or_paste_email_addresses),
      style = ClerkMaterialTheme.typography.bodyMedium,
      color = ClerkMaterialTheme.colors.mutedForeground,
    )
    ClerkTextField(
      modifier = Modifier.fillMaxWidth().height(112.dp),
      value = emailInput,
      onValueChange = { emailInput = it },
      label = stringResource(R.string.enter_email_addresses),
      maxLines = INVITE_EMAIL_MAX_LINES,
    )
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(dp12),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = stringResource(R.string.role),
        style = ClerkMaterialTheme.typography.bodyMedium,
        color = ClerkMaterialTheme.colors.mutedForeground,
      )
      RoleDropdown(
        roles = state.roles,
        selectedRoleKey = state.selectedInviteRoleKey,
        enabled = state.activeMutationId == null,
        onSelectRole = actions.onSelectInviteRole,
      )
    }
    if (limitExceeded) {
      Text(
        text = stringResource(R.string.invite_limit_exceeded),
        style = ClerkMaterialTheme.typography.bodySmall,
        color = ClerkMaterialTheme.colors.danger,
      )
    }
    ClerkButton(
      modifier = Modifier.fillMaxWidth(),
      text = stringResource(R.string.send_invitations),
      isLoading = state.activeMutationId == INVITE_MUTATION_ID,
      isEnabled = canSend,
      icons =
        ClerkButtonDefaults.icons(
          trailingIcon = R.drawable.ic_triangle_right,
          trailingIconColor = ClerkMaterialTheme.colors.primaryForeground,
        ),
      onClick = {
        actions.onInviteInputSubmitted(emailInput)
        actions.onSendInvitations()
        emailInput = ""
      },
    )
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
    modifier =
      Modifier.height(dp32).clickable(enabled = enabled && roles.isNotEmpty()) { expanded = true },
    shape = ClerkMaterialTheme.shape,
    color = ClerkMaterialTheme.colors.background,
    border = BorderStroke(dp1, ClerkMaterialTheme.computedColors.buttonBorder),
    shadowElevation = dp2,
  ) {
    Row(
      modifier = Modifier.padding(start = dp12, top = dp4, end = dp8, bottom = dp4),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(dp4),
    ) {
      Text(
        text = selectedRole?.name ?: stringResource(R.string.no_roles_available),
        style = ClerkMaterialTheme.typography.bodyMedium,
        color =
          if (enabled) ClerkMaterialTheme.colors.foreground
          else ClerkMaterialTheme.colors.mutedForeground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Icon(
        modifier = Modifier.size(dp16),
        painter = painterResource(R.drawable.ic_chevron_down),
        contentDescription = null,
        tint = ClerkMaterialTheme.colors.mutedForeground,
      )
      DropdownMenu(
        modifier = Modifier.background(ClerkMaterialTheme.colors.background),
        expanded = expanded,
        onDismissRequest = { expanded = false },
        containerColor = ClerkMaterialTheme.colors.background,
        tonalElevation = dp0,
      ) {
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
private fun InvitationRow(
  invitation: OrganizationInvitation,
  isLoading: Boolean,
  onRevoke: () -> Unit,
  modifier: Modifier = Modifier,
) {
  ListCard(modifier = modifier) {
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
  modifier: Modifier = Modifier,
) {
  ListCard(modifier = modifier) {
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
private fun MemberMoreMenu(
  currentRole: String?,
  roles: List<Role>,
  roleEditingEnabled: Boolean,
  enabled: Boolean,
  onUpdateRole: (String) -> Unit,
  onRemove: () -> Unit,
) {
  var expanded by rememberSaveable { mutableStateOf(false) }
  var mode by rememberSaveable { mutableStateOf(MemberMenuMode.Actions) }
  val canChangeRole = roleEditingEnabled && roles.isNotEmpty()

  Box {
    IconButton(
      onClick = {
        mode = MemberMenuMode.Actions
        expanded = true
      }
    ) {
      Icon(
        imageVector = Icons.Outlined.MoreVert,
        contentDescription = stringResource(R.string.more_options),
        tint = ClerkMaterialTheme.colors.mutedForeground,
      )
    }

    DropdownMenu(
      modifier =
        Modifier.defaultMinSize(minWidth = 220.dp).background(ClerkMaterialTheme.colors.background),
      expanded = expanded,
      onDismissRequest = {
        expanded = false
        mode = MemberMenuMode.Actions
      },
      shape = ClerkMaterialTheme.shape,
    ) {
      when (mode) {
        MemberMenuMode.Actions ->
          MemberActionsMenu(
            canChangeRole = canChangeRole,
            enabled = enabled,
            onChangeRole = { mode = MemberMenuMode.Roles },
            onRemove = {
              expanded = false
              mode = MemberMenuMode.Actions
              onRemove()
            },
          )
        MemberMenuMode.Roles ->
          MemberRoleMenu(
            currentRole = currentRole,
            roles = roles,
            enabled = enabled,
            onBack = { mode = MemberMenuMode.Actions },
            onSelectRole = { role ->
              expanded = false
              mode = MemberMenuMode.Actions
              if (role.key != currentRole) onUpdateRole(role.key)
            },
          )
      }
    }
  }
}

@Composable
private fun MemberActionsMenu(
  canChangeRole: Boolean,
  enabled: Boolean,
  onChangeRole: () -> Unit,
  onRemove: () -> Unit,
) {
  if (canChangeRole) {
    DropdownMenuItem(
      contentPadding = PaddingValues(horizontal = dp12, vertical = dp12),
      text = { Text(text = stringResource(R.string.change_role), style = menuItemTextStyle()) },
      trailingIcon = {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
          contentDescription = null,
          tint = ClerkMaterialTheme.colors.foreground,
        )
      },
      enabled = enabled,
      onClick = onChangeRole,
    )
  }
  DropdownMenuItem(
    contentPadding = PaddingValues(horizontal = dp12, vertical = dp12),
    text = { Text(text = stringResource(R.string.remove_member), style = menuItemTextStyle()) },
    enabled = enabled,
    colors = MenuDefaults.itemColors(textColor = ClerkMaterialTheme.colors.danger),
    onClick = onRemove,
  )
}

@Composable
private fun MemberRoleMenu(
  currentRole: String?,
  roles: List<Role>,
  enabled: Boolean,
  onBack: () -> Unit,
  onSelectRole: (Role) -> Unit,
) {
  DropdownMenuItem(
    contentPadding = PaddingValues(horizontal = dp12, vertical = dp12),
    leadingIcon = {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
        contentDescription = null,
        tint = ClerkMaterialTheme.colors.foreground,
      )
    },
    text = { Text(text = stringResource(R.string.back), style = menuItemTextStyle()) },
    onClick = onBack,
  )
  roles.forEach { role ->
    val selected = role.key == currentRole
    DropdownMenuItem(
      contentPadding = PaddingValues(horizontal = dp12, vertical = dp12),
      text = { Text(text = role.name, style = menuItemTextStyle()) },
      trailingIcon =
        if (selected) {
          {
            Icon(
              imageVector = Icons.Filled.Check,
              contentDescription = null,
              tint = ClerkMaterialTheme.colors.foreground,
            )
          }
        } else {
          null
        },
      enabled = enabled,
      onClick = { onSelectRole(role) },
    )
  }
}

@Composable private fun menuItemTextStyle() = ClerkMaterialTheme.typography.bodyLarge

@Composable
private fun LoadMoreButton(isLoading: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
  ClerkButton(
    modifier = modifier.fillMaxWidth(),
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
private fun EmptyState(text: String, modifier: Modifier = Modifier) {
  Text(
    modifier = modifier.fillMaxWidth().padding(vertical = dp24),
    text = text,
    style = ClerkMaterialTheme.typography.bodyMedium,
    color = ClerkMaterialTheme.colors.mutedForeground,
  )
}

@Composable
private fun NoticeText(text: String, modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier.fillMaxWidth(),
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
private fun ListCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
  Surface(
    modifier = modifier.fillMaxWidth(),
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

private fun OrganizationMembership.isSameMembership(other: OrganizationMembership?): Boolean {
  if (other == null) return false
  return id == other.id ||
    publicUserData?.userId?.let { userId -> userId == other.publicUserData?.userId } == true
}

private fun OrganizationMembership.displayRole(): String {
  return roleName.ifBlank { role }
}

private fun formattedMembershipDate(timestampMillis: Long): String {
  return SimpleDateFormat("M/d/yyyy", Locale.getDefault()).format(Date(timestampMillis))
}

private enum class MemberMenuMode {
  Actions,
  Roles,
}

private enum class InvitationAction {
  Revoke
}

private const val INVITE_MUTATION_ID = "invite"
private const val INVITE_EMAIL_MAX_LINES = 4
