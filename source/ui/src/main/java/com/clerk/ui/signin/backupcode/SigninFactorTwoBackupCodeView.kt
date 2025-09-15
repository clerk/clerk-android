package com.clerk.ui.signin.backupcode

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SnackbarHostState
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
import com.clerk.ui.core.button.standard.ClerkTextButton
import com.clerk.ui.core.common.ClerkThemedAuthScaffold
import com.clerk.ui.core.common.Spacers
import com.clerk.ui.core.error.ClerkErrorSnackbar
import com.clerk.ui.core.input.ClerkTextField
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
fun SignInFactorTwoBackupCodeView(
  onBackPressed: () -> Unit,
  onSubmitSuccess: () -> Unit,
  modifier: Modifier = Modifier,
  onUseAnotherMethod: () -> Unit,
) {
  SignInFactorTwoBackupCodeViewImpl(
    onBackPressed = onBackPressed,
    modifier = modifier,
    onSubmitSuccess = onSubmitSuccess,
    onUseAnotherMethod = onUseAnotherMethod,
  )
}

@Composable
private fun SignInFactorTwoBackupCodeViewImpl(
  onBackPressed: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: BackupCodeViewModel = viewModel(),
  onSubmitSuccess: () -> Unit = {},
  onUseAnotherMethod: () -> Unit = {},
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  var backupCode by remember { mutableStateOf("") }
  val snackbarHostState = remember { SnackbarHostState() }
  val context = LocalContext.current

  LaunchedEffect(state) {
    if (state is BackupCodeViewModel.AuthenticationState.Success) {
      onSubmitSuccess()
    }
  }

  LaunchedEffect(state) {
    if (state is BackupCodeViewModel.AuthenticationState.Error) {
      snackbarHostState.showSnackbar(
        message =
          (state as BackupCodeViewModel.AuthenticationState.Error).message
            ?: context.getString(R.string.an_error_occurred)
      )
    }
  }

  ClerkThemedAuthScaffold(
    onBackPressed = onBackPressed,
    modifier = modifier,
    hasLogo = false,
    title = stringResource(R.string.enter_a_backup_code),
    subtitle = stringResource(R.string.your_backup_code),
    snackbarHost = { ClerkErrorSnackbar(snackbarHostState) },
  ) {
    ClerkTextField(
      modifier = Modifier.fillMaxWidth(),
      value = backupCode,
      onValueChange = { backupCode = it },
      label = stringResource(R.string.backup_code),
    )
    Spacers.Vertical.Spacer24()
    ClerkButton(
      modifier = Modifier.fillMaxWidth(),
      text = stringResource(R.string.continue_text),
      isLoading = state is BackupCodeViewModel.AuthenticationState.Verifying,
      onClick = { viewModel.submit(backupCode) },
      icons = ClerkButtonDefaults.icons(trailingIcon = R.drawable.ic_triangle_right),
    )
    Spacers.Vertical.Spacer24()
    ClerkTextButton(
      text = stringResource(R.string.use_another_method),
      onClick = onUseAnotherMethod,
    )
  }
}

@PreviewLightDark
@Composable
private fun PreviewSignInFactorTwoBackupCodeView() {
  ClerkMaterialTheme {
    SignInFactorTwoBackupCodeView(onBackPressed = {}, onUseAnotherMethod = {}, onSubmitSuccess = {})
  }
}
