package com.clerk.ui.signin

import com.clerk.api.network.model.factor.Factor
import com.clerk.ui.core.common.StrategyKeys
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SignInFactorCodeHelperTest {

  private lateinit var helper: SignInFactorCodeHelper

  @Before
  fun setUp() {
    helper = SignInFactorCodeHelper()
  }

  // Tests for getShowResendValue
  @Test
  fun `getShowResendValue with Default state returns true`() {
    assertTrue(helper.getShowResendValue(VerificationState.Default))
  }

  @Test
  fun `getShowResendValue with Error state returns true`() {
    assertTrue(helper.getShowResendValue(VerificationState.Error("Some error")))
  }

  @Test
  fun `getShowResendValue with Verifying state returns false`() {
    assertFalse(helper.getShowResendValue(VerificationState.Verifying))
  }

  @Test
  fun `getShowResendValue with Success state returns false`() {
    assertFalse(helper.getShowResendValue(VerificationState.Success))
  }

  // Tests for showResend
  @Test
  fun `showResend with totp strategy returns false for Default state`() {
    val factor = Factor(strategy = "totp")
    assertFalse(helper.showResend(factor, VerificationState.Default))
  }

  @Test
  fun `showResend with totp strategy returns false for Error state`() {
    val factor = Factor(strategy = "totp")
    assertFalse(helper.showResend(factor, VerificationState.Error("error")))
  }

  @Test
  fun `showResend with other strategy and Default state returns true`() {
    val factor = Factor(strategy = StrategyKeys.PHONE_CODE)
    assertTrue(helper.showResend(factor, VerificationState.Default))
  }

  @Test
  fun `showResend with other strategy and Verifying state returns false`() {
    val factor = Factor(strategy = StrategyKeys.EMAIL_CODE)
    assertFalse(helper.showResend(factor, VerificationState.Verifying))
  }

  // Tests for showUseAnotherMethod
  @Test
  fun `showUseAnotherMethod with reset_password_email_code strategy returns false`() {
    val factor = Factor(strategy = StrategyKeys.RESET_PASSWORD_EMAIL_CODE)
    assertFalse(helper.showUseAnotherMethod(factor))
  }

  @Test
  fun `showUseAnotherMethod with reset_password_phone_code strategy returns false`() {
    val factor = Factor(strategy = StrategyKeys.RESET_PASSWORD_PHONE_CODE)
    assertFalse(helper.showUseAnotherMethod(factor))
  }

  @Test
  fun `showUseAnotherMethod with phone_code strategy returns true`() {
    val factor = Factor(strategy = StrategyKeys.PHONE_CODE)
    assertTrue(helper.showUseAnotherMethod(factor))
  }

  @Test
  fun `showUseAnotherMethod with email_code strategy returns true`() {
    val factor = Factor(strategy = StrategyKeys.EMAIL_CODE)
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
