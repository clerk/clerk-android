package com.clerk.ui.auth

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.Clerk
import com.clerk.api.log.ClerkLog
import com.clerk.api.session.Session
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.trusteddevice.TrustedDeviceValidationResult
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.R
import com.clerk.ui.core.badge.LastUsedAuthBadgeOverlay
import com.clerk.ui.core.button.social.ClerkSocialButton
import com.clerk.ui.core.button.social.ClerkSocialRow
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonConfiguration
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
import com.clerk.ui.core.button.standard.ClerkTextButton
import com.clerk.ui.core.composition.ClerkLogoProvider
import com.clerk.ui.core.composition.LocalAuthState
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.core.divider.TextDivider
import com.clerk.ui.core.input.ClerkPhoneNumberField
import com.clerk.ui.core.input.ClerkTextField
import com.clerk.ui.core.navigation.rememberDismissHandler
import com.clerk.ui.core.scaffold.ClerkThemedAuthScaffold
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.ClerkThemeOverrideProvider
import kotlinx.collections.immutable.toImmutableList

@Composable
fun AuthStartView(
  modifier: Modifier = Modifier,
  clerkTheme: ClerkTheme? = null,
  logo: (@Composable () -> Unit)? = null,
  preferGoogleOneTap: Boolean = true,
  startSocialOAuthAsSignUp: Boolean = false,
  isDismissible: Boolean = true,
  onDismiss: (() -> Unit)? = null,
  onAuthComplete: () -> Unit,
) {
  ClerkLogoProvider(logo) {
    AuthStartViewImpl(
      modifier = modifier,
      preferGoogleOneTap = preferGoogleOneTap,
      startSocialOAuthAsSignUp = startSocialOAuthAsSignUp,
      isDismissible = isDismissible,
      onDismiss = onDismiss,
      onAuthComplete = onAuthComplete,
      clerkTheme = clerkTheme,
    )
  }
}

