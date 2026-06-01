package com.clerk.api.magiclink

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

internal object PkceUtil {
  private val secureRandom = SecureRandom()

  fun generateCodeVerifier(bytes: Int = DEFAULT_VERIFIER_BYTES): String {
    require(bytes > 0) { "bytes must be positive" }
    val random = ByteArray(bytes)
    secureRandom.nextBytes(random)
    return encodeBase64Url(random)
  }

  fun createS256CodeChallenge(codeVerifier: String): String {
    require(codeVerifier.isNotBlank()) { "codeVerifier must not be blank" }
    val digest =
      MessageDigest.getInstance(SHA_256).digest(codeVerifier.toByteArray(Charsets.US_ASCII))
    return encodeBase64Url(digest)
  }

  fun generatePair(): PkcePair {
    val verifier = generateCodeVerifier()
    return PkcePair(verifier = verifier, challenge = createS256CodeChallenge(verifier))
  }

  private fun encodeBase64Url(bytes: ByteArray): String {
    return Base64.encodeToString(bytes, BASE64_URL_FLAGS)
  }

  private const val BASE64_URL_FLAGS = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
  private const val SHA_256 = "SHA-256"
  private const val DEFAULT_VERIFIER_BYTES = 32
}

internal data class PkcePair(val verifier: String, val challenge: String)
