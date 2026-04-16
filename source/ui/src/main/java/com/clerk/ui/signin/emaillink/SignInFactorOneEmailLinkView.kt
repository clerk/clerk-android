package com.clerk.ui.signin.emaillink

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.Clerk
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.R
import com.clerk.ui.auth.AuthDestination
import com.clerk.ui.auth.AuthStateEffects
import com.clerk.ui.auth.PreviewAuthStateProvider
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonConfiguration
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
import com.clerk.ui.core.button.standard.ClerkTextButton
import com.clerk.ui.core.common.StrategyKeys
import com.clerk.ui.core.composition.LocalAuthState
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.core.scaffold.ClerkThemedAuthScaffold
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkThemeOverrideProvider
import com.clerk.ui.util.EmailAppLauncher
import kotlinx.coroutines.launch

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
  ObserveHostResume(onHostResumed = viewModel::onHostResumed)

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
    OpenEmailAppButton(snackbarHostState = snackbarHostState)
    Spacers.Vertical.Spacer24()
    SignInEmailLinkSecondaryActions(
      onResendClick = viewModel::sendLink,
      onUseAnotherMethodClick = {
        authState.navigateTo(
          AuthDestination.SignInFactorOneUseAnotherMethod(currentFactor = factor)
        )
      },
    )
  }
}

@Composable
private fun ObserveHostResume(onHostResumed: () -> Unit) {
  val lifecycleOwner = LocalLifecycleOwner.current

  DisposableEffect(lifecycleOwner, onHostResumed) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        onHostResumed()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }
}

@Composable
private fun OpenEmailAppButton(snackbarHostState: SnackbarHostState) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  ClerkButton(
    text = stringResource(R.string.open_email_app),
    onClick = {
      if (!EmailAppLauncher.open(context)) {
        coroutineScope.launch {
          snackbarHostState.showSnackbar(
            message = context.getString(R.string.no_email_clients_installed_on_device),
            duration = SnackbarDuration.Short,
          )
        }
      }
    },
    modifier = Modifier.fillMaxWidth(),
    configuration =
      ClerkButtonDefaults.configuration(
        style = ClerkButtonConfiguration.ButtonStyle.Secondary,
        emphasis = ClerkButtonConfiguration.Emphasis.High,
      ),
  )
}

@Composable
private fun SignInEmailLinkSecondaryActions(
  onResendClick: () -> Unit,
  onUseAnotherMethodClick: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = dp8),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    ClerkTextButton(text = stringResource(R.string.resend), onClick = onResendClick)
    ClerkTextButton(
      text = stringResource(R.string.use_another_method),
      onClick = onUseAnotherMethodClick,
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
