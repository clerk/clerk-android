package com.clerk.api.devicekey

import android.util.Base64
import java.math.BigInteger
import java.security.SignatureException
import java.security.interfaces.ECPublicKey
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Es256Test {

  @Test
  fun `raw signature converts a DER encoded signature`() {
    val r = ByteArray(32) { 0x01 }
    val s = byteArrayOf(0x00, 0x80.toByte()) + ByteArray(31) { 0x02 }
    val derSignature = byteArrayOf(0x30, 0x45, 0x02, 0x20) + r + byteArrayOf(0x02, 0x21) + s

    val rawSignature = Es256.rawSignatureFromDer(derSignature)

    assertContentEquals(r + byteArrayOf(0x80.toByte()) + ByteArray(31) { 0x02 }, rawSignature)
  }

  @Test
  fun `raw signature pads short DER integers`() {
    val derSignature = byteArrayOf(0x30, 0x06, 0x02, 0x01, 0x01, 0x02, 0x01, 0x02)

    val rawSignature = Es256.rawSignatureFromDer(derSignature)

    assertContentEquals(
      ByteArray(31) + byteArrayOf(0x01) + ByteArray(31) + byteArrayOf(0x02),
      rawSignature,
    )
  }

  @Test
  fun `raw signature rejects malformed DER`() {
    assertFailsWith<SignatureException> {
      Es256.rawSignatureFromDer(byteArrayOf(0x30, 0x03, 0x02, 0x01, 0x01))
    }
  }

  @Test
  fun `raw signature rejects a high-bit DER integer without sign padding`() {
    assertFailsWith<SignatureException> {
      Es256.rawSignatureFromDer(
        byteArrayOf(0x30, 0x06, 0x02, 0x01, 0x80.toByte(), 0x02, 0x01, 0x01)
      )
    }
  }

  @Test
  fun `public JWK keeps the member order and encoding the biometric credential sends`() {
    val jwk = Es256.publicJwk(rfc9449ExampleKey)

    assertEquals(
      """{"kty":"EC","crv":"P-256","x":"$RFC9449_EXAMPLE_X","y":"$RFC9449_EXAMPLE_Y","alg":"ES256"}""",
      jwk.toString(),
    )
  }

  @Test
  fun `public JWK writes each coordinate as exactly 32 bytes`() {
    val shortX = BigInteger.ONE
    val highBitY = BigInteger.ONE.shiftLeft(255)

    val jwk = Es256.publicJwk(ecPublicKey(shortX, highBitY))

    assertContentEquals(
      ByteArray(31) + byteArrayOf(0x01),
      base64UrlDecode(jwk.getValue("x").jsonPrimitive.content),
    )
    assertContentEquals(
      byteArrayOf(0x80.toByte()) + ByteArray(31),
      base64UrlDecode(jwk.getValue("y").jsonPrimitive.content),
    )
  }

  @Test
  fun `thumbprint matches the jkt that RFC 9449 gives for its example key`() {
    val thumbprint = Es256.thumbprint(Es256.publicJwk(rfc9449ExampleKey))

    assertEquals("0ZcOCORZNYy-DWpqq30jZyJGHTN0d2HglBV3uiguA4I", thumbprint)
  }

  private companion object {
    const val RFC9449_EXAMPLE_X = "l8tFrhx-34tV3hRICRDY9zCkDlpBhF42UQUfWVAWBFs"
    const val RFC9449_EXAMPLE_Y = "9VE4jf_Ok_o64zbTTlcuNJajHmt6v9TDVrU0CdvGRDA"

    val rfc9449ExampleKey: ECPublicKey =
      ecPublicKey(
        BigInteger(1, base64UrlDecode(RFC9449_EXAMPLE_X)),
        BigInteger(1, base64UrlDecode(RFC9449_EXAMPLE_Y)),
      )

    fun base64UrlDecode(value: String): ByteArray =
      Base64.decode(value, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)

    fun ecPublicKey(x: BigInteger, y: BigInteger): ECPublicKey =
      object : ECPublicKey {
        override fun getW(): ECPoint = ECPoint(x, y)

        override fun getParams(): ECParameterSpec = error("not used")

        override fun getAlgorithm(): String = "EC"

        override fun getFormat(): String = error("not used")

        override fun getEncoded(): ByteArray = error("not used")
      }
  }
}
