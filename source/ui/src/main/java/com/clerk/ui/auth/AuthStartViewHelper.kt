package com.clerk.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.clerk.api.Clerk
import com.clerk.api.sso.OAuthProvider
import com.clerk.ui.R

private const val EMAIL_ADDRESS = "email_address"

private const val USERNAME = "username"

private const val PHONE_NUMBER = "phone_number"

internal interface AuthStartConfig {
  val enabledFirstFactorAttributes: List<String>
  val authenticatableSocialProviders: List<OAuthProvider>
  val applicationName: String?
  val passkeyFirstFactorIsEnabled: Boolean
  val passkeyAutofillIsEnabled: Boolean
  val biometricSignInIsEnabled: Boolean
}

internal object ClerkAuthStartConfig : AuthStartConfig {
  override val enabledFirstFactorAttributes: List<String>
    get() = Clerk.enabledFirstFactorAttributes

  override val authenticatableSocialProviders: List<OAuthProvider>
    get() =
      Clerk.socialProviders.values
        .filter { it.enabled && it.authenticatable }
        .map { OAuthProvider.fromStrategy(it.strategy) }

  override val applicationName: String?
    get() = Clerk.applicationName

  override val passkeyFirstFactorIsEnabled: Boolean
    get() = Clerk.passkeyFirstFactorIsEnabled

  override val passkeyAutofillIsEnabled: Boolean
    get() = Clerk.passkeyAutofillIsEnabled

  override val biometricSignInIsEnabled: Boolean
    get() = Clerk.biometricSignInIsEnabled
}

internal data class FixedAuthStartConfig(
  override val enabledFirstFactorAttributes: List<String> = emptyList(),
  override val authenticatableSocialProviders: List<OAuthProvider> = emptyList(),
  override val applicationName: String? = null,
  override val passkeyFirstFactorIsEnabled: Boolean = false,
  override val passkeyAutofillIsEnabled: Boolean = false,
  override val biometricSignInIsEnabled: Boolean = false,
) : AuthStartConfig

@Stable
internal class AuthStartViewHelper(private val config: AuthStartConfig = ClerkAuthStartConfig) {

  val authenticatableSocialProviders: List<OAuthProvider>
    get() = config.authenticatableSocialProviders

  val emailIsEnabled: Boolean
    get() = config.enabledFirstFactorAttributes.contains(EMAIL_ADDRESS)

  val usernameIsEnabled: Boolean
    get() = config.enabledFirstFactorAttributes.contains(USERNAME)

  val phoneNumberIsEnabled: Boolean
    get() = config.enabledFirstFactorAttributes.contains(PHONE_NUMBER)

  val showIdentifierSwitcher: Boolean
    get() = (emailIsEnabled || usernameIsEnabled) && phoneNumberIsEnabled

  val showIdentifierField: Boolean
    get() = emailIsEnabled || usernameIsEnabled || phoneNumberIsEnabled

  val showOrDivider: Boolean
    get() = authenticatableSocialProviders.isNotEmpty() && showIdentifierField

  val passkeySignInConfigIsEnabled: Boolean
    get() = config.passkeyFirstFactorIsEnabled && config.passkeyAutofillIsEnabled

  val biometricSignInConfigIsEnabled: Boolean
    get() = config.biometricSignInIsEnabled

  fun getKeyboardType(isPhoneNumberFieldActive: Boolean): KeyboardType {
    return if (isPhoneNumberFieldActive) {
      KeyboardType.Phone
    } else {
      if (emailIsEnabled && !usernameIsEnabled) {
        KeyboardType.Email
      } else {
        KeyboardType.Text
      }
    }
  }

  fun identifierContentType(): ContentType {
    return when {
      emailIsEnabled && usernameIsEnabled -> ContentType.EmailAddress + ContentType.Username
      emailIsEnabled -> ContentType.EmailAddress
      else -> ContentType.Username
    }
  }

  fun continueIsDisabled(
    isPhoneNumberFieldActive: Boolean,
    identifier: String,
    phoneNumber: String,
  ): Boolean {
    return if (isPhoneNumberFieldActive) {
      phoneNumber.isEmpty()
    } else {
      identifier.isEmpty()
    }
  }

  fun shouldStartOnPhoneNumber(authStartPhoneNumber: String, authStartIdentifier: String): Boolean {
    val identifierFieldIsEnabled = emailIsEnabled || usernameIsEnabled

    return when {
      !phoneNumberIsEnabled -> false
      !identifierFieldIsEnabled -> true
      authStartPhoneNumber.isNotEmpty() && authStartIdentifier.isEmpty() -> true
      else -> false
    }
  }

  @Composable
  fun titleString(authMode: AuthMode): String {
    return when (authMode) {
      AuthMode.SignIn,
      AuthMode.SignInOrUp -> {
        val appName = config.applicationName
        if (appName != null) {
          stringResource(R.string.continue_to, appName)
        } else {
          stringResource(R.string.continue_text)
        }
      }
      AuthMode.SignUp -> stringResource(R.string.create_your_account)
    }
  }

  @Composable
  fun subtitleString(authMode: AuthMode): String {
    return when (authMode) {
      AuthMode.SignIn,
      AuthMode.SignInOrUp -> stringResource(R.string.welcome_sign_in_to_continue)
      AuthMode.SignUp -> stringResource(R.string.welcome_please_fill_in_the_details_to_get_started)
    }
  }

  @Composable
  fun identifierSwitcherString(isPhoneNumberFieldActive: Boolean): String {
    return if (isPhoneNumberFieldActive) {
      when {
        emailIsEnabled && usernameIsEnabled ->
          stringResource(R.string.use_email_address_or_username)
        emailIsEnabled -> stringResource(R.string.use_email_address)
        usernameIsEnabled -> stringResource(R.string.use_username)
        else -> ""
      }
    } else {
      stringResource(R.string.use_phone_number)
    }
  }

  @Composable
  fun emailOrUsernamePlaceholder(): String {
    return when {
      (emailIsEnabled && !usernameIsEnabled) -> stringResource(R.string.enter_your_email)
      (!emailIsEnabled && usernameIsEnabled) -> stringResource(R.string.enter_your_username)
      else -> stringResource(R.string.enter_your_email_or_username)
    }
  }
}

internal fun shouldStartAutomaticPasskeySignIn(
  authMode: AuthMode,
  lockedInitialIdentifierIsActive: Boolean,
  passkeySignInConfigIsEnabled: Boolean,
): Boolean {
  return authMode != AuthMode.SignUp &&
    !lockedInitialIdentifierIsActive &&
    passkeySignInConfigIsEnabled
}
