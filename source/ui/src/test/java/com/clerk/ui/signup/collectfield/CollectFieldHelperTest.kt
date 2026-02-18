package com.clerk.ui.signup.collectfield

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectFieldHelperTest {

  private val helper = CollectFieldHelper()

  @Test
  fun fieldIsOptionalReturnsFalseWhenFieldIsRequired() {
    val isOptional =
      helper.fieldIsOptional(CollectField.Email, requiredFields = listOf("email_address"))

    assertFalse(isOptional)
  }

  @Test
  fun fieldIsOptionalReturnsTrueWhenFieldIsNotRequired() {
    val isOptional =
      helper.fieldIsOptional(CollectField.Phone, requiredFields = listOf("email_address"))

    assertTrue(isOptional)
  }

  @Test
  fun fieldIsOptionalReturnsTrueWhenRequiredFieldsAreUnavailable() {
    val isOptional = helper.fieldIsOptional(CollectField.Username, requiredFields = null)

    assertTrue(isOptional)
  }
}
