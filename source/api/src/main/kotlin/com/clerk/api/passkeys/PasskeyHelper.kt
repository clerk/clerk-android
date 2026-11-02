package com.clerk.api.passkeys

import com.clerk.api.Clerk
import com.clerk.api.log.ClerkLogger
import java.net.URL
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

internal object PasskeyHelper {
  fun getDomain(): String? {
    return try {
      URL(Clerk.baseUrl).host?.removePrefix("www.")?.takeIf { it.isNotEmpty() }
    } catch (e: Exception) {
      ClerkLogger.e("Error parsing domain from baseUrl: ${e.message}")
      null
    }
  }
}

@Serializable
internal data class GetPasskeyRequest(
  val challenge: String,
  val allowCredentials: List<Map<String, String>> = emptyList(),
  val timeout: Long,
  val userVerification: String,
  val rpId: String,
)

@Serializable
internal data class PublicKeyCredentialData(
  val id: String,
  val rawId: String,
  val type: String,
  val response: Map<String, String>,
)

@Serializable
internal data class FullPasskeyResponse(
  val id: String,
  val rawId: String,
  val type: String,
  val response: JsonObject,
)
