package com.clerk.ui.auth

import androidx.compose.ui.autofill.ContentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthStartViewHelperTest {

  @Test
  fun shouldStartOnPhoneNumberReturnsFalseWhenIdentifierMethodsAreEnabled() {
    val helper = AuthStartViewHelper()
    helper.setTestValues(enabledFirstFactorAttributes = listOf("email_address", "phone_number"))

    val result =
      helper.shouldStartOnPhoneNumber(authStartPhoneNumber = "", authStartIdentifier = "")

    assertFalse(result)
  }

  @Test
  fun shouldStartOnPhoneNumberReturnsTrueWhenPhoneIsOnlyIdentifierMethod() {
    val helper = AuthStartViewHelper()
    helper.setTestValues(enabledFirstFactorAttributes = listOf("phone_number"))

    val result =
      helper.shouldStartOnPhoneNumber(authStartPhoneNumber = "", authStartIdentifier = "")

    assertTrue(result)
  }

  @Test
  fun shouldStartOnPhoneNumberReturnsTrueWhenPhoneWasAlreadyEntered() {
    val helper = AuthStartViewHelper()
    helper.setTestValues(enabledFirstFactorAttributes = listOf("email_address", "phone_number"))

    val result =
      helper.shouldStartOnPhoneNumber(
        authStartPhoneNumber = "+13012370655",
        authStartIdentifier = "",
      )

    assertTrue(result)
  }

  @Test
  fun shouldStartOnPhoneNumberReturnsFalseWhenIdentifierWasAlreadyEntered() {
    val helper = AuthStartViewHelper()
    helper.setTestValues(enabledFirstFactorAttributes = listOf("email_address", "phone_number"))

    val result =
      helper.shouldStartOnPhoneNumber(
        authStartPhoneNumber = "",
        authStartIdentifier = "sam@example.com",
      )

    assertFalse(result)
  }

  @Test
  fun identifierContentTypeReturnsEmailAddressWhenOnlyEmailIsEnabled() {
    val helper = AuthStartViewHelper()
    helper.setTestValues(enabledFirstFactorAttributes = listOf("email_address"))

    assertEquals(ContentType.EmailAddress, helper.identifierContentType())
  }

  @Test
  fun identifierContentTypeReturnsUsernameWhenOnlyUsernameIsEnabled() {
    val helper = AuthStartViewHelper()
    helper.setTestValues(enabledFirstFactorAttributes = listOf("username"))

    assertEquals(ContentType.Username, helper.identifierContentType())
  }

  @Test
  fun identifierContentTypeReturnsEmailAndUsernameWhenBothAreEnabled() {
    val helper = AuthStartViewHelper()
    helper.setTestValues(enabledFirstFactorAttributes = listOf("email_address", "username"))

    val contentType = helper.identifierContentType()

    assertFalse(contentType == ContentType.EmailAddress)
    assertFalse(contentType == ContentType.Username)
  }

  @Test
  fun automaticPasskeySignInIsEnabledForUnlockedSignInWhenPasskeyAutofillIsEnabled() {
    val helper = AuthStartViewHelper()
    helper.setTestValues(passkeyIsEnabled = true, passkeyAutofillIsEnabled = true)

    val result =
      shouldStartAutomaticPasskeySignIn(
        authMode = AuthMode.SignIn,
        lockedInitialIdentifierIsActive = false,
        passkeySignInConfigIsEnabled = helper.passkeySignInConfigIsEnabled,
      )

    assertTrue(result)
  }

  @Test
  fun automaticPasskeySignInIsDisabledForSignUp() {
    val helper = AuthStartViewHelper()
    helper.setTestValues(passkeyIsEnabled = true, passkeyAutofillIsEnabled = true)

    val result =
      shouldStartAutomaticPasskeySignIn(
        authMode = AuthMode.SignUp,
        lockedInitialIdentifierIsActive = false,
        passkeySignInConfigIsEnabled = helper.passkeySignInConfigIsEnabled,
      )

    assertFalse(result)
  }

  @Test
  fun automaticPasskeySignInIsDisabledWhenInitialIdentifierIsLocked() {
    val helper = AuthStartViewHelper()
    helper.setTestValues(passkeyIsEnabled = true, passkeyAutofillIsEnabled = true)

    val result =
      shouldStartAutomaticPasskeySignIn(
        authMode = AuthMode.SignInOrUp,
        lockedInitialIdentifierIsActive = true,
        passkeySignInConfigIsEnabled = helper.passkeySignInConfigIsEnabled,
      )

    assertFalse(result)
  }

  @Test
  fun automaticPasskeySignInIsDisabledWhenPasskeyAutofillIsDisabled() {
    val helper = AuthStartViewHelper()
    helper.setTestValues(passkeyIsEnabled = true, passkeyAutofillIsEnabled = false)

    val result =
      shouldStartAutomaticPasskeySignIn(
        authMode = AuthMode.SignIn,
        lockedInitialIdentifierIsActive = false,
        passkeySignInConfigIsEnabled = helper.passkeySignInConfigIsEnabled,
      )

    assertFalse(result)
  }
}
