@file:Suppress("LongMethod", "LongParameterList", "TooManyFunctions")

package com.clerk.ui.organizationprofile.domains

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.Clerk
import com.clerk.api.organizations.Organization
import com.clerk.api.organizations.OrganizationDomain
import com.clerk.api.organizations.OrganizationMembership
import com.clerk.ui.R
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
internal fun OrganizationVerifiedDomainsView(
  organization: Organization,
  membership: OrganizationMembership?,
  onBackPressed: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: OrganizationVerifiedDomainsViewModel = viewModel(),
) {
  val state by viewModel.state.collectAsState()

  LaunchedEffect(organization.id, membership?.id) {
    viewModel.load(
      organization = organization,
      membership = membership,
      domainsEnabled = Clerk.organizationDomainsIsEnabled,
      enrollmentModes = Clerk.organizationDomainEnrollmentModes,
    )
  }

  ClerkThemedProfileScaffold(
    modifier = modifier,
    title = organizationVerifiedDomainsTitle(state.flow),
    horizontalPadding = dp0,
    onBackPressed = {
      if (state.flow == OrganizationVerifiedDomainsFlow.DomainsList) {
        onBackPressed()
      } else {
        viewModel.dismissFlow()
      }
    },
    errorMessage = state.errorMessage,
    content = {
      OrganizationVerifiedDomainsContent(
        state = state,
        actions =
          OrganizationVerifiedDomainsActions(
            onRetry = viewModel::retry,
            onLoadMore = viewModel::loadMoreDomains,
            onShowAddDomain = viewModel::showAddDomain,
            onShowVerifyEmail = viewModel::showVerifyEmail,
            onShowEnrollmentMode = viewModel::showEnrollmentMode,
            onShowDeleteDomain = viewModel::showDeleteDomain,
            onDismissFlow = viewModel::dismissFlow,
            onDomainNameChanged = viewModel::setDomainName,
            onCreateDomain = viewModel::createDomain,
            onAffiliationEmailLocalPartChanged = viewModel::setAffiliationEmailLocalPart,
            onSendAffiliationEmail = viewModel::sendAffiliationEmail,
            onVerificationCodeChanged = viewModel::setVerificationCode,
            onVerifyCode = viewModel::verifyCode,
            onResendVerificationCode = viewModel::resendVerificationCode,
            onSelectEnrollmentMode = viewModel::selectEnrollmentMode,
            onDeletePendingChanged = viewModel::setDeletePending,
            onUpdateEnrollmentMode = viewModel::updateEnrollmentMode,
            onDeleteDomain = viewModel::deleteDomain,
          ),
      )
    },
  )
}

@Composable
internal fun OrganizationVerifiedDomainsContent(
  state: OrganizationVerifiedDomainsState,
  actions: OrganizationVerifiedDomainsActions,
  modifier: Modifier = Modifier,
) {
  when (val flow = state.flow) {
    OrganizationVerifiedDomainsFlow.DomainsList ->
      OrganizationDomainsList(state = state, actions = actions, modifier = modifier)
    OrganizationVerifiedDomainsFlow.AddDomain ->
      AddDomainContent(state = state, actions = actions, modifier = modifier)
    is OrganizationVerifiedDomainsFlow.VerifyEmail ->
      VerifyEmailContent(
        domain = flow.domain,
        state = state,
        actions = actions,
        modifier = modifier,
      )
    is OrganizationVerifiedDomainsFlow.VerifyCode ->
      VerifyCodeContent(
        domain = flow.domain,
        emailAddress = flow.emailAddress,
        state = state,
        actions = actions,
        modifier = modifier,
      )
    is OrganizationVerifiedDomainsFlow.EnrollmentMode ->
      EnrollmentModeContent(
        domain = flow.domain,
        state = state,
        actions = actions,
        modifier = modifier,
      )
    is OrganizationVerifiedDomainsFlow.DeleteDomain ->
      DeleteDomainContent(
        domain = flow.domain,
        state = state,
        actions = actions,
        modifier = modifier,
      )
  }
}

