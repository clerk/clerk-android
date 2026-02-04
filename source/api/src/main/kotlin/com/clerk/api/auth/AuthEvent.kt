package com.clerk.api.auth

import com.clerk.api.session.Session
import com.clerk.api.signin.SignIn
import com.clerk.api.signup.SignUp
import com.clerk.api.user.User

/**
 * Represents authentication events emitted by the Auth class.
 *
 * Subscribe to [Auth.events] to receive these events and react to authentication state changes.
 */
sealed interface AuthEvent {
  /**
   * Emitted when a user successfully signs in.
   *
   * @property session The newly created session.
   * @property user The authenticated user.
   */
  data class SignedIn(val session: Session, val user: User) : AuthEvent

  /** Emitted when a user signs out. */
  data object SignedOut : AuthEvent

  /**
   * Emitted when the current session changes.
   *
   * This event fires whenever [Clerk.session] changes, which is based on
   * [Client.lastActiveSessionId]. The session may have any status (including PENDING).
   *
   * @property session The new current session, or null if no session exists.
   */
  data class SessionChanged(val session: Session?) : AuthEvent

  /**
   * Emitted when a sign-in flow is started.
   *
   * @property signIn The sign-in object representing the current flow.
   */
  data class SignInStarted(val signIn: SignIn) : AuthEvent

  /**
   * Emitted when a sign-up flow is started.
   *
   * @property signUp The sign-up object representing the current flow.
   */
  data class SignUpStarted(val signUp: SignUp) : AuthEvent

  /**
   * Emitted when a sign-in flow is completed.
   *
   * @property signIn The completed sign-in object.
   */
  data class SignInCompleted(val signIn: SignIn) : AuthEvent

  /**
   * Emitted when a sign-up flow is completed.
   *
   * @property signUp The completed sign-up object.
   */
  data class SignUpCompleted(val signUp: SignUp) : AuthEvent

  /**
   * Emitted when an authentication error occurs.
   *
   * @property message A description of the error.
   * @property throwable The underlying exception, if available.
   */
  data class Error(val message: String, val throwable: Throwable? = null) : AuthEvent
}
