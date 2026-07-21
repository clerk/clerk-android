package com.clerk.api.trusteddevice

import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.Test

class TrustedDeviceKeyManagerTest {

  @Test
  fun `raw ES256 signature converts DER encoded signature`() {
    val r = ByteArray(32) { 0x01 }
    val s = byteArrayOf(0x00, 0x80.toByte()) + ByteArray(31) { 0x02 }
    val derSignature = byteArrayOf(0x30, 0x45, 0x02, 0x20) + r + byteArrayOf(0x02, 0x21) + s

    val rawSignature = DefaultTrustedDeviceKeyManager.rawES256SignatureFromDer(derSignature)

    assertContentEquals(r + byteArrayOf(0x80.toByte()) + ByteArray(31) { 0x02 }, rawSignature)
  }

  @Test
  fun `raw ES256 signature pads short DER integers`() {
    val derSignature = byteArrayOf(0x30, 0x06, 0x02, 0x01, 0x01, 0x02, 0x01, 0x02)

    val rawSignature = DefaultTrustedDeviceKeyManager.rawES256SignatureFromDer(derSignature)

    assertContentEquals(
      ByteArray(31) + byteArrayOf(0x01) + ByteArray(31) + byteArrayOf(0x02),
      rawSignature,
    )
  }

  @Test
  fun `raw ES256 signature rejects malformed DER`() {
    val exception =
      assertFailsWith<TrustedDeviceKeyManagerException> {
        DefaultTrustedDeviceKeyManager.rawES256SignatureFromDer(
          byteArrayOf(0x30, 0x03, 0x02, 0x01, 0x01)
        )
      }

    assertEquals(TrustedDeviceKeyManagerException.Code.SIGNING_FAILED, exception.code)
    assertEquals("Android Keystore returned an invalid ES256 signature.", exception.message)
  }
}