@Composable
private fun OrganizationDomainsList(
  state: OrganizationVerifiedDomainsState,
  actions: OrganizationVerifiedDomainsActions,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = dp18, vertical = dp16),
    verticalArrangement = Arrangement.spacedBy(dp16),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    if (state.isLoadingInitial) {
      item { LoadingState() }
      return@LazyColumn
    }

    if (!state.canLoadDomains) {
      item { EmptyState(text = stringResource(R.string.no_verified_domains)) }
      return@LazyColumn
    }

    state.errorMessage?.let { message -> item { ErrorNotice(text = message) } }

    if (state.canManageDomains) {
      item { AddDomainRow(onClick = actions.onShowAddDomain) }
    }

    if (state.domains.isEmpty()) {
      item { EmptyState(text = stringResource(R.string.no_verified_domains)) }
    } else {
      items(state.domains, key = { it.id }) { domain ->
        DomainRow(
          domain = domain,
          canManage = state.canManageDomains,
          isLoading = state.activeMutationId != null,
          onVerify = { actions.onShowVerifyEmail(domain) },
          onManage = { actions.onShowEnrollmentMode(domain) },
          onDelete = { actions.onShowDeleteDomain(domain) },
        )
      }
    }

    if (state.hasNextPage) {
      item { LoadMoreButton(isLoading = state.isLoadingMore, onClick = actions.onLoadMore) }
    }
  }
}

@Composable
private fun AddDomainContent(
  state: OrganizationVerifiedDomainsState,
  actions: OrganizationVerifiedDomainsActions,
  modifier: Modifier = Modifier,
) {
  FormColumn(modifier = modifier) {
    Text(
      text = stringResource(R.string.add_domain_description),
      style = ClerkMaterialTheme.typography.bodyMedium,
      color = ClerkMaterialTheme.colors.mutedForeground,
    )
    state.errorMessage?.let { ErrorNotice(text = it) }
    ClerkTextField(
      modifier = Modifier.fillMaxWidth(),
      value = state.domainName,
      onValueChange = actions.onDomainNameChanged,
      label = stringResource(R.string.domain),
      leadingIcon = R.drawable.ic_globe,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
    )
    ClerkButton(
      modifier = Modifier.fillMaxWidth(),
      text = stringResource(R.string.save),
      isLoading = state.activeMutationId == CREATE_DOMAIN_MUTATION_ID,
      isEnabled = state.canCreateDomain,
      onClick = actions.onCreateDomain,
    )
  }
}

@Composable
private fun VerifyEmailContent(
  domain: OrganizationDomain,
  state: OrganizationVerifiedDomainsState,
  actions: OrganizationVerifiedDomainsActions,
  modifier: Modifier = Modifier,
) {
  FormColumn(modifier = modifier) {
    Text(
      text = stringResource(R.string.organization_domain_needs_email_verification, domain.name),
      style = ClerkMaterialTheme.typography.bodyMedium,
      color = ClerkMaterialTheme.colors.mutedForeground,
    )
    state.errorMessage?.let { ErrorNotice(text = it) }
    ClerkTextField(
      modifier = Modifier.fillMaxWidth(),
      value = state.affiliationEmailLocalPart,
      onValueChange = actions.onAffiliationEmailLocalPartChanged,
      label = stringResource(R.string.verification_email_address),
      leadingIcon = R.drawable.ic_email,
      supportingText = "@${domain.name}",
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
    )
    ClerkButton(
      modifier = Modifier.fillMaxWidth(),
      text = stringResource(R.string.send_code),
      isLoading = state.activeMutationId == SEND_CODE_MUTATION_ID,
      isEnabled = state.canSendVerificationEmail,
      onClick = { actions.onSendAffiliationEmail(domain) },
    )
  }
}

