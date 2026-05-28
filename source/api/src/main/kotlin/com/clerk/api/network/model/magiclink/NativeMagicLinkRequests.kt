package com.clerk.api.network.model.magiclink

import com.clerk.api.Constants.Strategy.EMAIL_LINK
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class NativeMagicLinkPrepareRequest(
  val strategy: String = EMAIL_LINK,
  @SerialName("email_address_id") val emailAddressId: String,
  @SerialName("redirect_uri") val redirectUri: String,
  @SerialName("code_challenge") val codeChallenge: String,
  @SerialName("code_challenge_method") val codeChallengeMethod: String = PKCE_METHOD_S256,
) {
  fun toFields(): Map<String, String> =
    mapOf(
      "strategy" to strategy,
      "email_address_id" to emailAddressId,
      "redirect_uri" to redirectUri,
      "code_challenge" to codeChallenge,
      "code_challenge_method" to codeChallengeMethod,
    )

  companion object {
    const val PKCE_METHOD_S256 = "S256"
  }
}

@Serializable
internal data class NativeMagicLinkCompleteRequest(
  @SerialName("flow_id") val flowId: String,
  @SerialName("approval_token") val approvalToken: String,
  @SerialName("code_verifier") val codeVerifier: String,
  @SerialName("attestation") val attestation: String? = null,
) {
  fun toFields(): Map<String, String> = buildMap {
    put("flow_id", flowId)
    put("approval_token", approvalToken)
    put("code_verifier", codeVerifier)
    attestation?.let { put("attestation", it) }
  }
}

@Serializable internal data class NativeMagicLinkCompleteResponse(val ticket: String)
