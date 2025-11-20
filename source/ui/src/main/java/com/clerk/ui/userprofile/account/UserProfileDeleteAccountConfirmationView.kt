package com.clerk.ui.userprofile.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.ui.R
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonConfiguration
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.input.ClerkTextField
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.userprofile.LocalUserProfileState

@Composable
internal fun UserProfileDeleteAccountConfirmationView(
  onError: (String?) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: UserProfileAccountViewModel = viewModel(),
  onClose: () -> Unit,
) {
  val userProfileState = LocalUserProfileState.current
  val state by viewModel.deleteAccountStateFlow.collectAsStateWithLifecycle()
  var input by remember { mutableStateOf("") }
  LaunchedEffect(state) {
    if (state is UserProfileAccountViewModel.DeleteAccountState.Success) {
      viewModel.resetState()
      userProfileState.clearBackStack()
    }
  }
  if (state is UserProfileAccountViewModel.DeleteAccountState.Error) {
    LaunchedEffect(state) {
      val errorMessage = (state as? UserProfileAccountViewModel.DeleteAccountState.Error)?.message
      onError(errorMessage)
      viewModel.resetState()
    }
  }
  DisposableEffect(Unit) { onDispose { viewModel.resetState() } }
  ClerkMaterialTheme {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .background(color = ClerkMaterialTheme.colors.background)
          .then(modifier)
    ) {
      TopBar(onClose)
      HorizontalDivider(thickness = dp1, color = ClerkMaterialTheme.computedColors.border)
      Text(
        modifier = Modifier.padding(horizontal = dp24).padding(top = dp24),
        text = stringResource(R.string.are_you_sure_you_want_to_delete_your_account),
        style = ClerkMaterialTheme.typography.bodyLarge,
        color = ClerkMaterialTheme.colors.danger,
      )
      Spacers.Vertical.Spacer12()
      ClerkTextField(
        modifier = Modifier.padding(horizontal = dp24),
        value = input,
        onValueChange = { input = it },
        label = stringResource(R.string.type_delete_to_continue),
      )
      Spacers.Vertical.Spacer24()
      ClerkButton(
        isLoading = state is UserProfileAccountViewModel.DeleteAccountState.Loading,
        isEnabled = input == "DELETE",
        modifier = Modifier.fillMaxWidth().padding(horizontal = dp24),
        text = stringResource(R.string.delete_account),
        onClick = { viewModel.deleteAccount() },
        configuration =
          ClerkButtonDefaults.configuration(style = ClerkButtonConfiguration.ButtonStyle.Negative),
      )
      Spacers.Vertical.Spacer24()
    }
  }
}

@Composable
private fun TopBar(onClose: () -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    IconButton(onClick = {}) {}
    Spacer(modifier = Modifier.weight(1f))
    Text(
      text = stringResource(R.string.delete_account),
      style = ClerkMaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    )
    Spacer(modifier = Modifier.weight(1f))
    IconButton(onClick = onClose) {
      Icon(
        modifier = Modifier.size(dp24),
        painter = painterResource(R.drawable.ic_cross),
        contentDescription = stringResource(R.string.close),
      )
    }
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  UserProfileDeleteAccountConfirmationView(onClose = {}, onError = {})
}
