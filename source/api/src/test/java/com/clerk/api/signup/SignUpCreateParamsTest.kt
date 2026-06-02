package com.clerk.api.signup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SignUpCreateParamsTest {

  @Test
  fun `standard create params include unsafe metadata when provided`() {
    val unsafeMetadataJson = """{"test":"test"}"""
    val params =
      SignUp.CreateParams.Standard(
        emailAddress = "user@example.com",
        unsafeMetadata = unsafeMetadataJson,
      )

    val paramsMap = params.toMap()

    assertEquals("user@example.com", paramsMap["email_address"])
    assertEquals(unsafeMetadataJson, paramsMap["unsafe_metadata"])
  }

  @Test
  fun `standard create params exclude unsafe metadata when omitted`() {
    val params = SignUp.CreateParams.Standard(emailAddress = "user@example.com")

    val paramsMap = params.toMap()

    assertFalse(paramsMap.containsKey("unsafe_metadata"))
  }
}
