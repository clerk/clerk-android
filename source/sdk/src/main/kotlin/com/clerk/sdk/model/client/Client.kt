package com.clerk.sdk.model.client

import com.clerk.sdk.model.error.ClerkErrorResponse
import com.clerk.sdk.model.response.ClientPiggybackedResponse
import com.clerk.sdk.model.session.Session
import com.clerk.sdk.network.ClerkApi
import com.clerk.sdk.network.serialization.ClerkApiResult
import com.clerk.sdk.signin.SignIn
import com.clerk.sdk.signup.SignUp
import kotlinx.serialization.SerialName
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
  val id: String? = null,

  /** The current sign in attempt, or null if there is none. */
  @SerialName("sign_in") val signIn: SignIn? = null,

  /** The current sign up attempt, or null if there is none. */
  @SerialName("sign_up") val signUp: SignUp? = null,

  /** A list of sessions that have been created on this client. */
  val sessions: List<Session> = emptyList(),

  /** The ID of the last active Session on this client. */
  @SerialName("last_active_session_id") val lastActiveSessionId: String? = null,

  /** Timestamp of last update for the client. */
  @SerialName("updated_at") val updatedAt: Long? = null,
) {

  companion object {
    /** Fetches the current client object from the Clerk API. */
    suspend fun get(): ClerkApiResult<ClientPiggybackedResponse<Client>, ClerkErrorResponse> =
      ClerkApi.instance.client()
  }
}
