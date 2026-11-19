package com.clerk.api.session

import com.clerk.api.Clerk
import com.clerk.api.network.model.client.Client
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InvalidatedSessionRemovalTest {

  @After
  fun tearDown() {
    Clerk.updateClient(Client())
    Clerk.clearSessionAndUserState()
    Clerk.stateStore.reset()
  }

  @Test
  fun `a revoked session is not published again by the next local client mutation`() {
    Clerk.updateClient(clientWithSession("sess_123"))
    assertEquals("sess_123", Clerk.session?.id)

    Clerk.removeInvalidatedSession("sess_123")

    assertNull(Clerk.session)
    assertEquals(emptyList<Session>(), Clerk.client.sessions)

    Clerk.mutateClient { client -> client.copy(updatedAt = 2_000) }

    assertNull(Clerk.session)
    assertEquals(emptyList<Session>(), Clerk.sessionsFlow.value)
  }

  @Test
  fun `removing the active session of a multi-session client promotes the next active one`() {
    Clerk.updateClient(
      clientWithSession("sess_123").let { client ->
        client.copy(sessions = client.sessions + session("sess_456"))
      }
    )

    Clerk.removeInvalidatedSession("sess_123")

    assertEquals("sess_456", Clerk.session?.id)
    assertEquals(listOf("sess_456"), Clerk.client.sessions.map { it.id })
  }

  @Test
  fun `a client response the server produced before the revocation does not restore the session`() {
    Clerk.updateClient(clientWithSession("sess_123"))
    val inFlightOrder = Clerk.stateStore.observeResponse(serverDateMillis = 5_000)

    Clerk.removeInvalidatedSession("sess_123")
    Clerk.applyClientResponse(clientWithSession("sess_123"), inFlightOrder)

    assertNull(Clerk.session)
    assertEquals(emptyList<Session>(), Clerk.client.sessions)
  }

  @Test
  fun `a revoked session stays out of responses from the same second and responses without a date`() {
    Clerk.updateClient(clientWithSession("sess_123"))

    Clerk.removeInvalidatedSession("sess_123")
    Clerk.applyClientResponse(
      clientWithSession("sess_123"),
      Clerk.stateStore.observeResponse(serverDateMillis = 5_000),
    )
    Clerk.applyClientResponse(clientWithSession("sess_123"), order = null)

    assertNull(Clerk.session)
    assertEquals(emptyList<Session>(), Clerk.sessionsFlow.value)
  }

  @Test
  fun `a later client response still publishes the sessions the server did not revoke`() {
    Clerk.updateClient(clientWithSession("sess_123"))

    Clerk.removeInvalidatedSession("sess_123")
    Clerk.applyClientResponse(
      clientWithSession("sess_123").let { client ->
        client.copy(sessions = client.sessions + session("sess_789"))
      },
      Clerk.stateStore.observeResponse(serverDateMillis = 6_000),
    )

    assertEquals("sess_789", Clerk.session?.id)
    assertEquals(listOf("sess_789"), Clerk.client.sessions.map { it.id })
  }

  @Test
  fun `a full reset forgets revoked sessions`() {
    Clerk.updateClient(clientWithSession("sess_123"))
    Clerk.removeInvalidatedSession("sess_123")

    Clerk.stateStore.reset()
    Clerk.updateClient(clientWithSession("sess_123"))

    assertEquals("sess_123", Clerk.session?.id)
  }

  private fun clientWithSession(sessionId: String): Client =
    Client(
      id = "client_123",
      sessions = listOf(session(sessionId)),
      lastActiveSessionId = sessionId,
    )

  private fun session(sessionId: String): Session =
    Session(
      id = sessionId,
      status = Session.SessionStatus.ACTIVE,
      expireAt = 10_000,
      lastActiveAt = 1_000,
      createdAt = 1_000,
      updatedAt = 1_000,
    )
}
