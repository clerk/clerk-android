package com.clerk.api.network.model.environment

import kotlinx.serialization.json.Json
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthConfigTest {
  @Test
  fun `decodes session minter flag`() {
    val authConfig =
      Json.decodeFromString<AuthConfig>("""{"single_session_mode":false,"session_minter":true}""")

    assertTrue(authConfig.sessionMinter)
  }

  @Test
  fun `defaults session minter to false when omitted`() {
    val authConfig = Json.decodeFromString<AuthConfig>("""{"single_session_mode":false}""")

    assertFalse(authConfig.sessionMinter)
  }
}
