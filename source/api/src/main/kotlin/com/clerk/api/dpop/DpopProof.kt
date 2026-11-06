package com.clerk.api.dpop

import com.clerk.api.devicekey.DeviceKey
import com.clerk.api.devicekey.Es256
import java.security.MessageDigest
import kotlin.time.Instant
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.HttpUrl

internal data class BoundRequest(val method: String, val url: HttpUrl, val accessToken: String?)

internal object DpopProof {
  fun build(key: DeviceKey, request: BoundRequest, now: Instant, jti: String): String {
    val (method, url, accessToken) = request
    val header = buildJsonObject {
      put("typ", "dpop+jwt")
      put("alg", "ES256")
      put("jwk", key.publicJwk)
    }
    val payload = buildJsonObject {
      put("jti", jti)
      put("htm", method)
      put(
        "htu",
        url.newBuilder().username("").password("").query(null).fragment(null).build().toString(),
      )
      put("iat", now.epochSeconds)
      if (accessToken != null) {
        put("ath", accessTokenHash(accessToken))
      }
      put("clerk_sl", key.securityLevel.wireName)
    }
    val signingInput = "${header.base64UrlEncode()}.${payload.base64UrlEncode()}"
    val signature = key.sign(signingInput.toByteArray(Charsets.US_ASCII))
    return "$signingInput.${Es256.base64UrlEncode(signature)}"
  }

  private fun accessTokenHash(accessToken: String): String =
    Es256.base64UrlEncode(
      MessageDigest.getInstance("SHA-256").digest(accessToken.toByteArray(Charsets.UTF_8))
    )

  private fun JsonObject.base64UrlEncode(): String =
    Es256.base64UrlEncode(toString().toByteArray(Charsets.UTF_8))
}
