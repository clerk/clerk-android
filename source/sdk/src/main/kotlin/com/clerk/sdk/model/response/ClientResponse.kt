package com.clerk.sdk.model.response

import com.clerk.sdk.model.client.Client
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse(
  @SerialName("response") val clientResponse: ClientResponse,
  val client: Client? = null,
)

@Serializable
data class ClientResponse(
  @SerialName("object") val objectType: String,
  val id: String,
  val sessions: List<String> = emptyList(),
  @SerialName("sign_in") val signIn: String? = null,
  @SerialName("sign_up") val signUp: String? = null,
  @SerialName("last_active_session_id") val lastActiveSessionId: String? = null,
  @SerialName("cookie_expires_at") val cookieExpiresAt: Long? = null,
  @SerialName("captcha_bypass") val captchaBypass: Boolean,
  @SerialName("created_at") val createdAt: Long,
  @SerialName("updated_at") val updatedAt: Long,
)
