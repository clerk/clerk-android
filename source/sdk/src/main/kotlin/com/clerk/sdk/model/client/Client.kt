package com.clerk.sdk.model.client

import com.clerk.sdk.model.session.Session
import com.clerk.sdk.model.signin.SignIn
import com.clerk.sdk.model.signup.SignUp
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * The Client object keeps track of the authenticated sessions in the current device. The device can
 * be a browser, a native application or any other medium that is usually the requesting part in a
 * request/response architecture.
 *
 * The Client object also holds information about any sign in or sign up attempts that might be in
 * progress, tracking the sign in or sign up progress.
 */
@Serializable
data class Client(
  /** Unique identifier for this client. */
  val id: String,

  /** The current sign in attempt, or null if there is none. */
  val signIn: SignIn? = null,

  /** The current sign up attempt, or null if there is none. */
  val signUp: SignUp? = null,

  /** A list of sessions that have been created on this client. */
  val sessions: List<Session>,

  /** The ID of the last active Session on this client. */
  val lastActiveSessionId: String? = null,

  /** Timestamp of last update for the client. */
  val updatedAt: Instant,
) {
  /** A list of active sessions on this client. */
  val activeSessions: List<Session>
    get() = sessions.filter { it.status == Session.SessionStatus.ACTIVE }
}
