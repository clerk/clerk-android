package com.clerk.api.session

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionLifetimeTest {

  @Test
  fun `a zero expire_at has no maximum lifetime`() {
    assertFalse(indefiniteSession().hasMaximumLifetime)
  }

  @Test
  fun `a future expire_at has a maximum lifetime`() {
    assertTrue(indefiniteSession().copy(expireAt = 2_000_000_000_000L).hasMaximumLifetime)
  }

  @Test
  fun `a revoked session with a zero expire_at has no maximum lifetime`() {
    assertFalse(indefiniteSession().copy(status = Session.SessionStatus.REVOKED).hasMaximumLifetime)
  }

  @Test
  fun `zero deadlines decode as zero`() {
    val session = indefiniteSession()

    assertEquals(0L, session.expireAt)
    assertEquals(0L, session.abandonAt)
    assertEquals(Session.SessionStatus.ACTIVE, session.status)
  }

  @Test
  fun `an indefinite session survives a serialization round trip`() {
    val session = indefiniteSession()

    assertEquals(session, Json.decodeFromString<Session>(Json.encodeToString(session)))
  }

  private fun indefiniteSession(): Session =
    Json.decodeFromString(
      """{
        "id":"sess_native","status":"active","expire_at":0,"abandon_at":0,
        "last_active_at":1,"created_at":1,"updated_at":1
      }"""
    )
}
