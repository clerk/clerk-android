package com.clerk.api.session

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionLifetimeTest {
  @Test
  fun `indefinite native session survives serialization`() {
    val session =
      Json.decodeFromString<Session>(
        """{
          "id":"sess_native","status":"active","expire_at":0,"abandon_at":0,
          "last_active_at":1,"created_at":1,"updated_at":1
        }"""
      )
    assertFalse(session.hasMaximumLifetime)
    assertEquals(0L, session.expireAt)
    assertEquals(0L, session.abandonAt)
    assertEquals(session, Json.decodeFromString<Session>(Json.encodeToString(session)))
    assertEquals(Session.SessionStatus.ACTIVE, session.status)
    assertTrue(session.copy(expireAt = 2000000000000L).hasMaximumLifetime)
    assertFalse(session.copy(status = Session.SessionStatus.REVOKED).hasMaximumLifetime)
  }
}
