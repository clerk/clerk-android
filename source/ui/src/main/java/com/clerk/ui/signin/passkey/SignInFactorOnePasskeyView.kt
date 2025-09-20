package com.clerk.ui.signin.passkey

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.Clerk
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.R
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
import com.clerk.ui.core.button.standard.ClerkTextButton
import com.clerk.ui.core.common.ClerkThemedAuthScaffold
import com.clerk.ui.core.common.Spacers
import com.clerk.ui.core.common.StrategyKeys
import com.clerk.ui.core.common.dimens.dp72
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.DefaultColors

/**
 * A composable that displays the passkey sign-in flow.
 *
 * @param factor The passkey factor to use for authentication.
 * @param modifier The modifier to be applied to the component.
 * @param onBackPressed A callback to be invoked when the user presses the back button.
 * @param onChangeIdentifierClicked A callback to be invoked when the user clicks the button to
 *   change the identifier.
 * @param onUseAnotherMethodClicked A callback to be invoked when the user clicks the button to use
 *   another method.
 */
@Composable
fun SignInFactorOnePasskeyView(
  factor: Factor,
  modifier: Modifier = Modifier,
  onBackPressed: () -> Unit = {},
  onChangeIdentifierClicked: () -> Unit = {},
  onUseAnotherMethodClicked: () -> Unit = {},
) {
  SignInFactorOnePasskeyViewImpl(
    modifier = modifier,
    onBackPressed = onBackPressed,
    onChangeIdentifierClicked = onChangeIdentifierClicked,
    factor = factor,
    onUseAnotherMethodClicked = onUseAnotherMethodClicked,
  )
}

@Composable
private fun SignInFactorOnePasskeyViewImpl(
  onBackPressed: () -> Unit,
  factor: Factor,
  modifier: Modifier = Modifier,
  onChangeIdentifierClicked: () -> Unit = {},
  viewModel: PasskeyViewModel = viewModel(),
  onUseAnotherMethodClicked: () -> Unit = {},
) {
  val snackbarHostState = remember { SnackbarHostState() }
  val state by viewModel.state.collectAsStateWithLifecycle()
  if (state is PasskeyViewModel.AuthenticateState.Failed) {
    val errorMessage =
      (state as PasskeyViewModel.AuthenticateState.Failed).message
        ?: "That action couldn't be completed."
    LaunchedEffect(state) {
      snackbarHostState.showSnackbar(
        message = errorMessage,
        withDismissAction = true,
        duration = SnackbarDuration.Short,
      )
    }
  }
  ClerkThemedAuthScaffold(
    modifier = modifier,
    onBackPressed = onBackPressed,
    title = stringResource(R.string.use_your_passkey),
    subtitle = stringResource(R.string.using_your_passkey),
    onClickIdentifier = onChangeIdentifierClicked,
    identifier = factor.safeIdentifier,
    snackbarHostState = snackbarHostState,
  ) {
    Icon(
      modifier = Modifier.size(dp72),
      painter = painterResource(R.drawable.ic_android_passkey),
      contentDescription = stringResource(R.string.passkey_icon),
      tint = ClerkMaterialTheme.colors.primary,
    )
    Spacers.Vertical.Spacer32()
    ClerkButton(
      text = stringResource(R.string.continue_text),
      onClick = { viewModel.authenticate() },
      modifier = Modifier.fillMaxWidth(),
      isLoading = state is PasskeyViewModel.AuthenticateState.Verifying,
      icons =
        ClerkButtonDefaults.icons(
          trailingIcon = R.drawable.ic_triangle_right,
          trailingIconColor = ClerkMaterialTheme.colors.primaryForeground,
        ),
    )
    Spacers.Vertical.Spacer16()
    ClerkTextButton(
      onClick = onUseAnotherMethodClicked,
      text = stringResource(R.string.use_a_different_method),
    )
  }
}

@PreviewLightDark
@Composable
private fun PreviewSignInFactorOnePasskeyView() {
  Clerk.customTheme = ClerkTheme(colors = DefaultColors.clerk)
  SignInFactorOnePasskeyView(
    factor = Factor(strategy = StrategyKeys.PASSKEY, safeIdentifier = "sam@clerk.dev")
  )
}
