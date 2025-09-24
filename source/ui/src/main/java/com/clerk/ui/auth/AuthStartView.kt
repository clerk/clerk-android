package com.clerk.ui.auth

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.clerk.api.Clerk
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.R
import com.clerk.ui.core.button.social.ClerkSocialRow
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
import com.clerk.ui.core.button.standard.ClerkTextButton
import com.clerk.ui.core.common.ClerkThemedAuthScaffold
import com.clerk.ui.core.common.dimens.dp24
import com.clerk.ui.core.divider.TextDivider
import com.clerk.ui.core.input.ClerkPhoneNumberField
import com.clerk.ui.core.input.ClerkTextField
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.DefaultColors
import kotlinx.collections.immutable.toImmutableList

@Composable
fun AuthStartView(authMode: AuthMode, modifier: Modifier = Modifier) {
  AuthStartViewImpl(authMode = authMode, modifier = modifier)
}

@Composable
internal fun AuthStartViewImpl(
  authMode: AuthMode,
  modifier: Modifier = Modifier,
  authViewHelper: AuthStartViewHelper = AuthStartViewHelper(),
  authStartViewModel: AuthStartViewModel = viewModel(),
) {
  var phoneActive by rememberSaveable { mutableStateOf(false) }
  var phone by rememberSaveable { mutableStateOf("") }
  var identifier by rememberSaveable { mutableStateOf("") }
  val isContinueEnabled by
    remember(identifier, phone, phoneActive) {
      derivedStateOf {
        !authViewHelper.continueIsDisabled(
          isPhoneNumberFieldActive = phoneActive,
          identifier = identifier,
          phoneNumber = phone,
        )
      }
    }

  val state by authStartViewModel.state.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val generic = stringResource(R.string.something_went_wrong_please_try_again)

  HandleAuthErrors(state = state, snackbarHostState = snackbarHostState, generic = generic)

  ClerkThemedAuthScaffold(
    modifier = modifier,
    hasBackButton = false,
    title = authViewHelper.titleString(authMode),
    subtitle = authViewHelper.subtitleString(authMode),
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
          authStartPhoneNumber = phone,
          authStartIdentifier = identifier,
          onPhoneNumberChange = { phone = it },
          onIdentifierChange = { identifier = it },
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
                  authMode = authMode,
                  isPhoneNumberFieldActive = phoneActive,
                  identifier = identifier,
                  phoneNumber = phone,
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

@Composable
internal fun HandleAuthErrors(
  state: AuthStartViewModel.AuthState,
  snackbarHostState: SnackbarHostState,
  generic: String,
) {
  val errorMessage: String? =
    when (state) {
      is AuthStartViewModel.AuthState.Error -> state.message
      is AuthStartViewModel.AuthState.OAuthState.Error -> state.message
      else -> null
    }
  LaunchedEffect(errorMessage) {
    errorMessage?.let { snackbarHostState.showSnackbar(it.ifBlank { generic }) }
  }
}

@Composable
internal fun rememberAuthInputs(
  authViewHelper: AuthStartViewHelper
): Triple<StateHolder, Boolean, Boolean> {
  var phone by rememberSaveable { mutableStateOf("") }
  var id by rememberSaveable { mutableStateOf("") }
  var phoneActive by rememberSaveable { mutableStateOf(false) }

  val isContinueEnabled by
    remember(id, phone, phoneActive) {
      derivedStateOf {
        !authViewHelper.continueIsDisabled(
          isPhoneNumberFieldActive = phoneActive,
          identifier = id,
          phoneNumber = phone,
        )
      }
    }

  return Triple(StateHolder(id, phone, isContinueEnabled), phoneActive, true)
}

internal data class StateHolder(
  val identifier: String,
  val phone: String,
  val isContinueEnabled: Boolean,
)

@SuppressLint("VisibleForTests")
@PreviewLightDark
@Composable
private fun Preview() {
  Clerk.customTheme = ClerkTheme(colors = DefaultColors.clerk)
  val authViewHelper = AuthStartViewHelper()

  authViewHelper.setTestValues(
    enabledFirstFactorAttributes = listOf("email_address", "phone_number", "username"),
    applicationName = "Acme Co",
    socialProviders = listOf(OAuthProvider.GOOGLE, OAuthProvider.APPLE, OAuthProvider.FACEBOOK),
  )

  AuthStartViewImpl(authMode = AuthMode.SignIn, authViewHelper = authViewHelper)
}
