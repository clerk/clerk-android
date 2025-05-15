package com.clerk.sdk.model.response

import com.clerk.sdk.model.client.Client
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a response from the Clerk API that includes a client object.
 *
 * Clerk has a concept of "piggybacking" a client object on top of the response. This means that the
 * response can contain additional information about the client, such as its ID, sessions, and
 * authentication status.
 *
 * @property response The response object containing the main data.
 * @property client The client object associated with the response, if available.
 * @see
 */
@Serializable
data class ClientPiggybackedResponse(
  @SerialName("response") val response: Response,
  val client: Client? = null,
)

@Serializable
data class Response(
  @SerialName("object") val objectType: String,
  val id: String,
  val sessions: List<String> = emptyList(),
  @SerialName("sign_in") val signIn: String? = null,
  @SerialName("sign_up") val signUp: String? = null,
  @SerialName("last_active_session_id") val lastActiveSessionId: String? = null,
  @SerialName("cookie_expires_at") val cookieExpiresAt: Long? = null,
  @SerialName("captcha_bypass") val captchaBypass: Boolean? = null,
  @SerialName("created_at") val createdAt: Long? = null,
  @SerialName("updated_at") val updatedAt: Long? = null,
)
