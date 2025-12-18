package com.clerk.ui.userprofile.email

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.ui.R
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.input.ClerkTextField
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.userprofile.common.BottomSheetTopBar
import com.clerk.ui.userprofile.verify.Mode

@Composable
internal fun UserProfileAddEmailViewBottomSheetContent(
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
  onVerify: (Mode.Email) -> Unit,
) {
  UserProfileAddEmailViewImpl(modifier = modifier, onVerify = onVerify, onDismiss = onDismiss)
}

@Composable
private fun UserProfileAddEmailViewImpl(
  onVerify: (Mode.Email) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: AddEmailViewModel = viewModel(),
) {
  var email by remember { mutableStateOf("") }

  val state by viewModel.state.collectAsStateWithLifecycle()

  LaunchedEffect(state) {
    if (state is AddEmailViewModel.State.Success) {
      onVerify(Mode.Email((state as AddEmailViewModel.State.Success).emailAddress))
      viewModel.resetState()
    }
  }

  Column(modifier = Modifier.fillMaxWidth().then(modifier)) {
    BottomSheetTopBar(
      title = stringResource(R.string.add_email_address),
      onClosePressed = onDismiss,
    )
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = dp24).padding(vertical = dp24),
      verticalArrangement = Arrangement.spacedBy(dp24),
    ) {
      Text(
        color = ClerkMaterialTheme.colors.mutedForeground,
        text = stringResource(R.string.you_ll_need_to_verify_this_email_address),
        style = ClerkMaterialTheme.typography.bodyMedium,
      )
      ClerkTextField(
        value = email,
        isError = state is AddEmailViewModel.State.Error,
        supportingText = (state as? AddEmailViewModel.State.Error)?.message,
        onValueChange = {
          viewModel.resetState()
          email = it
        },
        label = stringResource(R.string.enter_your_email),
        inputContentType = ContentType.EmailAddress,
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email),
      )
      ClerkButton(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(R.string.continue_text),
        isLoading = state is AddEmailViewModel.State.Loading,
        onClick = { viewModel.addEmail(email) },
        icons = ClerkButtonDefaults.icons(trailingIcon = R.drawable.ic_triangle_right),
      )
    }
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  ClerkMaterialTheme {
    Box(modifier = Modifier.background(color = ClerkMaterialTheme.colors.background)) {
      UserProfileAddEmailViewBottomSheetContent(onVerify = {}, onDismiss = {})
    }
  }
}
