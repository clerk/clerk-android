package com.clerk.ui.core.extensions

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StringExtensionsTest {

  @Test
  fun recognizesValidEmailAddresses() {
    listOf(
        "test@example.com",
        "user.name@domain.co.uk",
        "test123+tag@example.org",
        "user_name@test-domain.org",
        "user@sub-domain.example.com",
      )
      .forEach { assertTrue(it.isEmailAddress, "$it should be an email address") }
  }

  @Test
  fun rejectsInvalidEmailAddresses() {
    listOf(
        "testexample.com",
        "@example.com",
        "test@",
        "testuser",
        "test@domain",
        "test.domain.com",
        "test@@domain.com",
        "user@example..com",
        "user@.example.com",
        "user@-example.com",
        "user@example-.com",
      )
      .forEach { assertFalse(it.isEmailAddress, "$it should not be an email address") }
  }
}
