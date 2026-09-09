package com.clerk.ui.userprofile.connectedaccount

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.ExternalAccount
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.extensions.withMediumWeight
import com.clerk.ui.core.preview.ClerkPreview
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.userprofile.common.UserProfileButtonRow
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal fun LazyListScope.userProfileExternalAccountSection(
  externalAccounts: ImmutableList<ExternalAccount>,
  onError: (String) -> Unit,
  onClickAddAccount: () -> Unit,
  isInteractive: Boolean = true,
  loadRemoteLogos: Boolean = true,
  externalAccountRow: @Composable (ExternalAccount) -> Unit = {
    UserProfileExternalAccountRow(
      externalAccount = it,
      isInteractive = isInteractive,
      loadRemoteLogo = loadRemoteLogos,
      onError = onError,
    )
  },
) {
  item(key = "user_profile_external_account_header") {
    Text(
      modifier = Modifier.padding(horizontal = dp24),
      text = stringResource(R.string.connected_accounts).uppercase(),
      style = ClerkMaterialTheme.typography.bodySmall.withMediumWeight(),
      color = ClerkMaterialTheme.colors.mutedForeground,
    )
  }
  item(key = "user_profile_external_account_header_spacing") { Spacers.Vertical.Spacer16() }
  items(
    items = externalAccounts,
    key = { externalAccount -> "user_profile_external_account_${externalAccount.id}" },
    contentType = { "user_profile_external_account" },
  ) { externalAccount ->
    externalAccountRow(externalAccount)
  }
  item(key = "user_profile_external_account_add") {
    UserProfileButtonRow(
      text = stringResource(R.string.connect_account),
      onClick = onClickAddAccount,
    )
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  ClerkPreview { clerk ->
    ClerkMaterialTheme {
      Box(modifier = Modifier.background(ClerkMaterialTheme.colors.background)) {
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
          userProfileExternalAccountSection(
            onError = {},
            onClickAddAccount = {},
            externalAccounts = clerk.user!!.externalAccounts.toImmutableList(),
          )
        }
      }
    }
  }
}
