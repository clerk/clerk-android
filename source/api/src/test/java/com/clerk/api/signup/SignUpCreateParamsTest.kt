package com.clerk.api.signup

import com.clerk.api.network.serialization.toFormFields
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SignUpCreateParamsTest {

  @Test
  fun `standard create params include unsafe metadata when provided`() {
    val unsafeMetadataJson = """{"test":"test"}"""
    val unsafeMetadata = mapOf("test" to "test")
    val params =
      SignUp.CreateParams.Standard(
        emailAddress = "user@example.com",
        unsafeMetadata = unsafeMetadata,
      )

    val paramsMap = params.toFormFields()

    assertEquals("user@example.com", paramsMap["email_address"])
    assertEquals(unsafeMetadataJson, paramsMap["unsafe_metadata"])
  }

  @Test
  fun `standard create params exclude unsafe metadata when omitted`() {
    val params = SignUp.CreateParams.Standard(emailAddress = "user@example.com")

    val paramsMap = params.toFormFields()

    assertFalse(paramsMap.containsKey("unsafe_metadata"))
  }
}