@Composable
private fun VerifyCodeContent(
  domain: OrganizationDomain,
  emailAddress: String,
  state: OrganizationVerifiedDomainsState,
  actions: OrganizationVerifiedDomainsActions,
  modifier: Modifier = Modifier,
) {
  FormColumn(modifier = modifier) {
    Text(
      text = stringResource(R.string.organization_domain_code_sent_to, emailAddress),
      style = ClerkMaterialTheme.typography.bodyMedium,
      color = ClerkMaterialTheme.colors.mutedForeground,
    )
    state.errorMessage?.let { ErrorNotice(text = it) }
    ClerkTextField(
      modifier = Modifier.fillMaxWidth(),
      value = state.verificationCode,
      onValueChange = actions.onVerificationCodeChanged,
      label = stringResource(R.string.verification_code),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
    )
    ClerkButton(
      modifier = Modifier.fillMaxWidth(),
      text = stringResource(R.string.verify),
      isLoading = state.activeMutationId == VERIFY_CODE_MUTATION_ID,
      isEnabled = state.canVerifyCode,
      onClick = { actions.onVerifyCode(domain) },
    )
    ClerkButton(
      modifier = Modifier.fillMaxWidth(),
      text = stringResource(R.string.resend),
      isLoading = state.activeMutationId == SEND_CODE_MUTATION_ID,
      isEnabled = state.activeMutationId == null,
      configuration =
        ClerkButtonDefaults.configuration(style = ClerkButtonConfiguration.ButtonStyle.Secondary),
      onClick = { actions.onResendVerificationCode(domain, emailAddress) },
    )
  }
}

@Composable
private fun EnrollmentModeContent(
  domain: OrganizationDomain,
  state: OrganizationVerifiedDomainsState,
  actions: OrganizationVerifiedDomainsActions,
  modifier: Modifier = Modifier,
) {
  FormColumn(modifier = modifier) {
    Text(
      text = stringResource(R.string.organization_domain_enrollment_description),
      style = ClerkMaterialTheme.typography.bodyMedium,
      color = ClerkMaterialTheme.colors.mutedForeground,
    )
    state.errorMessage?.let { ErrorNotice(text = it) }
    state.enrollmentModeOptions.forEach { option ->
      EnrollmentModeOptionRow(
        mode = option,
        selected = state.selectedEnrollmentMode == option,
        enabled = state.activeMutationId == null,
        onClick = { actions.onSelectEnrollmentMode(option) },
      )
    }
    if (state.isManualInvitationSelected) {
      DeletePendingRow(
        checked = state.deletePending,
        enabled = state.activeMutationId == null,
        onCheckedChange = actions.onDeletePendingChanged,
      )
    }
    ClerkButton(
      modifier = Modifier.fillMaxWidth(),
      text = stringResource(R.string.save),
      isLoading = state.activeMutationId == UPDATE_ENROLLMENT_MUTATION_ID,
      isEnabled = state.canUpdateEnrollmentMode,
      onClick = { actions.onUpdateEnrollmentMode(domain) },
    )
  }
}

@Composable
private fun DeleteDomainContent(
  domain: OrganizationDomain,
  state: OrganizationVerifiedDomainsState,
  actions: OrganizationVerifiedDomainsActions,
  modifier: Modifier = Modifier,
) {
  FormColumn(modifier = modifier) {
    Text(
      text = stringResource(R.string.organization_domain_will_be_removed, domain.name),
      style = ClerkMaterialTheme.typography.bodyMedium.withMediumWeight(),
      color = ClerkMaterialTheme.colors.danger,
    )
    Text(
      text = stringResource(R.string.organization_domain_auto_join_removed),
      style = ClerkMaterialTheme.typography.bodyMedium,
      color = ClerkMaterialTheme.colors.mutedForeground,
    )
    state.errorMessage?.let { ErrorNotice(text = it) }
    ClerkButton(
      modifier = Modifier.fillMaxWidth(),
      text = stringResource(R.string.remove),
      isLoading = state.activeMutationId == DELETE_DOMAIN_MUTATION_ID,
      isEnabled = state.activeMutationId == null,
      configuration =
        ClerkButtonDefaults.configuration(style = ClerkButtonConfiguration.ButtonStyle.Negative),
      onClick = { actions.onDeleteDomain(domain) },
    )
    ClerkButton(
      modifier = Modifier.fillMaxWidth(),
      text = stringResource(R.string.cancel),
      isEnabled = state.activeMutationId == null,
      configuration =
        ClerkButtonDefaults.configuration(style = ClerkButtonConfiguration.ButtonStyle.Secondary),
      onClick = actions.onDismissFlow,
    )
  }
}

