package com.clerk.ui.auth

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
}
