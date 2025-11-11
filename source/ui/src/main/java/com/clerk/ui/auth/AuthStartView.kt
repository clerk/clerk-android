package com.clerk.ui.auth

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.sso.OAuthProvider
import com.clerk.ui.R
import com.clerk.ui.core.button.social.ClerkSocialRow
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
import com.clerk.ui.core.button.standard.ClerkTextButton
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.divider.TextDivider
import com.clerk.ui.core.input.ClerkPhoneNumberField
import com.clerk.ui.core.input.ClerkTextField
import com.clerk.ui.core.scaffold.ClerkThemedAuthScaffold
import com.clerk.ui.theme.ClerkMaterialTheme
import kotlinx.collections.immutable.toImmutableList

@Composable
fun AuthStartView(modifier: Modifier = Modifier, onAuthComplete: () -> Unit) {
  AuthStartViewImpl(modifier = modifier, onAuthComplete = onAuthComplete)
}

@Composable
internal fun AuthStartViewImpl(
  onAuthComplete: () -> Unit,
  modifier: Modifier = Modifier,
  authViewHelper: AuthStartViewHelper = AuthStartViewHelper(),
  authStartViewModel: AuthStartViewModel = viewModel(),
) {
  val authState = LocalAuthState.current
  val state by authStartViewModel.state.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val generic = stringResource(R.string.something_went_wrong_please_try_again)
  var phoneActive by rememberSaveable { mutableStateOf(false) }
  val isContinueEnabled by
    remember(authState.authStartIdentifier, authState.authStartPhoneNumber, phoneActive) {
      derivedStateOf {
        !authViewHelper.continueIsDisabled(
          isPhoneNumberFieldActive = phoneActive,
          identifier = authState.authStartIdentifier,
          phoneNumber = authState.authStartPhoneNumber,
        )
      }
    }

  LaunchedEffect(state) {
    when (val s = state) {
      is AuthStartViewModel.AuthState.Success.SignInSuccess -> {
        authState.setToStepForStatus(s.signIn!!, onAuthComplete = onAuthComplete)
        authStartViewModel.resetState()
      }
      is AuthStartViewModel.AuthState.Success.SignUpSuccess -> {
        authState.setToStepForStatus(s.signUp!!, onAuthComplete = onAuthComplete)
        authStartViewModel.resetState()
      }
      is AuthStartViewModel.AuthState.OAuthState.SignInSuccess -> {

        authState.setToStepForStatus(s.signIn, onAuthComplete = onAuthComplete)
      }
      is AuthStartViewModel.AuthState.OAuthState.SignUpSuccess -> {
        authState.setToStepForStatus(s.signUp, onAuthComplete = onAuthComplete)
      }
      is AuthStartViewModel.AuthState.Error -> {
        snackbarHostState.showSnackbar(s.message ?: generic)
        authStartViewModel.resetState()
      }
      is AuthStartViewModel.AuthState.OAuthState.Error -> {
        snackbarHostState.showSnackbar(s.message ?: generic)
        authStartViewModel.resetState()
      }
      else -> Unit
    }
  }

  ClerkThemedAuthScaffold(
    modifier = modifier,
    hasBackButton = false,
    title = authViewHelper.titleString(authState.mode),
    subtitle = authViewHelper.subtitleString(authState.mode),
    snackbarHostState = snackbarHostState,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(dp24, alignment = Alignment.CenterVertically),
    ) {
      if (authViewHelper.showIdentifierField) {
        AuthInputField(
          authViewHelper = authViewHelper,
          phoneNumberFieldIsActive = phoneActive,
          authStartPhoneNumber = authState.authStartPhoneNumber,
          authStartIdentifier = authState.authStartIdentifier,
          onPhoneNumberChange = { authState.authStartPhoneNumber = it },
          onIdentifierChange = { authState.authStartIdentifier = it },
        )

        AuthActionButtons(
          authViewHelper = authViewHelper,
          state =
            AuthButtonsState(
              isLoading = state is AuthStartViewModel.AuthState.Loading,
              isEnabled = isContinueEnabled,
              phoneNumberFieldIsActive = phoneActive,
            ),
          callbacks =
            AuthButtonsCallbacks(
              onStartAuth = {
                authStartViewModel.startAuth(
                  authMode = authState.mode,
                  isPhoneNumberFieldActive = phoneActive,
                  identifier = authState.authStartIdentifier,
                  phoneNumber = authState.authStartPhoneNumber,
                )
              },
              onSocialProviderClick = { authStartViewModel.authenticateWithSocialProvider(it) },
              onPhoneNumberFieldToggle = { phoneActive = !phoneActive },
            ),
        )
      }
    }
  }
}

