package com.clerk.ui.signin.emaillink

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.Clerk
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.R
import com.clerk.ui.auth.AuthDestination
import com.clerk.ui.auth.AuthStateEffects
import com.clerk.ui.auth.AuthenticationViewState
import com.clerk.ui.auth.PreviewAuthStateProvider
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkTextButton
import com.clerk.ui.core.common.StrategyKeys
import com.clerk.ui.core.composition.LocalAuthState
import com.clerk.ui.core.scaffold.ClerkThemedAuthScaffold
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkThemeOverrideProvider

@Composable
fun SignInFactorOneEmailLinkView(
  factor: Factor,
  modifier: Modifier = Modifier,
  clerkTheme: ClerkTheme? = null,
  onAuthComplete: () -> Unit,
) {
  ClerkThemeOverrideProvider(clerkTheme) {
    SignInFactorOneEmailLinkViewImpl(
      factor = factor,
      modifier = modifier,
      onAuthComplete = onAuthComplete,
    )
  }
}

@Composable
private fun SignInFactorOneEmailLinkViewImpl(
  factor: Factor,
  modifier: Modifier = Modifier,
  viewModel: SignInFactorOneEmailLinkViewModel = viewModel(),
  onAuthComplete: () -> Unit,
) {
  val authState = LocalAuthState.current
  val snackbarHostState = remember { SnackbarHostState() }
  val state by viewModel.state.collectAsStateWithLifecycle()

  LaunchedEffect(Unit) { viewModel.sendLink() }

  AuthStateEffects(
    authState = authState,
    state = state,
    snackbarHostState = snackbarHostState,
    onAuthComplete = onAuthComplete,
    onReset = viewModel::resetState,
  )

  ClerkThemedAuthScaffold(
    modifier = modifier,
    onBackPressed = authState::navigateBack,
    title = stringResource(R.string.check_your_email),
    subtitle =
      Clerk.applicationName?.let { stringResource(R.string.to_continue_to, it) }
        ?: stringResource(R.string.to_continue),
    identifier = factor.safeIdentifier,
    onClickIdentifier = { authState.clearBackStack() },
    snackbarHostState = snackbarHostState,
  ) {
    ClerkButton(
      text = stringResource(R.string.resend),
      onClick = { viewModel.sendLink() },
      modifier = Modifier.fillMaxWidth(),
      isLoading = state is AuthenticationViewState.Loading,
    )
    Spacers.Vertical.Spacer24()
    ClerkTextButton(
      text = stringResource(R.string.use_another_method),
      onClick = {
        authState.navigateTo(
          AuthDestination.SignInFactorOneUseAnotherMethod(currentFactor = factor)
        )
      },
    )
  }
}

@PreviewLightDark
@Composable
private fun PreviewSignInFactorOneEmailLinkView() {
  PreviewAuthStateProvider {
    SignInFactorOneEmailLinkView(
      factor = Factor(strategy = StrategyKeys.EMAIL_LINK, safeIdentifier = "sam@clerk.dev"),
      onAuthComplete = {},
    )
  }
}
