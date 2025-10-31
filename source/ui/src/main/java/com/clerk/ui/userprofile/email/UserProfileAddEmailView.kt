package com.clerk.ui.userprofile.email

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.ui.R
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
import com.clerk.ui.core.input.ClerkTextField
import com.clerk.ui.core.scaffold.ClerkThemedProfileScaffold
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.userprofile.LocalUserProfileState
import com.clerk.ui.userprofile.PreviewUserProfileStateProvider

@Composable
fun UserProfileAddEmailView(modifier: Modifier = Modifier) {
  UserProfileAddEmailViewImpl(modifier = modifier)
}

@Composable
private fun UserProfileAddEmailViewImpl(
  modifier: Modifier = Modifier,
  viewModel: AddEmailViewModel = viewModel(),
) {
  val userProfileState = LocalUserProfileState.current
  var email by remember { mutableStateOf("") }

  val state by viewModel.state.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val errorMessage: String? =
    when (val s = state) {
      is AddEmailViewModel.State.Error ->
        s.message ?: context.getString(R.string.something_went_wrong_please_try_again)
      else -> null
    }

  LaunchedEffect(state) {
    if (state == AddEmailViewModel.State.Success) {
      userProfileState.pop(2)
    }
  }

  ClerkThemedProfileScaffold(
    modifier = modifier,
    title = stringResource(R.string.add_email_address),
    errorMessage = errorMessage,
    content = {
      Text(
        text = stringResource(R.string.you_ll_need_to_verify_this_email_address),
        style = ClerkMaterialTheme.typography.bodyMedium,
      )
      Spacers.Vertical.Spacer24()
      ClerkTextField(
        value = email,
        onValueChange = { email = it },
        label = stringResource(R.string.enter_your_email),
      )
      Spacers.Vertical.Spacer24()
      ClerkButton(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(R.string.continue_text),
        isLoading = state is AddEmailViewModel.State.Loading,
        onClick = { viewModel.addEmail(email) },
        icons = ClerkButtonDefaults.icons(trailingIcon = R.drawable.ic_triangle_right),
      )
    },
  )
}

@PreviewLightDark
@Composable
private fun Preview() {
  PreviewUserProfileStateProvider {
    ClerkMaterialTheme {
      Box(modifier = Modifier.background(color = ClerkMaterialTheme.colors.background)) {
        UserProfileAddEmailView()
      }
    }
  }
}
