package com.clerk.api.session

import com.clerk.api.network.ClerkApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionLifetimeTest {

  @Test
  fun `a zero expire_at has no maximum lifetime`() {
    assertEquals(0L, decodeSession(deadlines = """"expire_at":0,""").expireAt)
  }

  @Test
  fun `a future expire_at has a maximum lifetime`() {
    assertNotEquals(0L, decodeSession(deadlines = """"expire_at":2000000000000,""").expireAt)
  }

  @Test
  fun `a revoked session with a zero expire_at has no maximum lifetime`() {
    val revoked =
      decodeSession(deadlines = """"expire_at":0,""").copy(status = Session.SessionStatus.REVOKED)

    assertEquals(0L, revoked.expireAt)
  }

  @Test
  fun `the deprecated hasMaximumLifetime still reports whether expire_at is set`() {
    assertFalse(decodeSession(deadlines = """"expire_at":0,""").hasMaximumLifetime)
    assertTrue(decodeSession(deadlines = """"expire_at":2000000000000,""").hasMaximumLifetime)
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
  fun `a zero abandon_at decodes as zero`() {
    assertEquals(0L, decodeSession(deadlines = """"abandon_at":0,""").abandonAt)
  }

  @Test
  fun `a session without a maximum lifetime survives a serialization round trip`() {
    val session = decodeSession(deadlines = """"expire_at":0,"abandon_at":0,""")
    val encoded = ClerkApi.json.encodeToString(Session.serializer(), session)

    assertEquals(session, ClerkApi.json.decodeFromString(Session.serializer(), encoded))
  }

  private fun decodeSession(deadlines: String): Session =
    ClerkApi.json.decodeFromString(
      Session.serializer(),
      """{"id":"sess_native","status":"active",$deadlines"last_active_at":1,"created_at":1,"updated_at":1}""",
    )
}