@Composable
private fun AuthInputField(
  authViewHelper: AuthStartViewHelper,
  phoneNumberFieldIsActive: Boolean,
  authStartPhoneNumber: String,
  authStartIdentifier: String,
  onPhoneNumberChange: (String) -> Unit,
  onIdentifierChange: (String) -> Unit,
) {
  if (authViewHelper.phoneNumberIsEnabled && phoneNumberFieldIsActive) {
    ClerkPhoneNumberField(
      value = authStartPhoneNumber,
      modifier = Modifier,
      onValueChange = onPhoneNumberChange,
    )
  } else {
    ClerkTextField(
      inputContentType = ContentType.EmailAddress,
      value = authStartIdentifier,
      onValueChange = onIdentifierChange,
      label = authViewHelper.emailOrUsernamePlaceholder(),
      keyboardOptions =
        KeyboardOptions(keyboardType = authViewHelper.getKeyboardType(phoneNumberFieldIsActive)),
    )
  }
}

private data class AuthButtonsState(
  val isLoading: Boolean = false,
  val isEnabled: Boolean = true,
  val phoneNumberFieldIsActive: Boolean = false,
)

private data class AuthButtonsCallbacks(
  val onStartAuth: () -> Unit,
  val onSocialProviderClick: (OAuthProvider) -> Unit,
  val onPhoneNumberFieldToggle: () -> Unit,
)

@Composable
private fun AuthActionButtons(
  authViewHelper: AuthStartViewHelper,
  state: AuthButtonsState,
  callbacks: AuthButtonsCallbacks,
) {
  ClerkButton(
    modifier = Modifier.fillMaxWidth(),
    text = stringResource(R.string.continue_text),
    isLoading = state.isLoading,
    isEnabled = state.isEnabled,
    icons =
      ClerkButtonDefaults.icons(
        trailingIcon = R.drawable.ic_triangle_right,
        trailingIconColor = ClerkMaterialTheme.colors.primaryForeground,
      ),
    onClick = callbacks.onStartAuth,
  )

  if (authViewHelper.showIdentifierSwitcher) {
    ClerkTextButton(
      text = authViewHelper.identifierSwitcherString(state.phoneNumberFieldIsActive),
      onClick = callbacks.onPhoneNumberFieldToggle,
    )
  }

  if (authViewHelper.showOrDivider) {
    TextDivider(stringResource(R.string.or))
  }

  ClerkSocialRow(
    providers = authViewHelper.authenticatableSocialProviders.toImmutableList(),
    onClick = callbacks.onSocialProviderClick,
  )
}

@SuppressLint("VisibleForTests")
@PreviewLightDark
@Composable
private fun Preview() {
  //  Clerk.customTheme = ClerkTheme(colors = DefaultColors.clerk)
  val authViewHelper = AuthStartViewHelper()

  authViewHelper.setTestValues(
    enabledFirstFactorAttributes = listOf("email_address", "phone_number", "username"),
    applicationName = "Acme Co",
    socialProviders = listOf(OAuthProvider.GOOGLE, OAuthProvider.APPLE, OAuthProvider.FACEBOOK),
  )

  PreviewAuthStateProvider {
    AuthStartViewImpl(authViewHelper = authViewHelper, onAuthComplete = {})
  }
}
