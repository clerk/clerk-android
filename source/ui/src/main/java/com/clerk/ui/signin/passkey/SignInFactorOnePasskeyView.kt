package com.clerk.ui.signin.passkey

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
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
import com.clerk.ui.auth.Destination
import com.clerk.ui.auth.LocalAuthState
import com.clerk.ui.auth.PreviewAuthStateProvider
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
import com.clerk.ui.core.button.standard.ClerkTextButton
import com.clerk.ui.core.common.AuthStateEffects
import com.clerk.ui.core.common.AuthenticationViewState
import com.clerk.ui.core.common.Spacers
import com.clerk.ui.core.common.StrategyKeys
import com.clerk.ui.core.common.dimens.dp72
import com.clerk.ui.core.common.scaffold.ClerkThemedAuthScaffold
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.DefaultColors

/**
 * A composable that displays the passkey sign-in flow.
 *
 * @param factor The passkey factor to use for authentication.
 * @param modifier The modifier to be applied to the component.
 */
@Composable
fun SignInFactorOnePasskeyView(
  factor: Factor,
  modifier: Modifier = Modifier,
  onAuthComplete: () -> Unit,
) {
  SignInFactorOnePasskeyViewImpl(
    modifier = modifier,
    factor = factor,
    onAuthComplete = onAuthComplete,
  )
}

@Composable
private fun SignInFactorOnePasskeyViewImpl(
  factor: Factor,
  modifier: Modifier = Modifier,
  viewModel: PasskeyViewModel = viewModel(),
  onAuthComplete: () -> Unit,
) {

  val authState = LocalAuthState.current
  val snackbarHostState = remember { SnackbarHostState() }
  val state by viewModel.state.collectAsStateWithLifecycle()

  AuthStateEffects(
    authState = authState,
    snackbarHostState = snackbarHostState,
    state = state,
    onAuthComplete = onAuthComplete,
  ) {
    viewModel.resetState()
  }

  ClerkThemedAuthScaffold(
    modifier = modifier,
    onBackPressed = authState::navigateBack,
    title = stringResource(R.string.use_your_passkey),
    subtitle = stringResource(R.string.using_your_passkey),
    onClickIdentifier = { authState.navigateToAuthStart() },
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
      isLoading = state is AuthenticationViewState.Loading,
      icons =
        ClerkButtonDefaults.icons(
          trailingIcon = R.drawable.ic_triangle_right,
          trailingIconColor = ClerkMaterialTheme.colors.primaryForeground,
        ),
    )
    Spacers.Vertical.Spacer16()
    ClerkTextButton(
      onClick = {
        authState.navigateTo(Destination.SignInFactorOneUseAnotherMethod(currentFactor = factor))
      },
      text = stringResource(R.string.use_a_different_method),
    )
  }
}

@PreviewLightDark
@Composable
private fun PreviewSignInFactorOnePasskeyView() {
  PreviewAuthStateProvider {
    Clerk.customTheme = ClerkTheme(colors = DefaultColors.clerk)
    SignInFactorOnePasskeyView(
      factor = Factor(strategy = StrategyKeys.PASSKEY, safeIdentifier = "sam@clerk.dev"),
      onAuthComplete = {},
    )
  }
}
