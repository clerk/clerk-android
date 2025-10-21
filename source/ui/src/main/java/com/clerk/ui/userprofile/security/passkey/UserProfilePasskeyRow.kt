package com.clerk.ui.userprofile.security.passkey

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.passkeys.Passkey
import com.clerk.ui.R
import com.clerk.ui.core.extensions.formattedRelativeDateTime
import com.clerk.ui.core.menu.DropDownItem
import com.clerk.ui.core.menu.ItemMoreMenu
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun UserProfilePasskeyRow(
  passkey: Passkey,
  onClickRename: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: UserProfilePasskeyViewModel = viewModel(),
) {
  ClerkMaterialTheme {
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .background(color = ClerkMaterialTheme.colors.background)
          .then(modifier),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier) {
        Text(
          text = passkey.name,
          style = ClerkMaterialTheme.typography.bodyLarge,
          color = ClerkMaterialTheme.colors.foreground,
        )
        Spacers.Vertical.Spacer4()
        Text(
          text = stringResource(R.string.created, formattedRelativeDateTime(passkey.createdAt)),
          style = ClerkMaterialTheme.typography.bodyMedium,
          color = ClerkMaterialTheme.colors.mutedForeground,
        )
        passkey.lastUsedAt?.let {
          Text(
            text = stringResource(R.string.last_used, formattedRelativeDateTime(it)),
            style = ClerkMaterialTheme.typography.bodyMedium,
            color = ClerkMaterialTheme.colors.mutedForeground,
          )
        }
      }
      Spacer(modifier = Modifier.weight(1f))
      ItemMoreMenu(
        dropDownItems =
          persistentListOf(
            DropDownItem(id = PasskeyActions.Rename, text = stringResource(R.string.rename)),
            DropDownItem(
              id = PasskeyActions.Remove,
              text = stringResource(R.string.remove),
              danger = true,
            ),
          ),
        onClick = {
          when (it) {
            PasskeyActions.Rename -> onClickRename()
            PasskeyActions.Remove -> viewModel.deletePasskey(passkey)
          }
        },
      )
    }
  }
}

enum class PasskeyActions {
  Rename,
  Remove,
}

@PreviewLightDark
@Composable
private fun Preview() {
  UserProfilePasskeyRow(
    onClickRename = {},
    passkey =
      Passkey(
        id = "1",
        name = "1Password",
        createdAt = 1760649770226,
        updatedAt = 1760649779603,
        lastUsedAt = 1760649779603,
      ),
  )
}
