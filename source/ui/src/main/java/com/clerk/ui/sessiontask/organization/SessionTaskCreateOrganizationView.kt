package com.clerk.ui.sessiontask.organization

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.Clerk
import com.clerk.api.organizations.OrganizationCreationDefaults
import com.clerk.ui.R
import com.clerk.ui.auth.handleSessionTaskCompletion
import com.clerk.ui.core.composition.LocalAuthState
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp18
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.core.footer.SecuredByClerkView
import com.clerk.ui.core.header.HeaderTextView
import com.clerk.ui.core.header.HeaderType
import com.clerk.ui.organizationprofile.form.OrganizationProfileFormView
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
internal fun SessionTaskCreateOrganizationView(
  creationDefaults: OrganizationCreationDefaults?,
  showBackButton: Boolean,
  onAuthComplete: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: SessionTaskCreateOrganizationViewModel = viewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val authState = LocalAuthState.current
  val defaultName = creationDefaults?.form?.name.orEmpty()
  val defaultSlug = creationDefaults?.form?.slug ?: createOrganizationSlug(defaultName)
  val defaultLogoUrl = creationDefaults?.form?.logo

  LaunchedEffect(state.completedSession) {
    state.completedSession?.let {
      authState.handleSessionTaskCompletion(it, onAuthComplete)
      viewModel.clearCompletedSession()
    }
  }

  SessionTaskOrganizationScaffold(
    modifier = modifier,
    errorMessage = state.errorMessage,
    onErrorShown = viewModel::clearError,
    hasBackButton = showBackButton,
    onBackPressed = { authState.navigateBack() },
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentPadding = PaddingValues(horizontal = dp18, vertical = dp16),
      verticalArrangement = Arrangement.spacedBy(dp24),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      item { CreateOrganizationHeader() }

      creationDefaults?.advisory?.let { advisory -> item { AdvisoryText(advisory = advisory) } }

      item {
        OrganizationProfileFormView(
          initialName = defaultName,
          initialSlug = defaultSlug,
          initialLogoUrl = defaultLogoUrl,
          preloadInitialLogo = true,
          slugEnabled = Clerk.organizationSlugIsEnabled,
          autoGenerateSlug = true,
          submitText = stringResource(R.string.continue_text),
          isLoading = state.isLoading,
          onSubmit = { submit ->
            viewModel.createOrganization(
              name = submit.name,
              slug = submit.slug,
              logoFile = submit.logoFile,
            )
          },
        )
      }

      item { SecuredByClerkView() }
    }
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