@Composable
@Suppress("CyclomaticComplexMethod", "LongMethod")
internal fun AuthStartViewImpl(
  onAuthComplete: () -> Unit,
  modifier: Modifier = Modifier,
  authViewHelper: AuthStartViewHelper = AuthStartViewHelper(),
  clerkTheme: ClerkTheme? = null,
  preferGoogleOneTap: Boolean = true,
  startSocialOAuthAsSignUp: Boolean = false,
  isDismissible: Boolean = true,
  onDismiss: (() -> Unit)? = null,
  authStartViewModel: AuthStartViewModel = viewModel(),
) {
  val authState = LocalAuthState.current
  val state by authStartViewModel.state.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val generic = stringResource(R.string.something_went_wrong_please_try_again)
  var phoneActive by
    rememberSaveable(authState.identifierConfigVersion) {
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
  val trustedDeviceSignInConfigIsEnabled =
    authViewHelper.trustedDeviceSignInConfigIsEnabled && authState.mode != AuthMode.SignUp
  var trustedDeviceSignInIsAvailable by remember { mutableStateOf(false) }

  LaunchedEffect(trustedDeviceSignInConfigIsEnabled) {
    trustedDeviceSignInIsAvailable =
      trustedDeviceSignInConfigIsEnabled && resolveTrustedDeviceSignInAvailability()
  }

  val lastAuthenticationStrategy =
    runCatching { Clerk.client.lastAuthenticationStrategy }.getOrNull()
  val lastUsedAuth =
    LastUsedAuth.from(
      lastAuthenticationStrategy = lastAuthenticationStrategy,
      enabledFirstFactorAttributes = Clerk.enabledFirstFactorAttributes,
      authenticatableSocialProviders = socialProviders,
      storedIdentifierType = authState.storedIdentifierType,
      trustedDeviceSignInIsVisible = trustedDeviceSignInIsAvailable,
    )
  val lastUsedSocialProvider = lastUsedAuth?.socialProvider
  val socialProvidersMinusLastUsed =
    if (lastUsedSocialProvider == null) {
      socialProviders
    } else {
      socialProviders.filter { it != lastUsedSocialProvider }
    }
  val showDismissButton = shouldShowAuthDismissButton(isDismissible)
  val dismissHandler = rememberDismissHandler(onDismiss)
  val lockedInitialIdentifierIsActive =
    authState.authStartIdentifierLocked || authState.authStartPhoneNumberLocked
  val passkeySignInConfigIsEnabled = authViewHelper.passkeySignInConfigIsEnabled
  val automaticPasskeySignInIsEnabled =
    shouldStartAutomaticPasskeySignIn(
      authMode = authState.mode,
      lockedInitialIdentifierIsActive = lockedInitialIdentifierIsActive,
      passkeySignInConfigIsEnabled = passkeySignInConfigIsEnabled,
    )
  var automaticPasskeySignInHasStarted by rememberSaveable { mutableStateOf(false) }

  LaunchedEffect(
    automaticPasskeySignInIsEnabled,
    automaticPasskeySignInHasStarted,
    authState.mode,
    lockedInitialIdentifierIsActive,
    passkeySignInConfigIsEnabled,
  ) {
    ClerkLog.d(
      "AuthStart automatic passkey gate: enabled=$automaticPasskeySignInIsEnabled, " +
        "hasStarted=$automaticPasskeySignInHasStarted, mode=${authState.mode}, " +
        "lockedInitialIdentifierIsActive=$lockedInitialIdentifierIsActive, " +
        "passkeySignInConfigIsEnabled=$passkeySignInConfigIsEnabled"
    )
    if (!automaticPasskeySignInIsEnabled) {
      if (automaticPasskeySignInHasStarted) {
        authStartViewModel.cancelAutomaticPasskeySignIn()
        automaticPasskeySignInHasStarted = false
      }
      return@LaunchedEffect
    }

    if (!automaticPasskeySignInHasStarted) {
      automaticPasskeySignInHasStarted = true
      authStartViewModel.startAutomaticPasskeySignIn()
    }
  }

  DisposableEffect(authStartViewModel) {
    onDispose { authStartViewModel.cancelAutomaticPasskeySignIn() }
  }

  val signInWithBiometricsTitle = stringResource(R.string.sign_in_with_biometrics)
  val biometricPromptSubtitle =
    Clerk.applicationName?.let { stringResource(R.string.app_uses_biometrics_to_sign_you_in, it) }
      ?: stringResource(R.string.use_biometrics_to_sign_in)

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
      trailingContent = dismissTrailingContent(showDismissButton, dismissHandler),
      showSignedInUserButton = false,
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
          val onSubmit: () -> Unit = {
            if (isContinueEnabled && state !is AuthStartViewModel.AuthState.Loading) {
              if (authState.mode != AuthMode.SignUp) {
                storeIdentifierType(
                  authState = authState,
                  authViewHelper = authViewHelper,
                  phoneNumberFieldIsActive = phoneActive,
                  authStartIdentifier = authState.authStartIdentifier,
                )
              }
              authState.lastSubmittedIdentifier =
                if (phoneActive && authViewHelper.phoneNumberIsEnabled) {
                  authState.authStartPhoneNumber
                } else {
                  authState.authStartIdentifier
                }
              authState.enableInProgressAuthAttemptResume()
              authStartViewModel.startAuth(
                authMode = authState.mode,
                isPhoneNumberFieldActive = phoneActive,
                identifier = authState.authStartIdentifier,
                phoneNumber = authState.authStartPhoneNumber,
                unsafeMetadata = authState.unsafeMetadata,
              )
            }
          }

          AuthInputField(
            authViewHelper = authViewHelper,
            phoneNumberFieldIsActive = phoneActive,
            authStartPhoneNumber = authState.authStartPhoneNumber,
            authStartIdentifier = authState.authStartIdentifier,
            onPhoneNumberChange = { authState.authStartPhoneNumber = it },
            onIdentifierChange = { authState.authStartIdentifier = it },
            showPhoneBadge = lastUsedAuth?.showsPhoneBadge == true,
            showEmailUsernameBadge = lastUsedAuth?.showsEmailUsernameBadge == true,
            onSubmit = onSubmit,
            identifierLocked = authState.authStartIdentifierLocked,
            phoneNumberLocked = authState.authStartPhoneNumberLocked,
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
            onClick = onSubmit,
          )

          if (authViewHelper.showIdentifierSwitcher) {
            ClerkTextButton(
              text = authViewHelper.identifierSwitcherString(phoneActive),
              onClick = { phoneActive = !phoneActive },
            )
          }
        }

        if (
          authViewHelper.showOrDivider ||
            (trustedDeviceSignInIsAvailable && authViewHelper.showIdentifierField)
        ) {
          TextDivider(stringResource(R.string.or))
        }
        Column(verticalArrangement = Arrangement.spacedBy(dp8)) {
          if (trustedDeviceSignInIsAvailable) {
            TrustedDeviceSignInButton(
              isLoading = state is AuthStartViewModel.AuthState.TrustedDeviceState.Loading,
              showsLastUsedBadge = lastUsedAuth?.showsTrustedDeviceBadge == true,
              onClick = {
                authState.enableInProgressAuthAttemptResume()
                authStartViewModel.signInWithTrustedDevice(
                  promptTitle = signInWithBiometricsTitle,
                  promptSubtitle = biometricPromptSubtitle,
                )
              },
            )
          }

          if (lastUsedSocialProvider != null) {
            LastUsedAuthBadgeOverlay(isVisible = true, modifier = Modifier.fillMaxWidth()) {
              ClerkSocialButton(
                provider = lastUsedSocialProvider,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                  authState.enableInProgressAuthAttemptResume()
                  authStartViewModel.authenticateWithSocialProvider(
                    provider = it,
                    transferable = authState.mode.transferable,
                    preferGoogleOneTap = preferGoogleOneTap,
                    startOAuthWithSignUp = startSocialOAuthAsSignUp,
                    unsafeMetadata = authState.unsafeMetadata,
                  )
                },
                forceIconOnly = false,
              )
            }
          }

          if (socialProvidersMinusLastUsed.isNotEmpty()) {
            ClerkSocialRow(
              providers = socialProvidersMinusLastUsed.toImmutableList(),
              onClick = {
                authState.enableInProgressAuthAttemptResume()
                authStartViewModel.authenticateWithSocialProvider(
                  provider = it,
                  transferable = authState.mode.transferable,
                  preferGoogleOneTap = preferGoogleOneTap,
                  startOAuthWithSignUp = startSocialOAuthAsSignUp,
                  unsafeMetadata = authState.unsafeMetadata,
                )
              },
            )
          }
        }
      }
    }
  }
}

