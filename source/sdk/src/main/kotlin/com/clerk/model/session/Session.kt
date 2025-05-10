package com.clerk.model.session

import com.clerk.model.token.TokenResource
import com.clerk.model.user.User
import com.clerk.model.userdata.PublicUserData
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * The Session object is an abstraction over an HTTP session. It models the period of information
 * exchange between a user and the server.
 *
 * The Session object includes methods for recording session activity and ending the session
 * client-side. For security reasons, sessions can also expire server-side.
 *
 * As soon as a User signs in, Clerk creates a Session for the current Client. Clients can have more
 * than one sessions at any point in time, but only one of those sessions will be active.
 *
 * In certain scenarios, a session might be replaced by another one. This is often the case with
 * mutli-session applications.
 *
 * All sessions that are expired, removed, replaced, ended or abandoned are not considered valid.
 *
 * The SessionWithActivities object is a modified Session object. It contains most of the
 * information that the Session object stores, adding extra information about the current session's
 * latest activity.
 *
 * The additional data included in the latest activity are useful for analytics purposes. A
 * SessionActivity object will provide information about the user's location, device and browser.
 *
 * While the SessionWithActivities object wraps the most important information around a Session
 * object, the two objects have entirely different methods.
 */
@Serializable
data class Session(
  /** A unique identifier for the session. */
  val id: String,

  /** The current state of the session. */
  val status: SessionStatus,

  /** The time the session expires and will cease to be active. */
  val expireAt: Instant,

  /** The time when the session was abandoned by the user. */
  val abandonAt: Instant,

  /** The time the session was last active on the client. */
  val lastActiveAt: Instant,

  /** The latest activity associated with the session. */
  val latestActivity: SessionActivity? = null,

  /** The last active organization identifier. */
  val lastActiveOrganizationId: String? = null,

  /** The JWT actor for the session. */
  val actor: String? = null,

  /** The user associated with the session. */
  val user: User? = null,

  /** Public information about the user that this session belongs to. */
  val publicUserData: PublicUserData? = null,

  /** The time the session was created. */
  val createdAt: Instant,

  /** The last time the session recorded activity of any kind. */
  val updatedAt: Instant,

  /** The last active token for the session. */
  val lastActiveToken: TokenResource? = null,
) {
  /** Represents the status of a session. */
  @Serializable
  enum class SessionStatus {
    /** The session was abandoned client-side. */
    ABANDONED,

    /** The session is valid, and all activity is allowed. */
    ACTIVE,

    /** The user signed out of the session, but the Session remains in the Client object. */
    ENDED,

    /** The period of allowed activity for this session has passed. */
    EXPIRED,

    /** The user signed out of the session, and the Session was removed from the Client object. */
    REMOVED,

    /**
     * The session has been replaced by another one, but the Session remains in the Client object.
     */
    REPLACED,

    /** The application ended the session, and the Session was removed from the Client object. */
    REVOKED,

    /** Unknown session status. */
    UNKNOWN,
  }
}

/**
 * A `SessionActivity` object will provide information about the user's location, device and
 * browser.
 */
@Serializable
data class SessionActivity(
  /** A unique identifier for the session activity record. */
  val id: String,

  /** The name of the browser from which this session activity occurred. */
  val browserName: String? = null,

  /** The version of the browser from which this session activity occurred. */
  val browserVersion: String? = null,

  /** The type of the device which was used in this session activity. */
  val deviceType: String? = null,

  /** The IP address from which this session activity originated. */
  val ipAddress: String? = null,

  /** The city from which this session activity occurred. Resolved by IP address geo-location. */
  val city: String? = null,

  /** The country from which this session activity occurred. Resolved by IP address geo-location. */
  val country: String? = null,

  /**
   * Will be set to true if the session activity came from a mobile device. Set to false otherwise.
   */
  val isMobile: Boolean? = null,
)