@Composable
private fun AddDomainRow(onClick: () -> Unit) {
  ListCard(modifier = Modifier.clickable { onClick() }) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(dp12),
    ) {
      IconSurface(icon = R.drawable.ic_plus)
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dp4)) {
        Text(
          text = stringResource(R.string.add_domain),
          style = ClerkMaterialTheme.typography.bodyMedium.withMediumWeight(),
          color = ClerkMaterialTheme.colors.foreground,
        )
        Text(
          text = stringResource(R.string.add_domain_short_description),
          style = ClerkMaterialTheme.typography.bodySmall,
          color = ClerkMaterialTheme.colors.mutedForeground,
        )
      }
    }
  }
}

@Composable
private fun DomainRow(
  domain: OrganizationDomain,
  canManage: Boolean,
  isLoading: Boolean,
  onVerify: () -> Unit,
  onManage: () -> Unit,
  onDelete: () -> Unit,
) {
  ListCard {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(dp12),
    ) {
      IconSurface(icon = R.drawable.ic_globe)
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dp4)) {
        Text(
          text = domain.name,
          style = ClerkMaterialTheme.typography.bodyMedium.withMediumWeight(),
          color = ClerkMaterialTheme.colors.foreground,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        DomainStatusBadge(domain = domain)
      }
      if (canManage) {
        DomainMoreMenu(
          domain = domain,
          enabled = !isLoading,
          onVerify = onVerify,
          onManage = onManage,
          onDelete = onDelete,
        )
      }
    }
  }
}

@Composable
private fun DomainMoreMenu(
  domain: OrganizationDomain,
  enabled: Boolean,
  onVerify: () -> Unit,
  onManage: () -> Unit,
  onDelete: () -> Unit,
) {
  val primaryAction = if (domain.isVerified) DomainAction.Manage else DomainAction.Verify
  val primaryText =
    if (domain.isVerified) stringResource(R.string.manage) else stringResource(R.string.verify)
  ItemMoreMenu(
    dropDownItems =
      listOf(
          DropDownItem(id = primaryAction, text = primaryText, enabled = enabled),
          DropDownItem(
            id = DomainAction.Delete,
            text = stringResource(R.string.delete_domain),
            danger = true,
            enabled = enabled,
          ),
        )
        .toImmutableList(),
    onClick = { action ->
      when (action) {
        DomainAction.Manage -> onManage()
        DomainAction.Verify -> onVerify()
        DomainAction.Delete -> onDelete()
      }
    },
  )
}

@Composable
private fun DomainStatusBadge(domain: OrganizationDomain) {
  val text =
    if (!domain.isVerified) {
      stringResource(R.string.unverified)
    } else {
      when (domain.enrollmentModeType) {
        OrganizationDomain.EnrollmentMode.ManualInvitation ->
          stringResource(R.string.no_automatic_enrollment)
        OrganizationDomain.EnrollmentMode.AutomaticInvitation ->
          stringResource(R.string.automatic_invitations)
        OrganizationDomain.EnrollmentMode.AutomaticSuggestion ->
          stringResource(R.string.automatic_suggestions)
        is OrganizationDomain.EnrollmentMode.Unknown -> domain.enrollmentMode
      }
    }
  val color =
    if (domain.isVerified) ClerkMaterialTheme.colors.muted
    else ClerkMaterialTheme.colors.warning.copy(alpha = 0.12f)
  val foreground =
    if (domain.isVerified) ClerkMaterialTheme.colors.mutedForeground
    else ClerkMaterialTheme.colors.warning
  Surface(shape = ClerkMaterialTheme.shape, color = color) {
    Text(
      modifier = Modifier.padding(horizontal = dp8, vertical = dp4),
      text = text,
      style = ClerkMaterialTheme.typography.bodySmall,
      color = foreground,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun EnrollmentModeOptionRow(
  mode: OrganizationDomain.EnrollmentMode,
  selected: Boolean,
  enabled: Boolean,
  onClick: () -> Unit,
) {
  ListCard(modifier = Modifier.clickable(enabled = enabled) { onClick() }) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.Top,
      horizontalArrangement = Arrangement.spacedBy(dp8),
    ) {
      RadioButton(selected = selected, enabled = enabled, onClick = onClick)
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dp4)) {
        Text(
          text = mode.title(),
          style = ClerkMaterialTheme.typography.bodyMedium.withMediumWeight(),
          color = ClerkMaterialTheme.colors.foreground,
        )
        Text(
          text = mode.description(),
          style = ClerkMaterialTheme.typography.bodySmall,
          color = ClerkMaterialTheme.colors.mutedForeground,
        )
      }
    }
  }
}

