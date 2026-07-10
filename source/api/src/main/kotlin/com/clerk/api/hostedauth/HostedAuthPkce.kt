package com.clerk.api.hostedauth

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

internal data class HostedAuthPkce(val codeVerifier: String, val codeChallenge: String)

internal fun generateHostedAuthState(random: SecureRandom = SecureRandom()): String =
  random.nextHex(byteCount = 16)

internal fun generateHostedAuthPkce(random: SecureRandom = SecureRandom()): HostedAuthPkce {
  val codeVerifier = random.nextHex(byteCount = 32)
  return HostedAuthPkce(
    codeVerifier = codeVerifier,
    codeChallenge = hostedAuthCodeChallenge(codeVerifier),
  )
}

internal fun hostedAuthCodeChallenge(codeVerifier: String): String {
  val digest =
    MessageDigest.getInstance("SHA-256").digest(codeVerifier.toByteArray(Charsets.US_ASCII))
  return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
}

private fun SecureRandom.nextHex(byteCount: Int): String {
  val bytes = ByteArray(byteCount)
  nextBytes(bytes)
  return buildString(capacity = byteCount * 2) {
    bytes.forEach { byte ->
      val value = byte.toInt() and BYTE_MASK
      append(HEX_DIGITS[value ushr HEX_DIGIT_SHIFT])
      append(HEX_DIGITS[value and HEX_DIGIT_MASK])
    }
  }
}

private const val HEX_DIGITS = "0123456789abcdef"
private const val BYTE_MASK = 0xff
private const val HEX_DIGIT_MASK = 0x0f
private const val HEX_DIGIT_SHIFT = 4
