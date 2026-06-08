package com.clerk.api.network.model.magiclink

import com.clerk.api.Constants.Strategy.EMAIL_LINK
import com.clerk.api.signup.SignUp
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

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

@Serializable(with = NativeMagicLinkCompleteResponseSerializer::class)
internal sealed interface NativeMagicLinkCompleteResponse {
  @Serializable data class Ticket(val ticket: String) : NativeMagicLinkCompleteResponse

  data class SignUpResult(val signUp: SignUp) : NativeMagicLinkCompleteResponse
}

internal object NativeMagicLinkCompleteResponseSerializer :
  KSerializer<NativeMagicLinkCompleteResponse> {
  override val descriptor: SerialDescriptor =
    buildClassSerialDescriptor("NativeMagicLinkCompleteResponse")

  override fun deserialize(decoder: Decoder): NativeMagicLinkCompleteResponse {
    val jsonDecoder =
      decoder as? JsonDecoder
        ?: throw SerializationException("NativeMagicLinkCompleteResponse can only decode JSON.")
    val element = jsonDecoder.decodeJsonElement()
    val objectElement =
      element as? JsonObject
        ?: throw SerializationException("NativeMagicLinkCompleteResponse must be a JSON object.")

    return if ("ticket" in objectElement) {
      jsonDecoder.json.decodeFromJsonElement<NativeMagicLinkCompleteResponse.Ticket>(element)
    } else {
      NativeMagicLinkCompleteResponse.SignUpResult(
        jsonDecoder.json.decodeFromJsonElement<SignUp>(element)
      )
    }
  }

  override fun serialize(encoder: Encoder, value: NativeMagicLinkCompleteResponse) {
    throw UnsupportedOperationException("Native magic-link complete responses are read-only.")
  }
}
