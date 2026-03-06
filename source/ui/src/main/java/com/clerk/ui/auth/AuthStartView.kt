package com.clerk.ui.auth

import android.annotation.SuppressLint
import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.Clerk
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.R
import com.clerk.ui.core.badge.LastUsedAuthBadgeOverlay
import com.clerk.ui.core.button.social.ClerkSocialButton
import com.clerk.ui.core.button.social.ClerkSocialRow
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
import com.clerk.ui.core.button.standard.ClerkTextButton
import com.clerk.ui.core.composition.LocalAuthState
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.core.divider.TextDivider
import com.clerk.ui.core.input.ClerkPhoneNumberField
import com.clerk.ui.core.input.ClerkTextField
import com.clerk.ui.core.scaffold.ClerkThemedAuthScaffold
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.ClerkThemeOverrideProvider
import kotlinx.collections.immutable.toImmutableList

@Composable
fun AuthStartView(
  modifier: Modifier = Modifier,
  clerkTheme: ClerkTheme? = null,
  onAuthComplete: () -> Unit,
) {
  AuthStartViewImpl(modifier = modifier, onAuthComplete = onAuthComplete, clerkTheme = clerkTheme)
}

@Composable
@Suppress("CyclomaticComplexMethod", "LongMethod")
internal fun AuthStartViewImpl(
  onAuthComplete: () -> Unit,
  modifier: Modifier = Modifier,
  authViewHelper: AuthStartViewHelper = AuthStartViewHelper(),
  clerkTheme: ClerkTheme? = null,
  authStartViewModel: AuthStartViewModel = viewModel(),
) {
  val context = LocalContext.current
  val authState = LocalAuthState.current
  val state by authStartViewModel.state.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val generic = stringResource(R.string.something_went_wrong_please_try_again)
  var phoneActive by rememberSaveable {
    mutableStateOf(
      authViewHelper.shouldStartOnPhoneNumber(
        authState.authStartPhoneNumber,
        authState.authStartIdentifier,
      )
    )
  }
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
  val socialProviders = authViewHelper.authenticatableSocialProviders
  val lastAuthenticationStrategy =
    runCatching { Clerk.client.lastAuthenticationStrategy }.getOrNull()
  val lastUsedAuth =
    remember(
      lastAuthenticationStrategy,
      socialProviders,
      Clerk.enabledFirstFactorAttributes,
      context,
    ) {
      LastUsedAuth.from(
        lastAuthenticationStrategy = lastAuthenticationStrategy,
        enabledFirstFactorAttributes = Clerk.enabledFirstFactorAttributes,
        authenticatableSocialProviders = socialProviders,
        storedIdentifierType = LastUsedIdentifierStorage.retrieve(context),
      )
    }
  val lastUsedSocialProvider = lastUsedAuth?.socialProvider
  val socialProvidersMinusLastUsed =
    if (lastUsedSocialProvider == null) {
      socialProviders
    } else {
      socialProviders.filter { it != lastUsedSocialProvider }
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

  ClerkThemeOverrideProvider(clerkTheme = clerkTheme) {
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
            showPhoneBadge = lastUsedAuth?.showsPhoneBadge == true,
            showEmailUsernameBadge = lastUsedAuth?.showsEmailUsernameBadge == true,
          )

          ClerkButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.continue_text),
            isLoading = state is AuthStartViewModel.AuthState.Loading,
            isEnabled = isContinueEnabled,
            icons =
              ClerkButtonDefaults.icons(
                trailingIcon = R.drawable.ic_triangle_right,
                trailingIconColor = ClerkMaterialTheme.colors.primaryForeground,
              ),
            onClick = {
              if (authState.mode != AuthMode.SignUp) {
                storeIdentifierType(
                  context = context,
                  authViewHelper = authViewHelper,
                  phoneNumberFieldIsActive = phoneActive,
                  authStartIdentifier = authState.authStartIdentifier,
                )
              }
              authStartViewModel.startAuth(
                authMode = authState.mode,
                isPhoneNumberFieldActive = phoneActive,
                identifier = authState.authStartIdentifier,
                phoneNumber = authState.authStartPhoneNumber,
              )
            },
          )

          if (authViewHelper.showIdentifierSwitcher) {
            ClerkTextButton(
              text = authViewHelper.identifierSwitcherString(phoneActive),
              onClick = { phoneActive = !phoneActive },
            )
          }
        }

        if (authViewHelper.showOrDivider) {
          TextDivider(stringResource(R.string.or))
        }
        Column(verticalArrangement = Arrangement.spacedBy(dp8)) {
          if (lastUsedSocialProvider != null) {
            LastUsedAuthBadgeOverlay(isVisible = true, modifier = Modifier.fillMaxWidth()) {
              ClerkSocialButton(
                provider = lastUsedSocialProvider,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                  authStartViewModel.authenticateWithSocialProvider(it, authState.mode.transferable)
                },
                forceIconOnly = false,
              )
            }
          }

          if (socialProvidersMinusLastUsed.isNotEmpty()) {
            ClerkSocialRow(
              providers = socialProvidersMinusLastUsed.toImmutableList(),
              onClick = {
                authStartViewModel.authenticateWithSocialProvider(it, authState.mode.transferable)
              },
            )
          }
        }
      }
    }
  }
}

@Composable
@Suppress("LongParameterList")
private fun AuthInputField(
  authViewHelper: AuthStartViewHelper,
  phoneNumberFieldIsActive: Boolean,
  authStartPhoneNumber: String,
  authStartIdentifier: String,
  onPhoneNumberChange: (String) -> Unit,
  onIdentifierChange: (String) -> Unit,
  showPhoneBadge: Boolean,
  showEmailUsernameBadge: Boolean,
) {
  if (authViewHelper.phoneNumberIsEnabled && phoneNumberFieldIsActive) {
    LastUsedAuthBadgeOverlay(isVisible = showPhoneBadge) {
      ClerkPhoneNumberField(
        value = authStartPhoneNumber,
        modifier = Modifier.fillMaxWidth(),
        onValueChange = onPhoneNumberChange,
      )
    }
  } else {
    LastUsedAuthBadgeOverlay(isVisible = showEmailUsernameBadge) {
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
}

private fun storeIdentifierType(
  context: Context,
  authViewHelper: AuthStartViewHelper,
  phoneNumberFieldIsActive: Boolean,
  authStartIdentifier: String,
) {
  val identifierType =
    if (phoneNumberFieldIsActive && authViewHelper.phoneNumberIsEnabled) {
      IdentifierType.Phone
    } else if (authStartIdentifier.isEmailAddress) {
      IdentifierType.Email
    } else {
      IdentifierType.Username
    }

  LastUsedIdentifierStorage.store(context, identifierType)
}

private val emailRegex = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")

private val String.isEmailAddress: Boolean
  get() = emailRegex.matches(this)

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
