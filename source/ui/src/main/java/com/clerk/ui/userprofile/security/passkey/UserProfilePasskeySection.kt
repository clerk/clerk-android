package com.clerk.ui.userprofile.security.passkey

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.Clerk
import com.clerk.api.passkeys.Passkey
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp32
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.userprofile.common.UserProfileButtonRow
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Composable
fun UserProfilePasskeySection(modifier: Modifier = Modifier) {
  val sortedPasskeys = Clerk.user?.passkeys?.sortedBy { it.createdAt }.orEmpty().toImmutableList()
  UserProfilePasskeySectionImpl(passkeys = sortedPasskeys, modifier = modifier)
}

@Composable
fun UserProfilePasskeySectionImpl(passkeys: ImmutableList<Passkey>, modifier: Modifier = Modifier) {
  ClerkMaterialTheme {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .background(color = ClerkMaterialTheme.colors.background)
          .padding(horizontal = dp24)
          .padding(top = dp32)
          .padding(bottom = dp16)
          .then(modifier)
    ) {
      Text(
        text = stringResource(R.string.passkeys).uppercase(),
        style = ClerkMaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
        color = ClerkMaterialTheme.colors.mutedForeground,
      )
      Spacers.Vertical.Spacer16()
      LazyColumn(modifier = Modifier.fillMaxWidth()) {
        items(passkeys) { passkey -> UserProfilePasskeyRow(passkey = passkey, onClickRename = {}) }
      }
      UserProfileButtonRow(text = "Add a passkey") {}
    }
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  UserProfilePasskeySectionImpl(
    passkeys =
      persistentListOf(
        Passkey(
          id = "1",
          name = "1Password",
          createdAt = 1760649770226,
          updatedAt = 1760649779603,
          lastUsedAt = 1760649779603,
        ),
        Passkey(
          id = "2",
          name = "1Password",
          createdAt = 1760649770226,
          updatedAt = 1760649779603,
          lastUsedAt = 1760649779603,
        ),
        Passkey(
          id = "1",
          name = "Google chrome",
          createdAt = 1760649770226,
          updatedAt = 1760649779603,
          lastUsedAt = 1760649779603,
        ),
      )
  )
}
