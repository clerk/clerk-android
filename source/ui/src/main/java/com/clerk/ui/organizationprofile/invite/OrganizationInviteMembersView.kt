package com.clerk.ui.organizationprofile.invite

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.organizations.Organization
import com.clerk.api.organizations.Role
import com.clerk.ui.R
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp2
import com.clerk.ui.core.dimens.dp20
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp32
import com.clerk.ui.core.dimens.dp4
import com.clerk.ui.core.dimens.dp44
import com.clerk.ui.core.dimens.dp72
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.core.error.ClerkErrorSnackbar
import com.clerk.ui.core.extensions.withMediumWeight
import com.clerk.ui.core.input.ClerkTextField
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
@Suppress("LongMethod")
internal fun OrganizationInviteMembersView(
  organization: Organization,
  onComplete: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: OrganizationInviteMembersViewModel = viewModel(),
) {
  val state by viewModel.state.collectAsState()
  var emailInput by rememberSaveable { mutableStateOf("") }
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(organization.id) { viewModel.loadRoles(organization) }
  LaunchedEffect(state.completion) {
    if (state.completion != null) {
      viewModel.clearCompletion()
      onComplete()
    }
  }
  LaunchedEffect(state.errorMessage) {
    state.errorMessage?.let { snackbarHostState.showSnackbar(it) }
  }

  ClerkMaterialTheme {
    Scaffold(
      modifier = modifier,
      containerColor = ClerkMaterialTheme.colors.background,
      snackbarHost = { ClerkErrorSnackbar(snackbarHostState) },
      topBar = {
        InviteMembersTopBar(
          title = stringResource(R.string.invite_new_members),
          onCancel = onComplete,
        )
      },
    ) { innerPadding ->
      Column(
        modifier =
          Modifier.fillMaxSize()
            .background(ClerkMaterialTheme.colors.background)
            .padding(innerPadding)
            .padding(horizontal = dp24, vertical = dp24),
        verticalArrangement = Arrangement.spacedBy(dp24),
      ) {
        InviteMembersContent(
          emailInput = emailInput,
          state = state,
          onEmailInputChanged = {
            emailInput = it
            viewModel.clearError()
          },
          onSelectRole = viewModel::selectRole,
          onSendInvitations = {
            viewModel.sendInvitations(
              organization = organization,
              emailAddresses = parseInviteEmailAddresses(emailInput),
            )
          },
        )
      }
    }
  }
}

@Composable
private fun InviteMembersTopBar(title: String, onCancel: () -> Unit) {
  Column(modifier = Modifier.fillMaxWidth().background(ClerkMaterialTheme.colors.background)) {
    Row(
      modifier = Modifier.fillMaxWidth().statusBarsPadding().height(dp44).padding(horizontal = dp8),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(modifier = Modifier.width(dp72), contentAlignment = Alignment.CenterStart) {
        Text(
          modifier =
            Modifier.clickable(onClick = onCancel).padding(horizontal = dp8, vertical = dp8),
          text = stringResource(R.string.cancel),
          style = ClerkMaterialTheme.typography.bodyLarge,
          color = ClerkMaterialTheme.colors.primary,
        )
      }
      Text(
        modifier = Modifier.weight(1f),
        text = title,
        style = ClerkMaterialTheme.typography.titleMedium.withMediumWeight(),
        color = ClerkMaterialTheme.colors.foreground,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.width(dp72))
    }
    HorizontalDivider(thickness = dp1, color = ClerkMaterialTheme.computedColors.border)
  }
}

@Composable
private fun InviteMembersContent(
  emailInput: String,
  state: OrganizationInviteMembersState,
  onEmailInputChanged: (String) -> Unit,
  onSelectRole: (String) -> Unit,
  onSendInvitations: () -> Unit,
) {
  Text(
    text = stringResource(R.string.enter_or_paste_email_addresses),
    style = ClerkMaterialTheme.typography.bodyMedium,
    color = ClerkMaterialTheme.colors.mutedForeground,
  )

  ClerkTextField(
    modifier = Modifier.fillMaxWidth().height(112.dp),
    value = emailInput,
    onValueChange = onEmailInputChanged,
    label = stringResource(R.string.enter_email_addresses),
    maxLines = INVITE_EMAIL_MAX_LINES,
  )

  RoleSelector(
    roles = state.roles,
    selectedRoleKey = state.selectedRoleKey,
    isLoading = state.isLoadingRoles,
    onSelectRole = onSelectRole,
  )

  ClerkButton(
    modifier = Modifier.fillMaxWidth(),
    text = stringResource(R.string.send_invitations),
    isLoading = state.isSubmitting,
    isEnabled =
      !state.isLoadingRoles &&
        !state.isSubmitting &&
        state.selectedRoleKey != null &&
        parseInviteEmailAddresses(emailInput).isNotEmpty(),
    icons =
      ClerkButtonDefaults.icons(
        trailingIcon = R.drawable.ic_triangle_right,
        trailingIconColor = ClerkMaterialTheme.colors.primaryForeground,
      ),
    onClick = onSendInvitations,
  )
}

@Composable
private fun RoleSelector(
  roles: List<Role>,
  selectedRoleKey: String?,
  isLoading: Boolean,
  onSelectRole: (String) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(dp12),
  ) {
    Text(
      text = stringResource(R.string.role),
      style = ClerkMaterialTheme.typography.bodyMedium,
      color = ClerkMaterialTheme.colors.mutedForeground,
    )
    when {
      isLoading ->
        CircularProgressIndicator(
          modifier = Modifier.size(dp20),
          strokeWidth = dp2,
          color = ClerkMaterialTheme.colors.primary,
        )
      roles.isEmpty() ->
        Text(
          text = stringResource(R.string.no_roles_available),
          style = ClerkMaterialTheme.typography.bodyMedium,
          color = ClerkMaterialTheme.colors.mutedForeground,
        )
      else ->
        RoleDropdown(roles = roles, selectedRoleKey = selectedRoleKey, onSelectRole = onSelectRole)
    }
  }
}

@Composable
private fun RoleDropdown(
  roles: List<Role>,
  selectedRoleKey: String?,
  onSelectRole: (String) -> Unit,
) {
  var expanded by rememberSaveable { mutableStateOf(false) }
  val selectedRole = roles.firstOrNull { it.key == selectedRoleKey } ?: roles.first()

  Box {
    Surface(
      modifier = Modifier.height(dp32).clickable { expanded = true },
      shape = ClerkMaterialTheme.shape,
      color = ClerkMaterialTheme.colors.background,
      border = BorderStroke(dp1, ClerkMaterialTheme.computedColors.buttonBorder),
    ) {
      Row(
        modifier = Modifier.padding(start = dp12, top = dp4, end = dp8, bottom = dp4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dp4),
      ) {
        Text(
          text = selectedRole.name,
          style = ClerkMaterialTheme.typography.bodyMedium,
          color = ClerkMaterialTheme.colors.foreground,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Icon(
          modifier = Modifier.size(dp16),
          painter = painterResource(R.drawable.ic_chevron_down),
          contentDescription = null,
          tint = ClerkMaterialTheme.colors.mutedForeground,
        )
      }
    }
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

internal fun parseInviteEmailAddresses(input: String): List<String> {
  return input
    .split(',', ';', '\n', '\t', ' ')
    .map { it.trim() }
    .filter { it.matches(EMAIL_PATTERN) }
    .distinct()
}

private val EMAIL_PATTERN = Regex("^\\S+@\\S+\\.\\S+$")
private const val INVITE_EMAIL_MAX_LINES = 4
