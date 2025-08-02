package com.clerk.api.session

import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.token.TokenResource
import com.clerk.api.network.model.userdata.PublicUserData
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.user.User
import kotlinx.serialization.SerialName
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
 * multi-session applications.
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
  @SerialName("expire_at") val expireAt: Long,

  /** The time when the session was abandoned by the user. */
  @SerialName("abandon_at") val abandonAt: Long,

  /** The time the session was last active on the client. */
  @SerialName("last_active_at") val lastActiveAt: Long,

  /** The latest activity associated with the session. */
  @SerialName("latest_activity") val latestActivity: SessionActivity? = null,

  /** The last active organization identifier. */
  @SerialName("last_active_organization_id") val lastActiveOrganizationId: String? = null,

  /** The JWT actor for the session. */
  val actor: String? = null,

  /** The user associated with the session. */
  val user: User? = null,

  /** Public information about the user that this session belongs to. */
  @SerialName("public_user_data") val publicUserData: PublicUserData? = null,

  /** The time the session was created. */
  @SerialName("created_at") val createdAt: Long,

  /** The last time the session recorded activity of any kind. */
  @SerialName("updated_at") val updatedAt: Long,

  /** The last active token for the session. */
  @SerialName("last_active_token") val lastActiveToken: TokenResource? = null,
) {
  /** Represents the status of a session. */
  @Serializable
  enum class SessionStatus {
    /** The session was abandoned client-side. */
    @SerialName("abandoned") ABANDONED,

    /** The session is valid, and all activity is allowed. */
    @SerialName("active") ACTIVE,

    /** The user signed out of the session, but the Session remains in the Client object. */
    @SerialName("ended") ENDED,

    /** The period of allowed activity for this session has passed. */
    @SerialName("expired") EXPIRED,

    /** The user signed out of the session, and the Session was removed from the Client object. */
    @SerialName("removed") REMOVED,

    /**
     * The session has been replaced by another one, but the Session remains in the Client object.
     */
    @SerialName("replaced") REPLACED,

    /** The application ended the session, and the Session was removed from the Client object. */
    @SerialName("revoked") REVOKED,

    /** Unknown session status. */
    @SerialName("unknown") UNKNOWN,
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
  @SerialName("browser_name") val browserName: String? = null,

  /** The version of the browser from which this session activity occurred. */
  @SerialName("browser_version") val browserVersion: String? = null,

  /** The type of the device which was used in this session activity. */
  @SerialName("device_type") val deviceType: String? = null,

  /** The IP address from which this session activity originated. */
  @SerialName("ip_address") val ipAddress: String? = null,

  /** The city from which this session activity occurred. Resolved by IP address geo-location. */
  val city: String? = null,

  /** The country from which this session activity occurred. Resolved by IP address geo-location. */
  val country: String? = null,

  /**
   * Will be set to true if the session activity came from a mobile device. Set to false otherwise.
   */
  @SerialName("is_mobile") val isMobile: Boolean? = null,
)

/** Deletes the current session. */
suspend fun Session.delete(): ClerkResult<Client, ClerkErrorResponse> {
  return ClerkApi.session.deleteSessions()
}

/**
 * Fetches a fresh JWT for the session.
 *
 * @param options The options to use when fetching the token.
 * @return The [ClerkResult] containing the [TokenResource] if successful, or [ClerkErrorResponse]
 *   if failed.
 * @see GetTokenOptions
 */
suspend fun Session.fetchToken(
  options: GetTokenOptions = GetTokenOptions()
): ClerkResult<TokenResource, ClerkErrorResponse> {
  val token = SessionTokenFetcher().getToken(this, options)
  return if (token != null) {
    ClerkResult.success(token)
  } else {
    ClerkResult.apiFailure(ClerkErrorResponse(errors = emptyList(), clerkTraceId = "local-error"))
  }
}

/**
 * Revokes the current session.
 *
 * @return The [ClerkResult] of the revocation. If the session was revoked successfully, the result
 *   will contain the revoked session. If the session was not revoked successfully, the result will
 *   contain the error response.
 * @see ClerkResult
 * @see ClerkErrorResponse
 */
suspend fun Session.revoke(): ClerkResult<Session, ClerkErrorResponse> {
  return ClerkApi.session.revokeSession(sessionIdToRevoke = this.id)
}

/**
 * Convenience accessor to tell if the given session is the current device. Used mostly for
 * constructing the User profile security view.
 */
val Session.isThisDevice: Boolean
  get() = this.id == Clerk.session?.id
