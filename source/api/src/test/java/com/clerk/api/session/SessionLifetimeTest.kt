package com.clerk.api.session

import com.clerk.api.configuration.CachedClerkState
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.environment.AuthConfig
import com.clerk.api.network.model.environment.DisplayConfig
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.model.environment.UserSettings
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionLifetimeTest {

  @Test
  fun `zero deadlines decode as zero`() {
    val session = decodeSession(deadlines = ZERO_DEADLINES)

    assertEquals(0L, session.expireAt)
    assertEquals(0L, session.abandonAt)
    assertEquals(Session.SessionStatus.ACTIVE, session.status)
  }

  @Test
  fun `a revoked session keeps zero deadlines`() {
    val session = decodeSession(deadlines = ZERO_DEADLINES, status = "revoked")

    assertEquals(0L, session.expireAt)
    assertEquals(0L, session.abandonAt)
    assertEquals(Session.SessionStatus.REVOKED, session.status)
  }

  @Test
  fun `a missing expire_at decodes as zero`() {
    assertEquals(0L, decodeSession(deadlines = "").expireAt)
  }

  @Test
  fun `a null expire_at decodes as zero`() {
    assertEquals(0L, decodeSession(deadlines = """"expire_at":null,""").expireAt)
  }

  @Test
  fun `zero deadlines survive the cached client round trip`() {
    val session = decodeSession(deadlines = ZERO_DEADLINES)
    val state =
      CachedClerkState(
        publishableKey = "pk_test_example",
        baseUrl = "https://example.clerk.accounts.dev",
        client = Client(id = "client_cached", sessions = listOf(session)),
        environment = testEnvironment(),
        clientServerFetchAtMillis = 1L,
      )

    val encoded = ClerkApi.json.encodeToString(CachedClerkState.serializer(), state)
    val restored = ClerkApi.json.decodeFromString(CachedClerkState.serializer(), encoded)

    val cachedSession =
      ClerkApi.json
        .parseToJsonElement(encoded)
        .jsonObject
        .getValue("client")
        .jsonObject
        .getValue("sessions")
        .jsonArray
        .single()
        .jsonObject
    assertNull(cachedSession["expire_at"])
    assertEquals("0", cachedSession["abandon_at"].toString())
    assertEquals(session, restored.client.sessions.single())
    assertEquals(0L, restored.client.sessions.single().expireAt)
    assertEquals(0L, restored.client.sessions.single().abandonAt)
  }

  private fun decodeSession(deadlines: String, status: String = "active"): Session =
    ClerkApi.json.decodeFromString(
      Session.serializer(),
      """{"id":"sess_native","status":"$status",$deadlines"last_active_at":1,"created_at":1,"updated_at":1}""",
    )

  private fun testEnvironment(): Environment =
    Environment(
      authConfig = AuthConfig(singleSessionMode = false),
      displayConfig =
        DisplayConfig(
          applicationName = "Test App",
          branded = true,
          logoImageUrl = "https://example.com/logo.png",
          homeUrl = "/",
          privacyPolicyUrl = null,
          termsUrl = null,
          googleOneTapClientId = null,
        ),
      userSettings =
        UserSettings(
          attributes = emptyMap(),
          signUp =
            UserSettings.SignUpUserSettings(
              customActionRequired = false,
              progressive = false,
              mode = "public",
              legalConsentEnabled = false,
            ),
          social = emptyMap(),
          actions = UserSettings.Actions(),
          passkeySettings = null,
        ),
    )

  private companion object {
    const val ZERO_DEADLINES = """"expire_at":0,"abandon_at":0,"""
  }
}
