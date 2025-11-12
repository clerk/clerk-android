package com.clerk.ui.signin.password.set

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.Clerk
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.resetPasswordFactor
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.R
import com.clerk.ui.auth.AuthDestination
import com.clerk.ui.auth.AuthState
import com.clerk.ui.auth.AuthStateEffects
import com.clerk.ui.auth.LocalAuthState
import com.clerk.ui.auth.PreviewAuthStateProvider
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
import com.clerk.ui.core.button.standard.ClerkTextButton
import com.clerk.ui.core.common.StrategyKeys
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.core.input.ClerkTextField
import com.clerk.ui.core.scaffold.ClerkThemedAuthScaffold
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.DefaultColors

/**
 * A composable view for the first factor password step in the sign-in flow.
 *
 * This view displays a password input field, the user's email, and options for "Forgot Password"
 * and "Use another method".
 *
 * @param factor The [Factor] associated with this password step.
 * @param modifier The [Modifier] to be applied to the view.
 * @param onAuthComplete A callback invoked when the authentication process is complete.
 */
@Composable
fun SignInFactorOnePasswordView(
  factor: Factor,
  modifier: Modifier = Modifier,
  onAuthComplete: () -> Unit,
) {
  SignInFactorOnePasswordViewImpl(
    modifier = modifier,
    factor = factor,
    onAuthComplete = onAuthComplete,
  )
}

@Composable
private fun SignInFactorOnePasswordViewImpl(
  factor: Factor,
  modifier: Modifier = Modifier,
  viewModel: SetPasswordViewModel = viewModel(),
  onAuthComplete: () -> Unit,
) {
  val authState = LocalAuthState.current
  val state by viewModel.state.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }

  AuthStateEffects(
    authState = authState,
    state = state,
    snackbarHostState = snackbarHostState,
    onAuthComplete,
  ) {
    viewModel.resetState()
  }

  ClerkThemedAuthScaffold(
    onBackPressed = { authState.navigateBack() },
    snackbarHostState = snackbarHostState,
    identifier = factor.safeIdentifier,
    onClickIdentifier = { authState.clearBackStack() },
    modifier = modifier,
    title = stringResource(R.string.enter_password),
    subtitle = stringResource(R.string.enter_the_password_associated_with_your_account),
  ) {
    ClerkTextField(
      value = authState.signInPassword,
      onValueChange = { authState.signInPassword = it },
      label = stringResource(R.string.enter_your_password),
      visualTransformation = PasswordVisualTransformation(),
      inputContentType = ContentType.Password,
      keyboardOptions =
        KeyboardOptions(autoCorrectEnabled = false, keyboardType = KeyboardType.Password),
    )
    Spacer(Modifier.height(dp24))
    ClerkButton(
      modifier = Modifier.fillMaxWidth(),
      onClick = { viewModel.submitPassword(password = authState.signInPassword) },
      text = stringResource(R.string.continue_text),
      isEnabled = authState.signInPassword.isNotEmpty(),
      icons =
        ClerkButtonDefaults.icons(
          trailingIcon = R.drawable.ic_triangle_right,
          trailingIconColor = ClerkMaterialTheme.colors.primaryForeground,
        ),
    )
    Spacer(Modifier.height(dp24))
    Footer(authState, factor)
  }
}

@Composable
private fun Footer(authState: AuthState, factor: Factor) {
  val windowInfo = LocalWindowInfo.current
  val density = LocalDensity.current
  val widthDp = with(density) { windowInfo.containerSize.width.toDp() }
  val isCompact = widthDp < 360.dp
  if (isCompact) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = dp8),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(dp24),
    ) {
      ClerkTextButton(
        text = stringResource(R.string.use_another_method),
        onClick = {
          authState.navigateTo(
            AuthDestination.SignInFactorOneUseAnotherMethod(currentFactor = factor)
          )
        },
      )
      ClerkTextButton(
        text = stringResource(R.string.forgot_password),
        onClick = {
          Clerk.signIn?.resetPasswordFactor?.let {
            authState.navigateTo(AuthDestination.SignInForgotPassword)
          }
            ?: authState.navigateTo(
              AuthDestination.SignInFactorOneUseAnotherMethod(currentFactor = factor)
            )
        },
      )
    }
  } else {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = dp8),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ClerkTextButton(
        text = stringResource(R.string.use_another_method),
        onClick = {
          authState.navigateTo(
            AuthDestination.SignInFactorOneUseAnotherMethod(currentFactor = factor)
          )
        },
      )
      ClerkTextButton(
        text = stringResource(R.string.forgot_password),
        onClick = {
          Clerk.signIn?.resetPasswordFactor?.let {
            authState.navigateTo(AuthDestination.SignInForgotPassword)
          }
            ?: authState.navigateTo(
              AuthDestination.SignInFactorOneUseAnotherMethod(currentFactor = factor)
            )
        },
      )
    }
  }
}

@PreviewLightDark
@Composable
private fun PreviewSignInFactorOnePasswordView() {
  PreviewAuthStateProvider {
    Clerk.customTheme = ClerkTheme(colors = DefaultColors.clerk)
    ClerkMaterialTheme {
      SignInFactorOnePasswordView(
        factor = Factor(strategy = StrategyKeys.PASSWORD, safeIdentifier = "sam@clerk.dev"),
        onAuthComplete = {},
      )
    }
  }
}
