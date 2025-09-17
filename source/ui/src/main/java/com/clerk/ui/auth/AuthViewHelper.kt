package com.clerk.ui.auth

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.res.stringResource
import com.clerk.api.Clerk
import com.clerk.api.network.model.environment.UserSettings
import com.clerk.api.sso.OAuthProvider
import com.clerk.ui.R

private const val EMAIL_ADDRESS = "email_address"

private const val USERNAME = "username"

private const val PHONE_NUMBER = "phone_number"

@Stable
internal class AuthViewHelper {

  // Test backdoor properties - set these for testing
  internal var testEnabledFirstFactorAttributes: List<String>? = null
  internal var testSocialProviders: List<OAuthProvider>? = null
  internal var testApplicationName: String? = null

  val authenticatableSocialProviders: List<OAuthProvider>
    get() {
      return if (testSocialProviders != null) {
        testSocialProviders!!
      } else {
        Clerk.socialProviders.values
          .filter { it.authenticatable }
          .map { OAuthProvider.fromStrategy(it.strategy) }
      }
    }

  val emailIsEnabled: Boolean
    get() =
      (testEnabledFirstFactorAttributes ?: Clerk.enabledFirstFactorAttributes).contains(
        EMAIL_ADDRESS
      )

  val usernameIsEnabled: Boolean
    get() =
      (testEnabledFirstFactorAttributes ?: Clerk.enabledFirstFactorAttributes).contains(USERNAME)

  val phoneNumberIsEnabled: Boolean
    get() =
      (testEnabledFirstFactorAttributes ?: Clerk.enabledFirstFactorAttributes).contains(
        PHONE_NUMBER
      )

  val showIdentifierSwitcher: Boolean
    get() = (emailIsEnabled || usernameIsEnabled) && phoneNumberIsEnabled

  val showIdentifierField: Boolean
    get() = emailIsEnabled || usernameIsEnabled || phoneNumberIsEnabled

  val showOrDivider: Boolean
    get() {
      val socialProviders = testSocialProviders ?: Clerk.socialProviders.values
      return socialProviders.any {
        // For testing, assume all test social providers are authenticatable
        if (testSocialProviders != null) true
        else (it as? UserSettings.SocialConfig)?.authenticatable == true
      } && showIdentifierField
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
    get() = testApplicationName ?: Clerk.applicationName

  fun shouldStartOnPhoneNumber(authStartPhoneNumber: String, authStartIdentifier: String): Boolean {
    return when {
      phoneNumberIsEnabled -> true
      !(emailIsEnabled || usernameIsEnabled) -> true
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
      else -> stringResource(R.string.enter_you_email_or_username)
    }
  }

  @VisibleForTesting
  internal fun setTestValues(
    enabledFirstFactorAttributes: List<String>? = null,
    socialProviders: List<OAuthProvider>? = null,
    applicationName: String? = null,
  ) {
    testEnabledFirstFactorAttributes = enabledFirstFactorAttributes
    testSocialProviders = socialProviders
    testApplicationName = applicationName
  }

  @VisibleForTesting
  internal fun clearTestValues() {
    testEnabledFirstFactorAttributes = null
    testSocialProviders = null
    testApplicationName = null
  }
}
