package com.clerk.ui.auth

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.clerk.api.Clerk
import com.clerk.api.OAuthProvider
import com.clerk.ui.R

private const val EMAIL_ADDRESS = "email_address"

private const val USERNAME = "username"

private const val PHONE_NUMBER = "phone_number"

@Stable
internal class AuthStartViewHelper(private val clerk: Clerk) {

  // Test backdoor properties - set these for testing
  internal var testEnabledFirstFactorAttributes: List<String>? = null
  internal var testSocialProviders: List<OAuthProvider>? = null
  internal var testApplicationName: String? = null
  internal var testPasskeyIsEnabled: Boolean? = null
  internal var testPasskeyAutofillIsEnabled: Boolean? = null
  internal var testBiometricSignInIsEnabled: Boolean? = null

  val authenticatableSocialProviders: List<OAuthProvider>
    get() {
      return if (testSocialProviders != null) {
        testSocialProviders!!
      } else {
        clerk.environment.userSettings.social.values
          .filter { it.enabled && it.authenticatable }
          .map {
            OAuthProvider.fromJson(
              kotlinx.serialization.json.JsonPrimitive(it.strategy.rawValue.removePrefix("oauth_")),
              clerk.context.requireRuntime(),
            )
          }
      }
    }

  val emailIsEnabled: Boolean
    get() =
      (testEnabledFirstFactorAttributes
          ?: clerk.environment.userSettings.enabledFirstFactorIdentifiers.map { it.rawValue })
        .contains(EMAIL_ADDRESS)

  val usernameIsEnabled: Boolean
    get() =
      (testEnabledFirstFactorAttributes
          ?: clerk.environment.userSettings.enabledFirstFactorIdentifiers.map { it.rawValue })
        .contains(USERNAME)

  val phoneNumberIsEnabled: Boolean
    get() =
      (testEnabledFirstFactorAttributes
          ?: clerk.environment.userSettings.enabledFirstFactorIdentifiers.map { it.rawValue })
        .contains(PHONE_NUMBER)

  val showIdentifierSwitcher: Boolean
    get() = (emailIsEnabled || usernameIsEnabled) && phoneNumberIsEnabled

  val showIdentifierField: Boolean
    get() = emailIsEnabled || usernameIsEnabled || phoneNumberIsEnabled

  val showOrDivider: Boolean
    get() = authenticatableSocialProviders.isNotEmpty() && showIdentifierField

  val passkeySignInConfigIsEnabled: Boolean
    get() =
      (testPasskeyIsEnabled
        ?: (clerk.environment.userSettings.attributes["passkey"]?.usedForFirstFactor == true)) &&
        (testPasskeyAutofillIsEnabled
          ?: clerk.environment.userSettings.passkeySettings.allowAutofill)

  val biometricSignInConfigIsEnabled: Boolean
    get() =
      testBiometricSignInIsEnabled
        ?: (clerk.environment.authConfig.nativeSettings?.trustedDeviceSignInEnabled == true)

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

  private val applicationName: String?
    get() =
      testApplicationName
        ?: clerk.environment.displayConfig.applicationName.takeIf { it.isNotBlank() }

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
        val appName = applicationName
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

  @VisibleForTesting
  internal fun setTestValues(
    enabledFirstFactorAttributes: List<String>? = null,
    socialProviders: List<OAuthProvider>? = null,
    applicationName: String? = null,
    passkeyIsEnabled: Boolean? = null,
    passkeyAutofillIsEnabled: Boolean? = null,
    biometricSignInIsEnabled: Boolean? = null,
  ) {
    testEnabledFirstFactorAttributes = enabledFirstFactorAttributes
    testSocialProviders = socialProviders
    testApplicationName = applicationName
    testPasskeyIsEnabled = passkeyIsEnabled
    testPasskeyAutofillIsEnabled = passkeyAutofillIsEnabled
    testBiometricSignInIsEnabled = biometricSignInIsEnabled
  }

  @VisibleForTesting
  internal fun clearTestValues() {
    testEnabledFirstFactorAttributes = null
    testSocialProviders = null
    testApplicationName = null
    testPasskeyIsEnabled = null
    testPasskeyAutofillIsEnabled = null
    testBiometricSignInIsEnabled = null
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
