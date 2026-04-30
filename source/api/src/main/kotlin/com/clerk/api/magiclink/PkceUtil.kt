package com.clerk.api.magiclink

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

internal object PkceUtil {
  private val secureRandom = SecureRandom()
  private val urlEncoder = Base64.getUrlEncoder().withoutPadding()

  fun generateCodeVerifier(bytes: Int = DEFAULT_VERIFIER_BYTES): String {
    require(bytes > 0) { "bytes must be positive" }
    val random = ByteArray(bytes)
    secureRandom.nextBytes(random)
    return urlEncoder.encodeToString(random)
  }

  fun createS256CodeChallenge(codeVerifier: String): String {
    require(codeVerifier.isNotBlank()) { "codeVerifier must not be blank" }
    val digest =
      MessageDigest.getInstance(SHA_256).digest(codeVerifier.toByteArray(Charsets.US_ASCII))
    return urlEncoder.encodeToString(digest)
  }

  fun generatePair(): PkcePair {
    val verifier = generateCodeVerifier()
    return PkcePair(verifier = verifier, challenge = createS256CodeChallenge(verifier))
  }

  private const val SHA_256 = "SHA-256"
  private const val DEFAULT_VERIFIER_BYTES = 32
}

internal data class PkcePair(val verifier: String, val challenge: String)
