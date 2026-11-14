package com.clerk.api.devicekey

import android.util.Base64
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SignatureException
import java.security.interfaces.ECPublicKey
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal object Es256 {
  private const val COORDINATE_SIZE_BYTES = 32
  private const val DER_SEQUENCE_TAG = 0x30
  private const val DER_INTEGER_TAG = 0x02
  private const val DER_LONG_FORM_FLAG = 0x80
  private const val DER_LENGTH_MASK = 0x7F
  private const val DER_BYTE_MASK = 0xFF

  // RFC 7638 section 3.2 hashes only the required members, in lexicographic order.
  private val THUMBPRINT_MEMBERS = listOf("crv", "kty", "x", "y")

  fun publicJwk(publicKey: ECPublicKey): JsonObject = buildJsonObject {
    put("kty", "EC")
    put("crv", "P-256")
    put("x", base64UrlEncode(fixedWidthCoordinate(publicKey.w.affineX)))
    put("y", base64UrlEncode(fixedWidthCoordinate(publicKey.w.affineY)))
    put("alg", "ES256")
  }

  fun thumbprint(publicJwk: JsonObject): String {
    val requiredMembers = JsonObject(THUMBPRINT_MEMBERS.associateWith { publicJwk.getValue(it) })
    val digest =
      MessageDigest.getInstance("SHA-256")
        .digest(requiredMembers.toString().toByteArray(Charsets.UTF_8))
    return base64UrlEncode(digest)
  }

  private fun fixedWidthCoordinate(coordinate: BigInteger): ByteArray {
    val bytes = coordinate.toByteArray()
    return when {
      bytes.size == COORDINATE_SIZE_BYTES -> bytes
      // Strip the sign byte added by BigInteger for values with the high bit set.
      bytes.size > COORDINATE_SIZE_BYTES ->
        bytes.copyOfRange(bytes.size - COORDINATE_SIZE_BYTES, bytes.size)
      else -> ByteArray(COORDINATE_SIZE_BYTES - bytes.size) + bytes
    }
  }

  fun rawSignatureFromDer(signature: ByteArray): ByteArray {
    val reader = DerReader(signature)
    validateSignature(reader.readByte() == DER_SEQUENCE_TAG)

    val sequenceLength = reader.readLength()
    validateSignature(sequenceLength == reader.remainingBytes)

    val r = reader.readInteger()
    val s = reader.readInteger()
    validateSignature(reader.remainingBytes == 0)

    return paddedComponent(r) + paddedComponent(s)
  }

  private fun validateSignature(isValid: Boolean) {
    if (!isValid) {
      throw invalidSignature()
    }
  }

  private fun paddedComponent(component: ByteArray): ByteArray {
    if (component.isEmpty() || component.first().toInt() and DER_LONG_FORM_FLAG != 0) {
      throw invalidSignature()
    }

    var startIndex = 0
    while (
      component.size - startIndex > COORDINATE_SIZE_BYTES && component[startIndex].toInt() == 0
    ) {
      startIndex += 1
    }

    val componentSize = component.size - startIndex
    if (componentSize !in 1..COORDINATE_SIZE_BYTES) {
      throw invalidSignature()
    }

    return ByteArray(COORDINATE_SIZE_BYTES).also { padded ->
      component.copyInto(
        destination = padded,
        destinationOffset = COORDINATE_SIZE_BYTES - componentSize,
        startIndex = startIndex,
      )
    }
  }

  private fun invalidSignature(): SignatureException =
    SignatureException("The signature is not a DER-encoded ES256 signature.")

  private class DerReader(private val bytes: ByteArray) {
    private var offset: Int = 0

    val remainingBytes: Int
      get() = bytes.size - offset

    fun readByte(): Int {
      if (offset >= bytes.size) {
        throw invalidSignature()
      }
      return bytes[offset++].toInt() and DER_BYTE_MASK
    }

    fun readLength(): Int {
      val first = readByte()
      if (first and DER_LONG_FORM_FLAG == 0) {
        return first
      }

      val byteCount = first and DER_LENGTH_MASK
      if (byteCount == 0 || byteCount > Int.SIZE_BYTES || byteCount > remainingBytes) {
        throw invalidSignature()
      }

      var length = 0
      repeat(byteCount) { length = (length shl Byte.SIZE_BITS) or readByte() }
      return length
    }

    fun readInteger(): ByteArray {
      if (readByte() != DER_INTEGER_TAG) {
        throw invalidSignature()
      }

      val length = readLength()
      if (length <= 0 || length > remainingBytes) {
        throw invalidSignature()
      }

      return bytes.copyOfRange(offset, offset + length).also { offset += length }
    }
  }

  fun base64UrlEncode(bytes: ByteArray): String {
    return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
  }
}
