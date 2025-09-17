package com.clerk.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.res.stringResource
import com.clerk.api.Clerk
import com.clerk.ui.R

private const val EMAIL_ADDRESS = "email_address"

private const val USERNAME = "username"

private const val PHONE_NUMBER = "phone_number"

@Stable
class AuthViewHelper {

  val emailIsEnabled = Clerk.enabledFirstFactorAttributes.contains(EMAIL_ADDRESS)
  val usernameIsEnabled = Clerk.enabledFirstFactorAttributes.contains(USERNAME)
  val phoneNumberIsEnabled = Clerk.enabledFirstFactorAttributes.contains(PHONE_NUMBER)
  val showIdentifierSwitcher = (emailIsEnabled || usernameIsEnabled) && phoneNumberIsEnabled
  val showIdentifierField = emailIsEnabled || usernameIsEnabled || phoneNumberIsEnabled
  val showOrDivider
    get() =
      Clerk.socialProviders.filter { it.value.authenticatable }.isNotEmpty() && showIdentifierField

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
        val appName = Clerk.applicationName
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
}
