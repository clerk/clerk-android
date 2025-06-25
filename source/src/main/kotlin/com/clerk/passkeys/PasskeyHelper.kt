package com.clerk.passkeys

import com.clerk.Clerk
import com.clerk.log.ClerkLog
import java.net.URL
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** Strategy key used in API requests to identify the authentication method */
internal const val STRATEGY_KEY = "strategy"

/** Passkey strategy identifier used in authentication requests */
private const val PASSKEY_STRATEGY_VALUE = "passkey"

/**
 * Helper utilities for passkey operations.
 *
 * Contains shared functionality used by both passkey creation and authentication services.
 */
internal object PasskeyHelper {

  /** Gets the passkey strategy value */
  val passkeyStrategy: String
    get() = PASSKEY_STRATEGY_VALUE

  /**
   * Extracts the domain from the Clerk base URL for use as the Relying Party ID.
   *
   * The domain is used to identify the relying party in WebAuthn operations. This method removes
   * the "www." prefix if present.
   *
   * @return The domain string, or an empty string if extraction fails.
   */
  fun getDomain(): String {
    return try {
      val url = URL(Clerk.baseUrl)
      val host = url.host ?: return ""
      host.replace("www.", "")
    } catch (e: Exception) {
      ClerkLog.e("Error parsing domain from baseUrl: ${e.message}")
      ""
    }
  }
}

/**
 * Request object for WebAuthn get assertion operations.
 *
 * This data class represents the structure needed for passkey authentication requests according to
 * the WebAuthn specification.
 *
 * @property challenge The cryptographic challenge string for the authentication request
 * @property allowCredentials List of allowed credentials, each containing type and ID
 * @property timeout Timeout in milliseconds for the authentication operation
 * @property userVerification Level of user verification required ("required", "preferred",
 *   "discouraged")
 * @property rpId Relying Party identifier (typically the domain)
 */
@Serializable
internal data class GetPasskeyRequest(
  val challenge: String,
  val allowCredentials: List<Map<String, String>> = emptyList(),
  val timeout: Long,
  val userVerification: String,
  val rpId: String,
)

/**
 * Object that represents a WebAuthn public key credential data
 *
 * This data class contains the essential information from a WebAuthn credential formatted for use
 * with the Clerk API.
 *
 * @property id The credential ID as a base64url-encoded string
 * @property rawId The raw credential ID as a base64url-encoded string
 * @property type The credential type (typically "public-key")
 * @property response Map containing the credential response data (attestationObject,
 *   clientDataJSON)
 */
@Serializable
internal data class PublicKeyCredentialData(
  val id: String,
  val rawId: String,
  val type: String,
  val response: Map<String, String>,
)

/**
 * Complete passkey response structure from the Android Credential Manager.
 *
 * This represents the full response structure returned by the credential manager during passkey
 * creation, before it's converted to [PublicKeyCredentialData].
 *
 * @property id The credential ID as a base64url-encoded string
 * @property rawId The raw credential ID as a base64url-encoded string
 * @property type The credential type (typically "public-key")
 * @property response JSON object containing the full credential response
 */
@Serializable
internal data class FullPasskeyResponse(
  val id: String,
  val rawId: String,
  val type: String,
  val response: JsonObject,
)
