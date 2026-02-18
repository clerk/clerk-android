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
  val id: String,
  val status: SessionStatus = SessionStatus.UNKNOWN,
  @SerialName("expire_at") val expireAt: Long,

  // Can be null if session wasn’t abandoned
  @SerialName("abandon_at") val abandonAt: Long? = null,
  @SerialName("last_active_at") val lastActiveAt: Long,
  @SerialName("latest_activity") val latestActivity: SessionActivity? = null,
  @SerialName("last_active_organization_id") val lastActiveOrganizationId: String? = null,

  // More future-proof than String?
  val actor: kotlinx.serialization.json.JsonElement? = null,
  val user: User? = null,
  @SerialName("public_user_data") val publicUserData: PublicUserData? = null,

  // New: factor_verification_age
  @SerialName("factor_verification_age") val factorVerificationAge: List<Int>? = null,
  @SerialName("created_at") val createdAt: Long,
  @SerialName("updated_at") val updatedAt: Long,
  val tasks: List<SessionTask> = emptyList(),
  @SerialName("last_active_token") val lastActiveToken: TokenResource? = null,
) {
  @Serializable
  enum class SessionStatus {
    @SerialName("abandoned") ABANDONED,
    @SerialName("active") ACTIVE,
    @SerialName("ended") ENDED,
    @SerialName("expired") EXPIRED,
    @SerialName("removed") REMOVED,
    @SerialName("replaced") REPLACED,
    @SerialName("revoked") REVOKED,
    @SerialName("unknown") UNKNOWN,
    @SerialName("pending") PENDING,
  }
}

@Serializable data class SessionTask(val key: String)

enum class SessionTaskKey {
  MFA_REQUIRED,
  UNKNOWN;

  companion object {
    fun fromRaw(rawValue: String): SessionTaskKey =
      when (rawValue.lowercase()) {
        "mfa_required",
        "mfa-required" -> MFA_REQUIRED
        else -> UNKNOWN
      }
  }
}

val SessionTask.parsedKey: SessionTaskKey
  get() = SessionTaskKey.fromRaw(key)

val Session.hasMfaRequiredTask: Boolean
  get() = tasks.any { it.parsedKey == SessionTaskKey.MFA_REQUIRED }

val Session.requiresForcedMfa: Boolean
  get() = status == Session.SessionStatus.PENDING && hasMfaRequiredTask

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
