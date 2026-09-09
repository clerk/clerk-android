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
import com.clerk.api.Passkey
import com.clerk.ui.R
import com.clerk.ui.core.composition.clerkViewModel
import com.clerk.ui.core.extensions.formattedRelativeDateTime
import com.clerk.ui.core.menu.DropDownItem
import com.clerk.ui.core.menu.ItemMoreMenu
import com.clerk.ui.core.preview.ClerkPreview
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun UserProfilePasskeyRow(
  passkey: Passkey,
  onClickRename: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: UserProfilePasskeyViewModel = clerkViewModel { UserProfilePasskeyViewModel(it) },
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
          text = passkey.name.orEmpty(),
          style = ClerkMaterialTheme.typography.bodyLarge,
          color = ClerkMaterialTheme.colors.foreground,
        )
        Spacers.Vertical.Spacer4()
        Text(
          text =
            stringResource(
              R.string.created,
              formattedRelativeDateTime(passkey.createdAt.toEpochMilli()),
            ),
          style = ClerkMaterialTheme.typography.bodyMedium,
          color = ClerkMaterialTheme.colors.mutedForeground,
        )
        passkey.lastUsedAt?.let {
          Text(
            text = stringResource(R.string.last_used, formattedRelativeDateTime(it.toEpochMilli())),
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

internal enum class PasskeyActions {
  Rename,
  Remove,
}

@PreviewLightDark
@Composable
private fun Preview() {
  ClerkPreview { clerk ->
    UserProfilePasskeyRow(
      onClickRename = {},
      passkey = clerk.user!!.passkeys[0],
    )
  }
}
