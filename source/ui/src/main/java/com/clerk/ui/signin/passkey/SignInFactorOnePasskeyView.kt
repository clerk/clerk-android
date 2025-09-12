package com.clerk.ui.signin.passkey

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
import com.clerk.ui.core.appbar.ClerkTopAppBar
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonConfig
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
import com.clerk.ui.core.button.standard.ClerkTextButton
import com.clerk.ui.core.common.HeaderTextView
import com.clerk.ui.core.common.HeaderType
import com.clerk.ui.core.common.SecuredByClerk
import com.clerk.ui.core.common.Spacers
import com.clerk.ui.core.common.StrategyKeys
import com.clerk.ui.core.common.dimens.dp18
import com.clerk.ui.core.common.dimens.dp72
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.DefaultColors

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
  ClerkMaterialTheme {
    Scaffold(
      snackbarHost = {
        SnackbarHost(snackbarHostState) { data ->
          Snackbar(
            containerColor = ClerkMaterialTheme.computedColors.backgroundDanger,
            contentColor = ClerkMaterialTheme.colors.foreground,
            dismissActionContentColor = ClerkMaterialTheme.colors.foreground,
            snackbarData = data,
          )
        }
      }
    ) { innerPadding ->
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .background(color = ClerkMaterialTheme.colors.background)
            .padding(innerPadding)
            .padding(horizontal = dp18)
            .then(modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        HeaderView(
          onBackPressed = onBackPressed,
          factor = factor,
          onChangeIdentifierClicked = onChangeIdentifierClicked,
        )
        Spacers.Vertical.Spacer32()
        BodyContent(viewModel, state, onUseAnotherMethodClicked)
        Spacers.Vertical.Spacer32()
        SecuredByClerk()
      }
    }
  }
}

@Composable
private fun BodyContent(
  viewModel: PasskeyViewModel,
  state: PasskeyViewModel.AuthenticateState,
  onUseAnotherMethodClicked: () -> Unit,
) {
  Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
    Icon(
      modifier = Modifier.size(dp72),
      painter = painterResource(R.drawable.ic_passkey),
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

@Composable
private fun HeaderView(
  onBackPressed: () -> Unit,
  factor: Factor,
  onChangeIdentifierClicked: () -> Unit,
) {
  ClerkTopAppBar(onBackPressed = onBackPressed)
  Spacers.Vertical.Spacer8()
  HeaderTextView(text = stringResource(R.string.use_your_passkey), type = HeaderType.Title)
  Spacers.Vertical.Spacer8()
  HeaderTextView(
    text =
      stringResource(
        R.string
          .using_your_passkey_confirms_it_s_you_your_device_may_ask_for_your_fingerprint_face_or_screen_lock
      ),
    type = HeaderType.Subtitle,
  )
  Spacers.Vertical.Spacer8()
  ClerkButton(
    text = factor.safeIdentifier!!,
    onClick = onChangeIdentifierClicked,
    icons =
      ClerkButtonDefaults.icons(
        trailingIcon = R.drawable.ic_edit,
        trailingIconColor = ClerkMaterialTheme.colors.mutedForeground,
      ),
    configuration =
      ClerkButtonDefaults.configuration(
        style = ClerkButtonConfig.ButtonStyle.Secondary,
        emphasis = ClerkButtonConfig.Emphasis.High,
      ),
  )
}

@PreviewLightDark
@Composable
private fun PreviewSignInFactorOnePasskeyView() {
  Clerk.customTheme = ClerkTheme(colors = DefaultColors.clerk)
  SignInFactorOnePasskeyView(
    factor = Factor(strategy = StrategyKeys.PASSKEY, safeIdentifier = "sam@clerk.dev")
  )
}