private fun dismissTrailingContent(
  showDismissButton: Boolean,
  onDismiss: () -> Unit,
): (@Composable () -> Unit)? {
  if (!showDismissButton) return null

  return { AuthDismissButton(onDismiss) }
}

/**
 * Resolves whether trusted-device sign-in can be offered on the auth start screen.
 *
 * Starts from fast local availability and then validates the local credential against the server,
 * cleaning up stale local state when the server no longer recognizes it.
 */
@Suppress("ReturnCount")
private suspend fun resolveTrustedDeviceSignInAvailability(): Boolean {
  if (Clerk.session?.status == Session.SessionStatus.ACTIVE) return false
  if (!Clerk.trustedDevices.localAvailability().isAvailable) return false

  return when (Clerk.trustedDevices.validateLocalCredentialIfPossible()) {
    is TrustedDeviceValidationResult.Invalid -> false
    TrustedDeviceValidationResult.Valid,
    TrustedDeviceValidationResult.Inconclusive -> true
  }
}

@Composable
private fun TrustedDeviceSignInButton(
  isLoading: Boolean,
  showsLastUsedBadge: Boolean,
  onClick: () -> Unit,
) {
  LastUsedAuthBadgeOverlay(isVisible = showsLastUsedBadge, modifier = Modifier.fillMaxWidth()) {
    ClerkButton(
      modifier = Modifier.fillMaxWidth(),
      text = stringResource(R.string.continue_with_biometrics),
      isLoading = isLoading,
      configuration =
        ClerkButtonDefaults.configuration(style = ClerkButtonConfiguration.ButtonStyle.Secondary),
      icons =
        ClerkButtonDefaults.icons(
          leadingIcon = R.drawable.ic_fingerprint,
          leadingIconColor = ClerkMaterialTheme.colors.foreground,
        ),
      onClick = onClick,
    )
  }
}

@Composable
private fun AuthDismissButton(onDismiss: () -> Unit) {
  IconButton(onClick = onDismiss) {
    Icon(
      modifier = Modifier.size(dp24),
      painter = painterResource(R.drawable.ic_cross),
      contentDescription = stringResource(R.string.close),
      tint = ClerkMaterialTheme.colors.foreground,
    )
  }
}

internal fun shouldShowAuthDismissButton(isDismissible: Boolean): Boolean = isDismissible

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
  onSubmit: () -> Unit,
  identifierLocked: Boolean,
  phoneNumberLocked: Boolean,
) {
  if (authViewHelper.phoneNumberIsEnabled && phoneNumberFieldIsActive) {
    LastUsedAuthBadgeOverlay(isVisible = showPhoneBadge) {
      ClerkPhoneNumberField(
        value = authStartPhoneNumber,
        modifier = Modifier.fillMaxWidth(),
        onValueChange = onPhoneNumberChange,
        imeAction = ImeAction.Go,
        keyboardActions = KeyboardActions(onGo = { onSubmit() }),
        enabled = !phoneNumberLocked,
      )
    }
  } else {
    LastUsedAuthBadgeOverlay(isVisible = showEmailUsernameBadge) {
      ClerkTextField(
        inputContentType = authViewHelper.identifierContentType(),
        value = authStartIdentifier,
        onValueChange = onIdentifierChange,
        label = authViewHelper.emailOrUsernamePlaceholder(),
        keyboardOptions =
          KeyboardOptions(
            keyboardType = authViewHelper.getKeyboardType(phoneNumberFieldIsActive),
            imeAction = ImeAction.Go,
          ),
        keyboardActions = KeyboardActions(onGo = { onSubmit() }),
        enabled = !identifierLocked,
      )
    }
  }
}

private fun storeIdentifierType(
  authState: AuthState,
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

  authState.storeLastUsedIdentifierType(identifierType)
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
