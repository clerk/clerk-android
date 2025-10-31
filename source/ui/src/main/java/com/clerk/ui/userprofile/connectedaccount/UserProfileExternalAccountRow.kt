package com.clerk.ui.userprofile.connectedaccount

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.externalaccount.ExternalAccount

@Composable
fun UserProfileExternalAccountRow(
  externalAccount: ExternalAccount,
  modifier: Modifier = Modifier,
) {}

@PreviewLightDark
@Composable
private fun Preview() {
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
