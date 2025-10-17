package com.clerk.ui.signin.backupcode

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.network.model.factor.Factor
import com.clerk.ui.R
import com.clerk.ui.auth.AuthStateEffects
import com.clerk.ui.auth.AuthenticationViewState
import com.clerk.ui.auth.Destination
import com.clerk.ui.auth.LocalAuthState
import com.clerk.ui.auth.PreviewAuthStateProvider
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
import com.clerk.ui.core.button.standard.ClerkTextButton
import com.clerk.ui.core.common.StrategyKeys
import com.clerk.ui.core.input.ClerkTextField
import com.clerk.ui.core.scaffold.ClerkThemedAuthScaffold
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme

/**
 * A composable view for handling second-factor authentication using a backup code.
 *
 * @param modifier The [Modifier] to be applied to the view.
 */
@Composable
fun SignInFactorTwoBackupCodeView(
  factor: Factor,
  modifier: Modifier = Modifier,
  onAuthComplete: () -> Unit,
) {
  SignInFactorTwoBackupCodeViewImpl(
    factor = factor,
    modifier = modifier,
    onAuthComplete = onAuthComplete,
  )
}

/**
 * The internal implementation of the [SignInFactorTwoBackupCodeView].
 *
 * @param modifier The [Modifier] to be applied to the view.
 * @param viewModel The [BackupCodeViewModel] used to manage the state and actions of the view.
 */
@Composable
private fun SignInFactorTwoBackupCodeViewImpl(
  factor: Factor,
  modifier: Modifier = Modifier,
  viewModel: BackupCodeViewModel = viewModel(),
  onAuthComplete: () -> Unit,
) {
  val authState = LocalAuthState.current
  val state by viewModel.state.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }

  AuthStateEffects(
    authState = authState,
    state = state,
    snackbarHostState = snackbarHostState,
    onAuthComplete = onAuthComplete,
  ) {
    viewModel.resetState()
  }

  ClerkThemedAuthScaffold(
    onBackPressed = { authState.navigateBack() },
    modifier = modifier,
    hasLogo = false,
    title = stringResource(R.string.enter_a_backup_code),
    subtitle = stringResource(R.string.your_backup_code),
    snackbarHostState = snackbarHostState,
  ) {
    ClerkTextField(
      modifier = Modifier.fillMaxWidth(),
      value = authState.signInBackupCode,
      onValueChange = { authState.signInBackupCode = it },
      label = stringResource(R.string.backup_code),
    )
    Spacers.Vertical.Spacer24()
    ClerkButton(
      modifier = Modifier.fillMaxWidth(),
      text = stringResource(R.string.continue_text),
      isLoading = state is AuthenticationViewState.Loading,
      onClick = { viewModel.submit(authState.signInBackupCode) },
      icons = ClerkButtonDefaults.icons(trailingIcon = R.drawable.ic_triangle_right),
    )
    Spacers.Vertical.Spacer24()
    ClerkTextButton(
      text = stringResource(R.string.use_another_method),
      onClick = {
        authState.navigateTo(Destination.SignInFactorTwoUseAnotherMethod(currentFactor = factor))
      },
    )
  }
}

@PreviewLightDark
@Composable
private fun PreviewSignInFactorTwoBackupCodeView() {
  PreviewAuthStateProvider {
    ClerkMaterialTheme {
      SignInFactorTwoBackupCodeView(
        onAuthComplete = {},
        factor = Factor(strategy = StrategyKeys.PASSWORD),
      )
    }
  }
}
