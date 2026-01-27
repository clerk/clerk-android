package com.clerk.ui.userprofile.connectedaccount

import android.annotation.SuppressLint
import android.app.Activity
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.clerk.api.externalaccount.ExternalAccount
import com.clerk.api.externalaccount.oauthProviderType
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.sso.logoUrl
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp2
import com.clerk.ui.core.dimens.dp20
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.core.extensions.withDarkVariant
import com.clerk.ui.core.menu.DropDownItem
import com.clerk.ui.core.menu.ItemMoreMenu
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme
import kotlinx.collections.immutable.persistentListOf

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
internal fun UserProfileExternalAccountRow(
  externalAccount: ExternalAccount,
  modifier: Modifier = Modifier,
  viewModel: AddConnectedAccountViewModel = viewModel(),
  onError: (String) -> Unit,
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val activity = context as Activity
  LaunchedEffect(state) {
    if (state is AddConnectedAccountViewModel.State.Error) {
      onError(
        (state as AddConnectedAccountViewModel.State.Error).message
          ?: context.getString(R.string.something_went_wrong_please_try_again)
      )
      viewModel.resetState()
    }
  }
  val isPreview = LocalInspectionMode.current
  ClerkMaterialTheme {
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .background(ClerkMaterialTheme.colors.background)
          .padding(start = dp24)
          .padding(vertical = dp16)
          .then(modifier),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      EmailWithAccountBadge(externalAccount)
      Spacer(modifier = Modifier.weight(1f))
      if (!isPreview) {
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
                viewModel.connectExternalAccount(externalAccount.oauthProviderType, activity)
              ExternalAccountAction.Remove -> viewModel.removeConnectedAccount(externalAccount)
            }
          },
        )
      }
    }
  }
}

@Composable
private fun EmailWithAccountBadge(externalAccount: ExternalAccount) {
  val fallbackPainter =
    if (externalAccount.oauthProviderType == OAuthProvider.GOOGLE)
      painterResource(R.drawable.ic_google)
    else painterResource(R.drawable.ic_globe)
  Column {
    Row(horizontalArrangement = Arrangement.spacedBy(dp8)) {
      AsyncImage(
        modifier = Modifier.size(dp20),
        model = externalAccount.oauthProviderType.logoUrl?.withDarkVariant(isSystemInDarkTheme()),
        contentDescription = null,
        fallback = fallbackPainter,
      )
      Text(
        text =
          externalAccount.oauthProviderType.name.lowercase().replaceFirstChar { it.titlecase() },
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

internal enum class ExternalAccountAction {
  Reconnect,
  Remove,
}

@PreviewLightDark
@Composable
private fun PreviewWithError() {
  ClerkMaterialTheme {
    UserProfileExternalAccountRow(
      onError = {},
      externalAccount =
        ExternalAccount(
          id = "eac_34o5pCBEhohJtr1Ni14YiX8aQ0K",
          identificationId = "idn_34o5pAvdtMtjAAdeFBfTkRfs77e",
          provider = "oauth_google",
          providerUserId = "102662613248529322762",
          emailAddress = "sam@clerk.dev",
          approvedScopes =
            "email https://www.googleapis.com/auth/userinfo.email" +
              " https://www.googleapis.com/auth/userinfo.profile openid profile",
          createdAt = 1L,
          verification =
            Verification(
              error =
                Error(
                  "This email address associated with this OAuth account is already claimed by another user."
                )
            ),
        ),
    )
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  ClerkMaterialTheme {
    UserProfileExternalAccountRow(
      onError = {},
      externalAccount =
        ExternalAccount(
          id = "eac_34o5pCBEhohJtr1Ni14YiX8aQ0K",
          identificationId = "idn_34o5pAvdtMtjAAdeFBfTkRfs77e",
          provider = "oauth_google",
          providerUserId = "102662613248529322762",
          emailAddress = "sam@clerk.dev",
          approvedScopes =
            "email https://www.googleapis.com/auth/userinfo.email" +
              " https://www.googleapis.com/auth/userinfo.profile openid profile",
          createdAt = 1L,
        ),
    )
  }
}
