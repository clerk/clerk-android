package com.clerk.ui.signin

import com.clerk.api.auth.types.Strategy
import com.clerk.api.network.model.factor.Factor
import com.clerk.ui.signin.code.SignInFactorCodeUiHelper
import com.clerk.ui.signin.code.VerificationState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SignInFactorCodeHelperTest {

  private val helper = SignInFactorCodeUiHelper

  @Test
  fun `getShowResendValue with Default state returns true`() {
    assertTrue(helper.getShowResendValue(VerificationState.Default))
  }

  @Test
  fun `getShowResendValue with Error state returns true`() {
    assertTrue(helper.getShowResendValue(VerificationState.Error))
  }

  @Test
  fun `getShowResendValue with Verifying state returns false`() {
    assertFalse(helper.getShowResendValue(VerificationState.Verifying))
  }

  @Test
  fun `getShowResendValue with Success state returns false`() {
    assertFalse(helper.getShowResendValue(VerificationState.Success))
  }

  @Test
  fun `showResend with totp strategy returns false for Default state`() {
    val factor = Factor(strategy = "totp")
    assertFalse(helper.showResend(factor, VerificationState.Default))
  }

  @Test
  fun `showResend with totp strategy returns false for Error state`() {
    val factor = Factor(strategy = "totp")
    assertFalse(helper.showResend(factor, VerificationState.Error))
  }

  @Test
  fun `showResend with other strategy and Default state returns true`() {
    val factor = Factor(strategy = Strategy.PhoneCode.value)
    assertTrue(helper.showResend(factor, VerificationState.Default))
  }

  @Test
  fun `showResend with other strategy and Verifying state returns false`() {
    val factor = Factor(strategy = Strategy.EmailCode.value)
    assertFalse(helper.showResend(factor, VerificationState.Verifying))
  }

  @Test
  fun `showUseAnotherMethod with reset_password_email_code strategy returns false`() {
    val factor = Factor(strategy = Strategy.ResetPasswordEmailCode.value)
    assertFalse(helper.showUseAnotherMethod(factor))
  }

  @Test
  fun `showUseAnotherMethod with reset_password_phone_code strategy returns false`() {
    val factor = Factor(strategy = Strategy.ResetPasswordPhoneCode.value)
    assertFalse(helper.showUseAnotherMethod(factor))
  }

  @Test
  fun `showUseAnotherMethod with phone_code strategy returns true`() {
    val factor = Factor(strategy = Strategy.PhoneCode.value)
    assertTrue(helper.showUseAnotherMethod(factor))
  }

  @Test
  fun `showUseAnotherMethod with email_code strategy returns true`() {
    val factor = Factor(strategy = Strategy.EmailCode.value)
    assertTrue(helper.showUseAnotherMethod(factor))
  }

  @Test
  fun `showUseAnotherMethod with totp strategy returns true`() {
    val factor = Factor(strategy = "totp")
    assertTrue(helper.showUseAnotherMethod(factor))
  }

  // Note: Testing Composable functions like titleForStrategy() and resendString()
  // requires a Compose test environment (e.g., using ComposeTestRule) and
  // is not covered in this standard JUnit test class.
}
