package com.clerk.api.network.model.client

import com.clerk.api.session.Session
import org.junit.Assert.assertEquals
import org.junit.Test

class ClientTest {
  private fun session(id: String, status: Session.SessionStatus): Session =
    Session(
      id = id,
      status = status,
      expireAt = 0L,
      lastActiveAt = 0L,
      createdAt = 0L,
      updatedAt = 0L,
  )

  @Test
  fun `signedInSessions returns active sessions by default`() {
    val active = session(id = "active", status = Session.SessionStatus.ACTIVE)
    val pending = session(id = "pending", status = Session.SessionStatus.PENDING)
    val ended = session(id = "ended", status = Session.SessionStatus.ENDED)

    val client = Client(sessions = listOf(active, pending, ended))

    assertEquals(listOf(active), client.signedInSessions())
  }

  @Test
  fun `signedInSessions includes pending when configured`() {
    val active = session(id = "active", status = Session.SessionStatus.ACTIVE)
    val pending = session(id = "pending", status = Session.SessionStatus.PENDING)
    val ended = session(id = "ended", status = Session.SessionStatus.ENDED)

    val client = Client(sessions = listOf(active, pending, ended))

    assertEquals(listOf(active, pending), client.signedInSessions(treatPendingAsSignedOut = false))
  }
}
