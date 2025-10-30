package com.clerk.ui.userprofile.security.passkey

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.userprofile.LocalUserProfileState
import com.clerk.ui.userprofile.UserProfileDestination
import com.clerk.ui.userprofile.common.UserProfileButtonRow
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun UserProfilePasskeySection(onError: (String?) -> Unit, modifier: Modifier = Modifier) {
  val user by Clerk.userFlow.collectAsStateWithLifecycle()
  val sortedPasskeys = user?.passkeys?.sortedBy { it.createdAt }.orEmpty().toImmutableList()
  UserProfilePasskeySectionImpl(passkeys = sortedPasskeys, modifier = modifier, onError = onError)
}

@Composable
private fun UserProfilePasskeySectionImpl(
  passkeys: ImmutableList<Passkey>,
  onError: (String?) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: UserProfilePasskeyViewModel = viewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val userProfileState = LocalUserProfileState.current
  val errorMessage = (state as? UserProfilePasskeyViewModel.State.Error)?.message

  if (state is UserProfilePasskeyViewModel.State.Error) {
    LaunchedEffect(Unit) { onError(errorMessage) }
  }

  ClerkMaterialTheme {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .background(color = ClerkMaterialTheme.colors.background)
          .padding(top = dp16)
          .then(modifier)
    ) {
      Text(
        modifier = Modifier.padding(horizontal = dp24),
        text = stringResource(R.string.passkeys).uppercase(),
        style = ClerkMaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
        color = ClerkMaterialTheme.colors.mutedForeground,
      )
      if (passkeys.isNotEmpty()) {
        Spacers.Vertical.Spacer24()
      }
      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = dp24)) {
        passkeys.forEachIndexed { index, passkey ->
          UserProfilePasskeyRow(
            passkey = passkey,
            onClickRename = {
              userProfileState.navigateTo(
                UserProfileDestination.RenamePasskeyView(
                  passkeyId = passkey.id,
                  passkeyName = passkey.name,
                )
              )
            },
          )
          if (index < passkeys.lastIndex) {
            Spacers.Vertical.Spacer32()
          }
        }
      }
      UserProfileButtonRow(
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
