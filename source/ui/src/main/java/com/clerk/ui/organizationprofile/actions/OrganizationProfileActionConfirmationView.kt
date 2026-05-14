@file:Suppress("LongParameterList")

package com.clerk.ui.organizationprofile.actions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.organizations.Organization
import com.clerk.api.organizations.OrganizationMembership
import com.clerk.ui.R
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonConfiguration
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
import com.clerk.ui.core.dimens.dp0
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp18
import com.clerk.ui.core.input.ClerkTextField
import com.clerk.ui.core.scaffold.ClerkThemedProfileScaffold
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
internal fun OrganizationProfileActionConfirmationView(
  action: OrganizationProfileConfirmationAction,
  organization: Organization,
  membership: OrganizationMembership?,
  onBackPressed: () -> Unit,
  onSuccess: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: OrganizationProfileActionConfirmationViewModel = viewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()

  DisposableEffect(action, organization.id) { onDispose { viewModel.reset() } }

  LaunchedEffect(state.isComplete) {
    if (state.isComplete) {
      viewModel.reset()
      onSuccess()
    }
  }

  ClerkThemedProfileScaffold(
    modifier = modifier,
    title = action.title(),
    horizontalPadding = dp0,
    onBackPressed = onBackPressed,
    errorMessage = state.errorMessage,
    onErrorShown = viewModel::clearError,
    content = {
      OrganizationProfileActionConfirmationContent(
        action = action,
        organizationName = organization.name,
        state = state,
        actions =
          OrganizationProfileActionConfirmationActions(
            onConfirmationTextChanged = viewModel::setConfirmationText,
            onConfirm = { viewModel.confirm(action, organization, membership) },
            onCancel = onBackPressed,
          ),
        canConfirm =
          state.canSubmit(organization.name) &&
            (action != OrganizationProfileConfirmationAction.LeaveOrganization ||
              membership != null),
      )
    },
  )
}

@Composable
internal fun OrganizationProfileActionConfirmationContent(
  action: OrganizationProfileConfirmationAction,
  organizationName: String,
  state: OrganizationProfileActionConfirmationState,
  actions: OrganizationProfileActionConfirmationActions,
  modifier: Modifier = Modifier,
  canConfirm: Boolean = state.canSubmit(organizationName),
) {
  ActionFormColumn(modifier = modifier) {
    Text(
      text = action.confirmationMessage(),
      style = ClerkMaterialTheme.typography.bodyMedium,
      color = ClerkMaterialTheme.colors.mutedForeground,
    )
    Text(
      text = stringResource(R.string.organization_action_irreversible),
      style = ClerkMaterialTheme.typography.bodyMedium,
      color = ClerkMaterialTheme.colors.danger,
    )
    Text(
      text = stringResource(R.string.type_organization_name_to_continue, organizationName),
      style = ClerkMaterialTheme.typography.bodyMedium,
      color = ClerkMaterialTheme.colors.foreground,
    )
    ClerkTextField(
      modifier = Modifier.fillMaxWidth(),
      value = state.confirmationText,
      onValueChange = actions.onConfirmationTextChanged,
      label = stringResource(R.string.organization_name),
      leadingIcon = R.drawable.ic_organization,
      enabled = !state.isLoading,
      keyboardOptions =
        KeyboardOptions(capitalization = KeyboardCapitalization.Words, autoCorrectEnabled = false),
    )
    ClerkButton(
      modifier = Modifier.fillMaxWidth(),
      text = action.title(),
      isLoading = state.isLoading,
      isEnabled = canConfirm && !state.isLoading,
      configuration =
        ClerkButtonDefaults.configuration(style = ClerkButtonConfiguration.ButtonStyle.Negative),
      onClick = actions.onConfirm,
    )
    ClerkButton(
      modifier = Modifier.fillMaxWidth(),
      text = stringResource(R.string.cancel),
      isEnabled = !state.isLoading,
      configuration =
        ClerkButtonDefaults.configuration(style = ClerkButtonConfiguration.ButtonStyle.Secondary),
      onClick = actions.onCancel,
    )
  }
}

internal data class OrganizationProfileActionConfirmationActions(
  val onConfirmationTextChanged: (String) -> Unit,
  val onConfirm: () -> Unit,
  val onCancel: () -> Unit,
)

@Composable
private fun OrganizationProfileConfirmationAction.title(): String {
  return when (this) {
    OrganizationProfileConfirmationAction.LeaveOrganization ->
      stringResource(R.string.leave_organization)
    OrganizationProfileConfirmationAction.DeleteOrganization ->
      stringResource(R.string.delete_organization)
  }
}

@Composable
private fun OrganizationProfileConfirmationAction.confirmationMessage(): String {
  return when (this) {
    OrganizationProfileConfirmationAction.LeaveOrganization ->
      stringResource(R.string.leave_organization_confirmation_message)
    OrganizationProfileConfirmationAction.DeleteOrganization ->
      stringResource(R.string.delete_organization_confirmation_message)
  }
}

@Composable
private fun ActionFormColumn(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .widthIn(max = ACTION_CONFIRMATION_MAX_WIDTH)
        .padding(horizontal = dp18, vertical = dp16),
    verticalArrangement = Arrangement.spacedBy(dp16),
  ) {
    content()
  }
}

private val ACTION_CONFIRMATION_MAX_WIDTH = 560.dp
