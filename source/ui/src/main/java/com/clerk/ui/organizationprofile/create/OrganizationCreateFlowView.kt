package com.clerk.ui.organizationprofile.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.Clerk
import com.clerk.api.organizations.Organization
import com.clerk.api.organizations.OrganizationCreationDefaults
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp18
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.core.footer.SecuredByClerkView
import com.clerk.ui.core.header.HeaderTextView
import com.clerk.ui.core.header.HeaderType
import com.clerk.ui.organizationprofile.form.OrganizationProfileFormView
import com.clerk.ui.organizationprofile.invite.OrganizationInviteMembersView
import com.clerk.ui.sessiontask.organization.createOrganizationSlug
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
internal fun OrganizationCreateFlowView(
  creationDefaults: OrganizationCreationDefaults?,
  onComplete: () -> Unit,
  modifier: Modifier = Modifier,
  skipInvitationScreen: Boolean = false,
  viewModel: OrganizationCreateFlowViewModel = viewModel(),
) {
  val state by viewModel.state.collectAsState()
  var inviteOrganization by remember { mutableStateOf<Organization?>(null) }

  LaunchedEffect(state.createdOrganization) {
    val organization = state.createdOrganization ?: return@LaunchedEffect
    viewModel.clearCompletedCreate()
    if (shouldShowPostCreateInviteStep(organization, skipInvitationScreen)) {
      inviteOrganization = organization
    } else {
      onComplete()
    }
  }

  val organizationForInvite = inviteOrganization
  if (organizationForInvite != null) {
    OrganizationInviteMembersView(organization = organizationForInvite, onComplete = onComplete)
  } else {
    CreateOrganizationFormContent(
      modifier = modifier,
      creationDefaults = creationDefaults,
      state = state,
      onSubmit = { submit ->
        viewModel.createOrganization(
          name = submit.name,
          slug = submit.slug,
          logoFile = submit.logoFile,
        )
      },
    )
  }
}

@Composable
private fun CreateOrganizationFormContent(
  creationDefaults: OrganizationCreationDefaults?,
  state: OrganizationCreateFlowState,
  onSubmit: (com.clerk.ui.organizationprofile.form.OrganizationProfileFormSubmit) -> Unit,
  modifier: Modifier = Modifier,
) {
  val defaultName = creationDefaults?.form?.name.orEmpty()
  val defaultSlug = creationDefaults?.form?.slug ?: createOrganizationSlug(defaultName)
  val defaultLogoUrl = creationDefaults?.form?.logo

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = dp18, vertical = dp16),
    verticalArrangement = Arrangement.spacedBy(dp24),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    item { CreateOrganizationHeader() }
    creationDefaults?.advisory?.let { advisory -> item { AdvisoryText(advisory = advisory) } }
    state.errorMessage?.let { errorMessage ->
      item { CreateOrganizationErrorText(errorMessage = errorMessage) }
    }
    item {
      OrganizationProfileFormView(
        initialName = defaultName,
        initialSlug = defaultSlug,
        initialLogoUrl = defaultLogoUrl,
        preloadInitialLogo = true,
        slugEnabled = Clerk.organizationSlugIsEnabled,
        autoGenerateSlug = true,
        submitText = stringResource(R.string.create_organization),
        isLoading = state.isLoading,
        onSubmit = onSubmit,
      )
    }
    item { SecuredByClerkView() }
  }
}

@Composable
private fun CreateOrganizationHeader() {
  Column(
    modifier = Modifier.fillMaxWidth().padding(bottom = dp8),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(dp8),
  ) {
    HeaderTextView(text = stringResource(R.string.create_organization), type = HeaderType.Title)
    HeaderTextView(
      text = stringResource(R.string.enter_your_organization_details_to_continue),
      type = HeaderType.Subtitle,
    )
  }
}

@Composable
private fun CreateOrganizationErrorText(errorMessage: String) {
  Text(
    modifier = Modifier.fillMaxWidth(),
    text = errorMessage,
    style = ClerkMaterialTheme.typography.bodySmall,
    color = ClerkMaterialTheme.colors.danger,
  )
}

@Composable
private fun AdvisoryText(advisory: OrganizationCreationDefaults.Advisory) {
  val message =
    when (advisory.code) {
      "organization_already_exists" ->
        stringResource(
          R.string.organization_already_exists_advisory,
          advisory.meta["organization_name"].orEmpty(),
          advisory.meta["organization_domain"].orEmpty(),
        )
      else -> null
    }

  message?.let {
    Text(
      modifier = Modifier.fillMaxWidth(),
      text = it,
      style = ClerkMaterialTheme.typography.bodySmall,
      color = ClerkMaterialTheme.colors.warning,
    )
  }
}

private fun shouldShowPostCreateInviteStep(
  organization: Organization,
  skipInvitationScreen: Boolean,
): Boolean = !skipInvitationScreen && organization.maxAllowedMemberships != 1
