package com.clerk.ui.userprofile.security.passkey

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.Clerk
import com.clerk.api.passkeys.Passkey
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp32
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.userprofile.common.UserProfileButtonRow
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun UserProfilePasskeySection(modifier: Modifier = Modifier, onError: (String) -> Unit) {
  val sortedPasskeys = Clerk.user?.passkeys?.sortedBy { it.createdAt }.orEmpty().toImmutableList()
  UserProfilePasskeySectionImpl(passkeys = sortedPasskeys, modifier = modifier, onError = onError)
}

@Composable
private fun UserProfilePasskeySectionImpl(
  passkeys: ImmutableList<Passkey>,
  modifier: Modifier = Modifier,
  viewModel: UserProfilePasskeyViewModel = viewModel(),
  onError: (String) -> Unit,
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  when (val s = state) {
    is UserProfilePasskeyViewModel.State.Error -> onError(s.message)
    else -> {}
  }

  ClerkMaterialTheme {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .background(color = ClerkMaterialTheme.colors.background)
          .padding(top = dp32)
          .padding(bottom = dp16)
          .then(modifier)
    ) {
      Text(
        modifier = Modifier.padding(horizontal = dp24),
        text = stringResource(R.string.passkeys).uppercase(),
        style = ClerkMaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
        color = ClerkMaterialTheme.colors.mutedForeground,
      )
      Spacers.Vertical.Spacer16()
      LazyColumn(modifier = Modifier.fillMaxWidth().padding(horizontal = dp24)) {
        items(passkeys) { passkey -> UserProfilePasskeyRow(passkey = passkey, onClickRename = {}) }
      }
      UserProfileButtonRow(
        modifier = Modifier.padding(horizontal = dp8),
        text = stringResource(R.string.add_a_passkey),
        onClick = { viewModel.createPasskey() },
      )
    }
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  UserProfilePasskeySectionImpl(
    onError = {},
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
      ),
  )
}
