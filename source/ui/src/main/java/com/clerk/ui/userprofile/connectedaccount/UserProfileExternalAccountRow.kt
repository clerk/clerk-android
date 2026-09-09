package com.clerk.ui.userprofile.connectedaccount

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.clerk.api.ExternalAccount
import com.clerk.api.OAuthProvider
import com.clerk.ui.R
import com.clerk.ui.core.composition.clerkViewModel
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp2
import com.clerk.ui.core.dimens.dp20
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp48
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.core.extensions.logoUrl
import com.clerk.ui.core.extensions.providerName
import com.clerk.ui.core.extensions.withDarkVariant
import com.clerk.ui.core.menu.DropDownItem
import com.clerk.ui.core.menu.ItemMoreMenu
import com.clerk.ui.core.preview.ClerkPreview
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme
import kotlinx.collections.immutable.persistentListOf

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
internal fun UserProfileExternalAccountRow(
  externalAccount: ExternalAccount,
  modifier: Modifier = Modifier,
  isInteractive: Boolean = true,
  viewModel: AddConnectedAccountViewModel? =
    if (isInteractive) clerkViewModel { AddConnectedAccountViewModel(it) } else null,
  loadRemoteLogo: Boolean = true,
  onError: (String) -> Unit,
) {
  if (isInteractive && viewModel != null) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(state) {
      if (state is AddConnectedAccountViewModel.State.Error) {
        onError(
          (state as AddConnectedAccountViewModel.State.Error).message
            ?: context.getString(R.string.something_went_wrong_please_try_again)
        )
        viewModel.resetState()
      }
    }
  }
  val isPreview = LocalInspectionMode.current
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .background(ClerkMaterialTheme.colors.background)
        .padding(start = dp24)
        .padding(vertical = dp16)
        .then(modifier),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    EmailWithAccountBadge(externalAccount, loadRemoteLogo = loadRemoteLogo)
    Spacer(modifier = Modifier.weight(1f))
    if (!isPreview) {
      if (isInteractive && viewModel != null) {
        ItemMoreMenu(
          dropDownItems =
            persistentListOf(
              DropDownItem(
                id = ExternalAccountAction.Reconnect,
                text = stringResource(R.string.reconnect),
                isHidden = externalAccount.verification?.error == null,
              ),
              DropDownItem(
                id = ExternalAccountAction.Remove,
                text = stringResource(R.string.remove_connection),
                danger = true,
              ),
            ),
          onClick = {
            when (it) {
              ExternalAccountAction.Reconnect ->
                viewModel.connectExternalAccount(externalAccount.provider)
              ExternalAccountAction.Remove -> viewModel.removeConnectedAccount(externalAccount)
            }
          },
        )
      } else {
        Spacer(modifier = Modifier.size(dp48))
      }
    }
  }
}

@Composable
private fun EmailWithAccountBadge(externalAccount: ExternalAccount, loadRemoteLogo: Boolean) {
  val fallbackPainter =
    if (externalAccount.provider == OAuthProvider.Google) painterResource(R.drawable.ic_google)
    else painterResource(R.drawable.ic_globe)
  Column {
    Row(horizontalArrangement = Arrangement.spacedBy(dp8)) {
      ExternalAccountLogo(
        externalAccount = externalAccount,
        loadRemoteLogo = loadRemoteLogo,
        fallbackPainter = fallbackPainter,
      )
      Text(
        text = externalAccount.provider.providerName,
        color = ClerkMaterialTheme.colors.mutedForeground,
        style = ClerkMaterialTheme.typography.bodyMedium,
      )
    }
    Spacers.Vertical.Spacer8()
    if (externalAccount.verification?.error != null) {
      Row(horizontalArrangement = Arrangement.spacedBy(dp8)) {
        Box(
          modifier =
            Modifier.size(dp20)
              .background(
                color = ClerkMaterialTheme.computedColors.backgroundDanger,
                shape = RoundedCornerShape(dp2),
              ),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            modifier = Modifier.size(dp16),
            painter = painterResource(R.drawable.ic_warning),
            contentDescription = null,
            tint = ClerkMaterialTheme.colors.warning,
          )
        }
        externalAccount.verification?.error?.message?.let {
          Text(
            text = it,
            color = ClerkMaterialTheme.colors.danger,
            style = ClerkMaterialTheme.typography.bodyMedium,
          )
        }
      }
    } else {
      Text(
        text = externalAccount.emailAddress,
        color = ClerkMaterialTheme.colors.foreground,
        style = ClerkMaterialTheme.typography.bodyLarge,
      )
    }
  }
}

@Composable
private fun ExternalAccountLogo(
  externalAccount: ExternalAccount,
  loadRemoteLogo: Boolean,
  fallbackPainter: androidx.compose.ui.graphics.painter.Painter,
) {
  if (loadRemoteLogo) {
    AsyncImage(
      modifier = Modifier.size(dp20),
      model = externalAccountLogoModel(externalAccount),
      contentDescription = null,
      fallback = fallbackPainter,
    )
  } else {
    Image(modifier = Modifier.size(dp20), painter = fallbackPainter, contentDescription = null)
  }
}

@Composable
private fun externalAccountLogoModel(externalAccount: ExternalAccount): String? =
  externalAccount.provider.logoUrl?.withDarkVariant(isSystemInDarkTheme())

internal enum class ExternalAccountAction {
  Reconnect,
  Remove,
}

@PreviewLightDark
@Composable
private fun PreviewWithError() {
  ClerkPreview { clerk ->
    ClerkMaterialTheme {
      UserProfileExternalAccountRow(
        onError = {},
        externalAccount = clerk.user!!.externalAccounts[0],
      )
    }
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  ClerkPreview { clerk ->
    ClerkMaterialTheme {
      UserProfileExternalAccountRow(
        onError = {},
        externalAccount = clerk.user!!.externalAccounts[0],
      )
    }
  }
}
