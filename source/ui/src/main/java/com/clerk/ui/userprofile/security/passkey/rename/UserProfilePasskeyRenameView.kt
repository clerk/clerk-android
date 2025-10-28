package com.clerk.ui.userprofile.security.passkey.rename

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.ui.R
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.input.ClerkTextField
import com.clerk.ui.core.scaffold.ClerkThemedProfileScaffold
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
fun UserProfilePasskeyRenameView(
  passkeyId: String,
  passkeyName: String,
  modifier: Modifier = Modifier,
  onBackPressed: () -> Unit,
) {
  UserProfilePasskeyRenameViewImpl(
    modifier = modifier,
    passkeyName = passkeyName,
    onBackPressed = onBackPressed,
    passkeyId = passkeyId,
  )
}

@Composable
private fun UserProfilePasskeyRenameViewImpl(
  passkeyId: String,
  passkeyName: String,
  onBackPressed: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: UserProfilePasskeyRenameViewModel = viewModel(),
) {
  var passkeyName by rememberSaveable { mutableStateOf(passkeyName) }
  val state by viewModel.state.collectAsStateWithLifecycle()
  val errorMessage = (state as? UserProfilePasskeyRenameViewModel.State.Error)?.message
  ClerkThemedProfileScaffold(
    modifier = modifier,
    hasBackButton = true,
    errorMessage = errorMessage,
    title = stringResource(R.string.rename_passkey),
    onBackPressed = onBackPressed,
  ) {
    Text(
      text = stringResource(R.string.you_can_change_the_passkey_name),
      style = ClerkMaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
      color = ClerkMaterialTheme.colors.mutedForeground,
    )
    Spacers.Vertical.Spacer20()
    ClerkTextField(
      value = passkeyName,
      onValueChange = { passkeyName = it },
      label = stringResource(R.string.name_of_passkey),
    )
    Spacers.Vertical.Spacer24()
    ClerkButton(
      modifier = Modifier.fillMaxWidth(),
      text = stringResource(R.string.save),
      onClick = { viewModel.renamePasskey(passkeyId) },
    )
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  ClerkMaterialTheme {
    UserProfilePasskeyRenameView(
      passkeyId = "123",
      passkeyName = "One password",
      onBackPressed = {},
    )
  }
}
