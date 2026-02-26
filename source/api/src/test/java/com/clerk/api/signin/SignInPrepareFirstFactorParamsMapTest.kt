package com.clerk.api.signin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SignInPrepareFirstFactorParamsMapTest {

  @Test
  fun `reset password email code maps email address id using snake case`() {
    val params =
      SignIn.PrepareFirstFactorParams.ResetPasswordEmailCode(emailAddressId = "email_123")

    val result = params.toMap()

    assertEquals("email_123", result["email_address_id"])
    assertEquals("reset_password_email_code", result["strategy"])
    assertFalse(result.containsKey("emailAddressId"))
  }

  @Test
  fun `reset password phone code maps phone number id using snake case`() {
    val params = SignIn.PrepareFirstFactorParams.ResetPasswordPhoneCode(phoneNumberId = "phone_123")

    val result = params.toMap()

    assertEquals("phone_123", result["phone_number_id"])
    assertEquals("reset_password_phone_code", result["strategy"])
    assertFalse(result.containsKey("phoneNumberId"))
  }
}
