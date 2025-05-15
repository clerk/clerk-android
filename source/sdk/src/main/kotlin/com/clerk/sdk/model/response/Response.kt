package com.clerk.sdk.model.response

import com.clerk.sdk.model.client.Client
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
