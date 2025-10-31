package com.clerk.ui.userprofile.connectedaccount

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.Clerk
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.user.unconnectedProviders
import com.clerk.ui.R
import com.clerk.ui.core.button.social.ClerkSocialRow
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp4
import com.clerk.ui.core.extensions.withSemiBoldWeight
import com.clerk.ui.theme.ClerkMaterialTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Composable
fun UserProfileAddConnectedAccountView(modifier: Modifier = Modifier, onBackPressed: () -> Unit) {
  val unconnectedProviders = Clerk.user?.unconnectedProviders.orEmpty()
  UserProfileAddConnectedAccountViewImpl(
    modifier = modifier,
    unconnectedProviders = unconnectedProviders.toImmutableList(),
    onClosePressed = onBackPressed,
  )
}

@Composable
private fun UserProfileAddConnectedAccountViewImpl(
  unconnectedProviders: ImmutableList<OAuthProvider>,
  modifier: Modifier = Modifier,
  viewModel: AddConnectedAccountViewModel = viewModel(),
  onClosePressed: () -> Unit,
) {
  Column(modifier = Modifier.then(modifier)) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = dp4),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      IconButton(onClick = {}) {}
      Spacer(modifier = Modifier.weight(1f))
      Text(
        modifier = Modifier.padding(bottom = dp12),
        text = stringResource(R.string.connect_account),
        style = ClerkMaterialTheme.typography.titleMedium.withSemiBoldWeight(),
        color = ClerkMaterialTheme.colors.foreground,
      )
      Spacer(modifier = Modifier.weight(1f))
      IconButton(onClosePressed) {
        Icon(
          modifier = Modifier.size(dp24),
          painter = painterResource(R.drawable.ic_cross),
          contentDescription = stringResource(R.string.close),
        )
      }
    }
    HorizontalDivider(thickness = dp1, color = ClerkMaterialTheme.computedColors.border)
    Column(
      modifier = Modifier.fillMaxWidth().padding(dp24),
      verticalArrangement = Arrangement.spacedBy(dp24),
    ) {
      Text(
        text = stringResource(R.string.link_another_login_option),
        style = ClerkMaterialTheme.typography.bodyMedium,
        color = ClerkMaterialTheme.colors.mutedForeground,
      )
      ClerkSocialRow(
        providers = unconnectedProviders,
        onClick = { viewModel.connectExternalAccount(it) },
      )
    }
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  ClerkMaterialTheme {
    Box(modifier = Modifier.background(ClerkMaterialTheme.colors.background)) {
      val unconnectedProviders =
        persistentListOf(
          OAuthProvider.GOOGLE,
          OAuthProvider.FACEBOOK,
          OAuthProvider.APPLE,
          OAuthProvider.BOX,
          OAuthProvider.GITHUB,
        )
      UserProfileAddConnectedAccountViewImpl(
        unconnectedProviders = unconnectedProviders,
        onClosePressed = {},
      )
    }
  }
}
