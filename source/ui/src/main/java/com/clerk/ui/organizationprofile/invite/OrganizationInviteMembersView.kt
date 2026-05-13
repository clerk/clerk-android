package com.clerk.ui.organizationprofile.invite

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.organizations.Organization
import com.clerk.api.organizations.Role
import com.clerk.ui.R
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.dimens.dp0
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp18
import com.clerk.ui.core.dimens.dp2
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.core.input.ClerkTextField
import com.clerk.ui.core.scaffold.ClerkThemedProfileScaffold
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

  LaunchedEffect(organization.id) { viewModel.loadRoles(organization) }
  LaunchedEffect(state.completion) {
    if (state.completion != null) {
      viewModel.clearCompletion()
      onComplete()
    }
  }

  ClerkThemedProfileScaffold(
    modifier = modifier,
    title = stringResource(R.string.invite_new_members),
    horizontalPadding = dp0,
    onBackPressed = onComplete,
    errorMessage = state.errorMessage,
    content = {
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = dp18, vertical = dp16),
        verticalArrangement = Arrangement.spacedBy(dp24),
      ) {
        Text(
          text = stringResource(R.string.enter_or_paste_email_addresses),
          style = ClerkMaterialTheme.typography.bodyMedium,
          color = ClerkMaterialTheme.colors.mutedForeground,
        )

        ClerkTextField(
          value = emailInput,
          onValueChange = {
            emailInput = it
            viewModel.clearError()
          },
          label = stringResource(R.string.enter_email_addresses),
          maxLines = INVITE_EMAIL_MAX_LINES,
        )

        RoleSelector(
          roles = state.roles,
          selectedRoleKey = state.selectedRoleKey,
          isLoading = state.isLoadingRoles,
          onSelectRole = viewModel::selectRole,
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
          onClick = {
            viewModel.sendInvitations(
              organization = organization,
              emailAddresses = parseInviteEmailAddresses(emailInput),
            )
          },
        )
      }
    },
  )
}

@Composable
private fun RoleSelector(
  roles: List<Role>,
  selectedRoleKey: String?,
  isLoading: Boolean,
  onSelectRole: (String) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(dp8)) {
    Text(
      text = stringResource(R.string.role),
      style = ClerkMaterialTheme.typography.bodySmall,
      color = ClerkMaterialTheme.colors.mutedForeground,
    )
    when {
      isLoading -> CircularProgressIndicator()
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
      modifier = Modifier.clickable { expanded = true },
      shape = ClerkMaterialTheme.shape,
      color = ClerkMaterialTheme.colors.background,
      border = BorderStroke(dp1, ClerkMaterialTheme.computedColors.buttonBorder),
      shadowElevation = dp2,
    ) {
      Row(
        modifier = Modifier.padding(horizontal = dp12, vertical = dp12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dp12),
      ) {
        Text(
          text = selectedRole.name,
          style = ClerkMaterialTheme.typography.bodyMedium,
          color = ClerkMaterialTheme.colors.foreground,
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
