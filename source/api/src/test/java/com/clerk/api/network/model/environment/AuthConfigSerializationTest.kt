package com.clerk.api.network.model.environment

import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthConfigSerializationTest {

  @After
  fun tearDown() {
    Clerk.environment = null
  }

  @Test
  fun `session minter deserializes correctly`() {
    val authConfig =
      ClerkApi.json.decodeFromString<AuthConfig>(authConfigJson(sessionMinter = true))

    assertTrue(authConfig.sessionMinter)
  }

  @Test
  fun `session minter defaults to false when the instance does not report it`() {
    val authConfig =
      ClerkApi.json.decodeFromString<AuthConfig>(authConfigJson(sessionMinter = null))

    assertFalse(authConfig.sessionMinter)
  }

  @Test
  fun `the flag is off when no environment has been loaded`() {
    Clerk.environment = null

    assertFalse(Clerk.sessionMinterIsEnabled)
  }

  private fun authConfigJson(sessionMinter: Boolean?): String {
    val sessionMinterJson = sessionMinter?.let { ""","session_minter":$it""" }.orEmpty()

    return """{"single_session_mode":false$sessionMinterJson}"""
  }
}