@Composable
private fun DeletePendingRow(
  checked: Boolean,
  enabled: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  ListCard(modifier = Modifier.clickable(enabled = enabled) { onCheckedChange(!checked) }) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(dp8),
    ) {
      Checkbox(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
      Text(
        modifier = Modifier.weight(1f),
        text = stringResource(R.string.delete_pending_invitations_and_suggestions),
        style = ClerkMaterialTheme.typography.bodyMedium,
        color = ClerkMaterialTheme.colors.foreground,
      )
    }
  }
}

@Composable
private fun IconSurface(icon: Int) {
  Surface(
    modifier = Modifier.size(dp48),
    shape = ClerkMaterialTheme.shape,
    color = ClerkMaterialTheme.colors.muted,
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        modifier = Modifier.size(dp24),
        painter = painterResource(icon),
        contentDescription = null,
        tint = ClerkMaterialTheme.colors.mutedForeground,
      )
    }
  }
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
private fun ErrorNotice(text: String) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = ClerkMaterialTheme.shape,
    color = ClerkMaterialTheme.colors.danger.copy(alpha = 0.08f),
    border = BorderStroke(dp1, ClerkMaterialTheme.colors.danger.copy(alpha = 0.2f)),
  ) {
    Text(
      modifier = Modifier.padding(dp12),
      text = text,
      style = ClerkMaterialTheme.typography.bodySmall,
      color = ClerkMaterialTheme.colors.danger,
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
private fun FormColumn(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .widthIn(max = FORM_MAX_WIDTH)
        .padding(horizontal = dp18, vertical = dp16),
    verticalArrangement = Arrangement.spacedBy(dp16),
  ) {
    content()
  }
}

@Composable
private fun organizationVerifiedDomainsTitle(flow: OrganizationVerifiedDomainsFlow): String {
  return when (flow) {
    OrganizationVerifiedDomainsFlow.DomainsList -> stringResource(R.string.verified_domains)
    OrganizationVerifiedDomainsFlow.AddDomain -> stringResource(R.string.add_domain)
    is OrganizationVerifiedDomainsFlow.VerifyEmail -> stringResource(R.string.verify_domain)
    is OrganizationVerifiedDomainsFlow.VerifyCode -> stringResource(R.string.verify_domain)
    is OrganizationVerifiedDomainsFlow.EnrollmentMode -> stringResource(R.string.update_domain)
    is OrganizationVerifiedDomainsFlow.DeleteDomain -> stringResource(R.string.remove_domain)
  }
}

@Composable
private fun OrganizationDomain.EnrollmentMode.title(): String {
  return when (this) {
    OrganizationDomain.EnrollmentMode.ManualInvitation ->
      stringResource(R.string.no_automatic_enrollment)
    OrganizationDomain.EnrollmentMode.AutomaticInvitation ->
      stringResource(R.string.automatic_invitations)
    OrganizationDomain.EnrollmentMode.AutomaticSuggestion ->
      stringResource(R.string.automatic_suggestions)
    is OrganizationDomain.EnrollmentMode.Unknown -> rawValue
  }
}

@Composable
private fun OrganizationDomain.EnrollmentMode.description(): String {
  return when (this) {
    OrganizationDomain.EnrollmentMode.ManualInvitation ->
      stringResource(R.string.no_automatic_enrollment_description)
    OrganizationDomain.EnrollmentMode.AutomaticInvitation ->
      stringResource(R.string.automatic_invitations_description)
    OrganizationDomain.EnrollmentMode.AutomaticSuggestion ->
      stringResource(R.string.automatic_suggestions_description)
    is OrganizationDomain.EnrollmentMode.Unknown -> rawValue
  }
}

private enum class DomainAction {
  Manage,
  Verify,
  Delete,
}

private val FORM_MAX_WIDTH = 560.dp
