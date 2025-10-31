package com.clerk.ui.userprofile.connectedaccount

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.externalaccount.ExternalAccount
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.extensions.withMediumWeight
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.userprofile.common.UserProfileButtonRow
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun UserProfileExternalAccountSection(
  externalAccounts: ImmutableList<ExternalAccount>,
  modifier: Modifier = Modifier,
) {
  ClerkMaterialTheme {
    Column(modifier = Modifier.fillMaxWidth().then(modifier)) {
      Text(
        modifier = Modifier.padding(horizontal = dp24),
        text = stringResource(R.string.connected_accounts).uppercase(),
        style = ClerkMaterialTheme.typography.bodySmall.withMediumWeight(),
        color = ClerkMaterialTheme.colors.mutedForeground,
      )
      externalAccounts.forEach { UserProfileExternalAccountRow(it) }
      UserProfileButtonRow(text = stringResource(R.string.connect_account), onClick = {})
    }
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  ClerkMaterialTheme {
    Box(modifier = Modifier.background(ClerkMaterialTheme.colors.background)) {
      UserProfileExternalAccountSection(
        externalAccounts =
          persistentListOf(
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
            ExternalAccount(
              id = "eac_34o5pCBEhohJtr1Ni14YiX8aQ0K",
              identificationId = "idn_34o5pAvdtMtjAAdeFBfTkRfs77e",
              provider = "oauth_linear",
              providerUserId = "102662613248529322762",
              emailAddress = "sam@clerk.dev",
              approvedScopes =
                "email https://www.googleapis.com/auth/userinfo.email" +
                  " https://www.googleapis.com/auth/userinfo.profile openid profile",
              createdAt = 1L,
            ),
          )
      )
    }
  }
}
