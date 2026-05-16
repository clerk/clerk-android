package com.clerk.ui.organizationprofile.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.Clerk
import com.clerk.api.organizations.Organization
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp0
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp18
import com.clerk.ui.core.scaffold.ClerkThemedProfileScaffold
import com.clerk.ui.organizationprofile.form.OrganizationProfileFormView
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
internal fun OrganizationProfileUpdateProfileView(
  organization: Organization,
  onBackPressed: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: OrganizationProfileUpdateViewModel = viewModel(),
) {
  val state by viewModel.state.collectAsState()
  val errorMessage = (state as? OrganizationProfileUpdateViewModel.State.Error)?.message

  LaunchedEffect(state) {
    if (state is OrganizationProfileUpdateViewModel.State.Success) {
      viewModel.reset()
      onBackPressed()
    }
  }

  ClerkMaterialTheme {
    ClerkThemedProfileScaffold(
      modifier = modifier,
      title = stringResource(R.string.update_profile),
      horizontalPadding = dp0,
      onBackPressed = onBackPressed,
      errorMessage = errorMessage,
      content = {
        Column(
          modifier = Modifier.fillMaxWidth().padding(horizontal = dp18, vertical = dp16),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          OrganizationProfileFormView(
            initialName = organization.name,
            initialSlug = organization.slug,
            initialLogoUrl = organization.imageUrl,
            initialHasLogo = organization.hasImage,
            slugEnabled = Clerk.organizationSlugIsEnabled,
            submitText = stringResource(R.string.save),
            isLoading = state is OrganizationProfileUpdateViewModel.State.Loading,
            onSubmit = { submit ->
              viewModel.save(
                organization = organization,
                name = submit.name,
                slug = submit.slug,
                logoFile = submit.logoFile,
                removeLogo = submit.removeLogo,
              )
            },
          )
        }
      },
    )
  }
}
