@file:Suppress("LongMethod", "LongParameterList", "TooManyFunctions")

package com.clerk.ui.organizationprofile.domains

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.clerk.ui.core.dimens.dp6
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.core.extensions.withMediumWeight
import com.clerk.ui.core.input.ClerkTextField
import com.clerk.ui.core.menu.DropDownItem
import com.clerk.ui.core.menu.ItemMoreMenu
import com.clerk.ui.core.scaffold.ClerkThemedProfileScaffold
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.DefaultColors
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
    onErrorShown = viewModel::clearError,
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
  LazyColumn(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = dp16)) {
    if (state.isLoadingInitial) {
      item { LoadingState() }
      return@LazyColumn
    }

    if (!state.canLoadDomains) {
      item { EmptyState(text = stringResource(R.string.no_verified_domains)) }
      return@LazyColumn
    }

    if (state.domains.isEmpty() && !state.canManageDomains) {
      item { EmptyState(text = stringResource(R.string.no_verified_domains)) }
    } else {
      itemsIndexed(state.domains, key = { _, domain -> domain.id }) { index, domain ->
        DomainRow(
          domain = domain,
          canManage = state.canManageDomains,
          showTopDivider = index == 0,
          isLoading = state.activeMutationId != null,
          onVerify = { actions.onShowVerifyEmail(domain) },
          onManage = { actions.onShowEnrollmentMode(domain) },
          onDelete = { actions.onShowDeleteDomain(domain) },
        )
      }
    }

    if (state.canManageDomains) {
      item {
        AddDomainRow(showTopDivider = state.domains.isEmpty(), onClick = actions.onShowAddDomain)
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
    state.enrollmentModeOptions.forEach { option ->
      EnrollmentModeOptionRow(
        mode = option,
        selected = state.selectedEnrollmentMode == option,
        enabled = state.activeMutationId == null,
        onClick = { actions.onSelectEnrollmentMode(option) },
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
private fun AddDomainRow(showTopDivider: Boolean, onClick: () -> Unit) {
  Surface(
    modifier = Modifier.fillMaxWidth().clickable { onClick() },
    color = ClerkMaterialTheme.colors.background,
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      if (showTopDivider) DomainDivider()
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = dp24, vertical = dp16),
        verticalArrangement = Arrangement.spacedBy(dp4),
      ) {
        Text(
          text = stringResource(R.string.add_domain),
          style = ClerkMaterialTheme.typography.bodyLarge.withMediumWeight(),
          color = DefaultColors.clerk.primary ?: ClerkMaterialTheme.colors.primary,
        )
        Text(
          text = stringResource(R.string.add_domain_short_description),
          style = ClerkMaterialTheme.typography.bodyMedium,
          color = ClerkMaterialTheme.colors.mutedForeground,
        )
      }
      DomainDivider()
    }
  }
}

@Composable
private fun DomainRow(
  domain: OrganizationDomain,
  canManage: Boolean,
  showTopDivider: Boolean,
  isLoading: Boolean,
  onVerify: () -> Unit,
  onManage: () -> Unit,
  onDelete: () -> Unit,
) {
  Surface(modifier = Modifier.fillMaxWidth(), color = ClerkMaterialTheme.colors.background) {
    Column(modifier = Modifier.fillMaxWidth()) {
      if (showTopDivider) DomainDivider()
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = dp24, vertical = dp16),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dp16),
      ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dp8)) {
          DomainStatusBadge(domain = domain)
          Text(
            text = domain.name,
            style = ClerkMaterialTheme.typography.bodyLarge,
            color = ClerkMaterialTheme.colors.foreground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
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
      DomainDivider()
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
  val borderColor =
    if (domain.isVerified) ClerkMaterialTheme.computedColors.buttonBorder
    else ClerkMaterialTheme.computedColors.borderWarning
  Surface(
    modifier = Modifier.height(dp18),
    shape = ClerkMaterialTheme.shape,
    color = color,
    border = BorderStroke(dp1, borderColor),
  ) {
    Box(modifier = Modifier.padding(horizontal = dp6), contentAlignment = Alignment.Center) {
      Text(
        text = text,
        style =
          ClerkMaterialTheme.typography.bodySmall.copy(
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
          ),
        color = foreground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
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
private fun DomainDivider() {
  HorizontalDivider(thickness = dp1, color = ClerkMaterialTheme.computedColors.border)
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
    is OrganizationVerifiedDomainsFlow.EnrollmentMode ->
      stringResource(R.string.update_domain_named, flow.domain.name)
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
