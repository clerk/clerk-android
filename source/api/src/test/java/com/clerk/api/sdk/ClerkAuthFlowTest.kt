package com.clerk.api.sdk

import com.clerk.api.Clerk
import com.clerk.api.auth.AuthEvent
import com.clerk.api.network.model.client.Client
import com.clerk.api.session.Session
import com.clerk.api.signin.SignIn
import com.clerk.api.user.User
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ClerkAuthFlowTest {
  @Before
  fun setUp() {
    Clerk.updateClient(Client())
  }

  @After
  fun tearDown() {
    Clerk.updateClient(Client())
  }

  @Test
  fun `isAuthFlowComplete is false when signed out`() {
    assertFalse(Clerk.isAuthFlowComplete)
    assertFalse(Clerk.isAuthFlowCompleteFlow.value)
  }

  @Test
  fun `isAuthFlowComplete requires an active session with a user`() {
    val user = mockk<User>(relaxed = true)
    val pendingSession = session(status = Session.SessionStatus.PENDING, user = user)

    Clerk.updateClient(client(pendingSession))
    assertFalse(Clerk.isAuthFlowComplete)

    Clerk.updateClient(client(pendingSession.copy(status = Session.SessionStatus.ACTIVE)))
    assertTrue(Clerk.isAuthFlowComplete)

    Clerk.updateClient(
      client(pendingSession.copy(status = Session.SessionStatus.ACTIVE, user = null))
    )
    assertFalse(Clerk.isAuthFlowComplete)
  }

  @Test
  fun `completed authentication holds registered auth flow until post-auth completes`() {
    val registration = requireNotNull(Clerk.registerAuthFlow())
    try {
      val completion = completedSignIn()
      Clerk.updateClient(
        client(session(status = Session.SessionStatus.ACTIVE, user = mockk(relaxed = true))),
        completedAuthFlow = completion,
      )

      assertFalse(Clerk.isAuthFlowComplete)
      assertEquals(completion, Clerk.pendingAuthFlowCompletion)

      Clerk.markAuthFlowComplete()

      assertTrue(Clerk.isAuthFlowComplete)
      assertNull(Clerk.pendingAuthFlowCompletion)
    } finally {
      registration.close()
    }
  }

  @Test
  fun `active client refresh does not hold registered auth flow`() {
    val registration = requireNotNull(Clerk.registerAuthFlow())
    try {
      Clerk.updateClient(
        client(session(status = Session.SessionStatus.ACTIVE, user = mockk(relaxed = true)))
      )

      assertTrue(Clerk.isAuthFlowComplete)
      assertNull(Clerk.pendingAuthFlowCompletion)
    } finally {
      registration.close()
    }
  }

  @Test
  fun `registerAuthFlow ignores an existing active user session`() {
    Clerk.updateClient(
      client(session(status = Session.SessionStatus.ACTIVE, user = mockk(relaxed = true)))
    )

    val registration = Clerk.registerAuthFlow()
    Clerk.markAuthFlowPending()

    assertNull(registration)
    assertTrue(Clerk.isAuthFlowComplete)
  }

  @Test
  fun `closing older registration does not clear newer auth flow`() {
    val previousRegistration = requireNotNull(Clerk.registerAuthFlow())
    val currentRegistration = requireNotNull(Clerk.registerAuthFlow())
    try {
      previousRegistration.close()
      Clerk.updateClient(
        client(session(status = Session.SessionStatus.ACTIVE, user = mockk(relaxed = true))),
        completedAuthFlow = completedSignIn(),
      )

      assertFalse(Clerk.isAuthFlowComplete)
    } finally {
      currentRegistration.close()
    }
  }

  @Test
  fun `restored pending session keeps auth flow held after activation`() {
    val pendingSession =
      session(status = Session.SessionStatus.PENDING, user = mockk(relaxed = true))
    Clerk.updateClient(client(pendingSession))
    val registration = requireNotNull(Clerk.registerAuthFlow())
    try {
      Clerk.updateClient(client(pendingSession.copy(status = Session.SessionStatus.ACTIVE)))

      assertFalse(Clerk.isAuthFlowComplete)

      Clerk.markAuthFlowComplete()
      assertTrue(Clerk.isAuthFlowComplete)
    } finally {
      registration.close()
    }
  }

  @Test
  fun `closing auth flow registration clears pending hold`() {
    val registration = requireNotNull(Clerk.registerAuthFlow())
    Clerk.updateClient(
      client(session(status = Session.SessionStatus.ACTIVE, user = mockk(relaxed = true))),
      completedAuthFlow = completedSignIn(),
    )

    registration.close()

    assertTrue(Clerk.isAuthFlowComplete)
    assertNull(Clerk.pendingAuthFlowCompletion)
  }

  private fun client(session: Session): Client =
    Client(id = "client_123", sessions = listOf(session), lastActiveSessionId = session.id)

  private fun session(status: Session.SessionStatus, user: User?): Session =
    Session(
      id = "session_123",
      status = status,
      expireAt = 10_000,
      lastActiveAt = 1_000,
      user = user,
      createdAt = 1_000,
      updatedAt = 1_000,
    )

  private fun completedSignIn(): AuthEvent.SignInCompleted =
    AuthEvent.SignInCompleted(
      SignIn(id = "sign_in_123", status = SignIn.Status.COMPLETE, createdSessionId = "session_123")
    )
}
