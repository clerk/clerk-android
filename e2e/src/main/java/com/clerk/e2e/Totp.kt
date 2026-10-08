package com.clerk.e2e

import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

internal object Totp {
  const val PERIOD_SECONDS = 30L
  const val MINIMUM_KEY_BYTES = 10
  private const val DIGITS = 6
  private const val MODULUS = 1_000_000
  private const val ALGORITHM = "SHA1"
  private const val BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
  private const val BITS_PER_BASE32_CHAR = 5
  private const val BITS_PER_BYTE = 8
  private const val BYTE_MASK = 0xFF
  private const val OFFSET_MASK = 0x0F
  private const val OTPAUTH_TOTP_PREFIX = "otpauth://totp/"

  fun code(secret: String, epochSeconds: Long): String {
    val key = base32Decode(secret)
    require(key.size >= MINIMUM_KEY_BYTES) {
      "TOTP secret decodes to ${key.size} bytes; at least $MINIMUM_KEY_BYTES are required."
    }
    val counter = ByteBuffer.allocate(Long.SIZE_BYTES).putLong(epochSeconds / PERIOD_SECONDS)
    val mac = Mac.getInstance("HmacSHA1").apply { init(SecretKeySpec(key, "HmacSHA1")) }
    val hash = mac.doFinal(counter.array())
    val offset = hash.last().toInt() and OFFSET_MASK
    val truncated =
      hash.sliceArray(offset until offset + Int.SIZE_BYTES).fold(0) { acc, byte ->
        (acc shl BITS_PER_BYTE) or (byte.toInt() and BYTE_MASK)
      } and Int.MAX_VALUE
    return (truncated % MODULUS).toString().padStart(DIGITS, '0')
  }

  fun secondsRemaining(epochSeconds: Long): Long = PERIOD_SECONDS - (epochSeconds % PERIOD_SECONDS)

  fun secretFrom(text: String): String? {
    val trimmed = text.trim()
    val secret =
      if (trimmed.startsWith("otpauth://", ignoreCase = true)) {
        secretFromOtpauthUri(trimmed)
      } else {
        trimmed
      }
    return secret?.takeIf { isBase32(it) && base32Decode(it).size >= MINIMUM_KEY_BYTES }
  }

  private fun secretFromOtpauthUri(uri: String): String? {
    if (!uri.startsWith(OTPAUTH_TOTP_PREFIX, ignoreCase = true)) return null
    val parameters =
      uri
        .substringAfter('?', missingDelimiterValue = "")
        .split('&')
        .filter { it.isNotEmpty() }
        .associate { it.substringBefore('=').lowercase() to it.substringAfter('=', "") }
    val usesSupportedParameters =
      parameters.matchesOrAbsent("algorithm") { it.equals(ALGORITHM, ignoreCase = true) } &&
        parameters.matchesOrAbsent("digits") { it.toIntOrNull() == DIGITS } &&
        parameters.matchesOrAbsent("period") { it.toLongOrNull() == PERIOD_SECONDS }
    return parameters["secret"]?.takeIf { usesSupportedParameters }
  }

  private fun Map<String, String>.matchesOrAbsent(
    name: String,
    predicate: (String) -> Boolean,
  ): Boolean = get(name)?.let(predicate) ?: true

  private fun isBase32(candidate: String): Boolean =
    candidate.uppercase().filterNot { it == '=' || it == ' ' }.all { it in BASE32_ALPHABET }

  private fun base32Decode(secret: String): ByteArray {
    val characters = secret.uppercase().filter { it in BASE32_ALPHABET }
    var buffer = 0
    var bitsLeft = 0
    val bytes = mutableListOf<Byte>()
    for (character in characters) {
      buffer = (buffer shl BITS_PER_BASE32_CHAR) or BASE32_ALPHABET.indexOf(character)
      bitsLeft += BITS_PER_BASE32_CHAR
      if (bitsLeft >= BITS_PER_BYTE) {
        bitsLeft -= BITS_PER_BYTE
        bytes += ((buffer shr bitsLeft) and BYTE_MASK).toByte()
      }
    }
    return bytes.toByteArray()
  }
}
