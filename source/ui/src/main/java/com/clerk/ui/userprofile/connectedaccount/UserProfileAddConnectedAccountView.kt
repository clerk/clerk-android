package com.clerk.ui.userprofile.connectedaccount

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.Clerk
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.user.unconnectedProviders
import com.clerk.ui.R
import com.clerk.ui.core.button.social.ClerkSocialRow
import com.clerk.ui.core.scaffold.ClerkThemedProfileScaffold
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable
fun UserProfileAddConnectedAccountView(modifier: Modifier = Modifier, onBackPressed: () -> Unit) {
  val unconnectedProviders = Clerk.user?.unconnectedProviders.orEmpty()
  UserProfileAddConnectedAccountViewImpl(
    modifier = modifier,
    unconnectedProviders = unconnectedProviders.toImmutableList(),
    onBackPressed = onBackPressed,
  )
}

@Composable
private fun UserProfileAddConnectedAccountViewImpl(
  unconnectedProviders: ImmutableList<OAuthProvider>,
  modifier: Modifier = Modifier,
  viewModel: AddConnectedAccountViewModel = viewModel(),
  onBackPressed: () -> Unit,
) {
  ClerkThemedProfileScaffold(
    modifier = modifier,
    title = stringResource(R.string.connect_account),
    onBackPressed = onBackPressed,
  ) {
    Text(
      text = stringResource(R.string.link_another_login_option),
      style = ClerkMaterialTheme.typography.bodyMedium,
      color = ClerkMaterialTheme.colors.mutedForeground,
    )
    Spacers.Vertical.Spacer24()
    ClerkSocialRow(
      providers = unconnectedProviders,
      onClick = { viewModel.connectExternalAccount(it) },
    )
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  val unconnectedProviders =
    listOf(
      OAuthProvider.GOOGLE,
      OAuthProvider.FACEBOOK,
      OAuthProvider.APPLE,
      OAuthProvider.BOX,
      OAuthProvider.GITHUB,
    )
  UserProfileAddConnectedAccountViewImpl(
    unconnectedProviders = unconnectedProviders.toImmutableList(),
    onBackPressed = {},
  )
}
