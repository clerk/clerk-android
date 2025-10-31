package com.clerk.ui.userprofile.connectedaccount

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import coil3.compose.AsyncImage
import com.clerk.api.externalaccount.ExternalAccount
import com.clerk.api.externalaccount.oauthProviderType
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.sso.logoUrl
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp20
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.core.menu.DropDownItem
import com.clerk.ui.core.menu.ItemMoreMenu
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme
import kotlinx.collections.immutable.persistentListOf

@Composable
fun UserProfileExternalAccountRow(externalAccount: ExternalAccount, modifier: Modifier = Modifier) {
  val isPreview = LocalInspectionMode.current
  ClerkMaterialTheme {
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .background(ClerkMaterialTheme.colors.background)
          .padding(horizontal = dp24, vertical = dp8)
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
              ExternalAccountAction.Reconnect -> TODO()
              ExternalAccountAction.Remove -> TODO()
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
        model = externalAccount.oauthProviderType.logoUrl,
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
    Spacers.Vertical.Spacer4()
    Text(
      text = externalAccount.emailAddress,
      color = ClerkMaterialTheme.colors.foreground,
      style = ClerkMaterialTheme.typography.bodyLarge,
    )
  }
}

enum class ExternalAccountAction {
  Reconnect,
  Remove,
}

@PreviewLightDark
@Composable
private fun Preview() {
  ClerkMaterialTheme {
    UserProfileExternalAccountRow(
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
        )
    )
  }
}
