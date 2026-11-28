package com.clerk.api.devicekey

import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import java.math.BigInteger
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.KeyStoreException
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeviceKeyTest {

  private val data = "GET https://clerk.example.com/v1/client".toByteArray()

  @Before
  fun deleteStoredKey() {
    JvmDeviceKey.getOrCreate().delete()
  }

  @Test
  fun `the factory signs with a key whose public JWK verifies the signature`() {
    val key = DeviceKeyFactory.create(ApplicationProvider.getApplicationContext())

    val signature = key.sign(data)

    assertEquals(64, signature.size)
    assertTrue(verifiesWithPublicJwk(key.publicJwk, data, signature))
    assertEquals(DeviceKeySecurityLevel.SOFTWARE, key.securityLevel)
  }

  @Test
  fun `getOrCreate twice returns the same key`() {
    val first = DeviceKeyFactory.create(ApplicationProvider.getApplicationContext())
    val second = DeviceKeyFactory.create(ApplicationProvider.getApplicationContext())

    assertEquals(first.thumbprint, second.thumbprint)
  }

  @Test
  fun `a factory that throws leaves the key unavailable`() {
    val failure = KeyStoreException("Keystore is locked")

    val state = DeviceKeyHolder { throw failure }.state()

    assertSame(failure, assertIs<DeviceKeyState.Unavailable>(state).cause)
  }

  @Test
  fun `an unavailable key is not created again in the same process`() {
    var loads = 0
    val holder = DeviceKeyHolder {
      loads += 1
      throw KeyStoreException("Keystore is locked")
    }

    holder.state()
    val result = holder.withKey { it.sign(data) }

    assertTrue(result.isFailure)
    assertEquals(1, loads)
  }

  @Test
  fun `a lost key is replaced once and the operation runs again with the new key`() {
    val holder = DeviceKeyHolder { JvmDeviceKey.getOrCreate() }
    val lost = assertIs<DeviceKeyState.Ready>(holder.state()).key
    lost.delete()

    val (thumbprint, signature) = holder.withKey { it.thumbprint to it.sign(data) }.getOrThrow()

    val replacement = assertIs<DeviceKeyState.Ready>(holder.state()).key
    assertNotEquals(lost.thumbprint, thumbprint)
    assertEquals(replacement.thumbprint, thumbprint)
    assertTrue(verifiesWithPublicJwk(replacement.publicJwk, data, signature))
  }

  @Test
  fun `a key lost again after its replacement stays unavailable for the process`() {
    var loads = 0
    val holder = DeviceKeyHolder {
      loads += 1
      FailingDeviceKey(DeviceKeyLostException())
    }

    val first = holder.withKey { it.sign(data) }
    val second = holder.withKey { it.sign(data) }

    assertIs<DeviceKeyLostException>(first.exceptionOrNull())
    assertIs<DeviceKeyLostException>(second.exceptionOrNull())
    assertIs<DeviceKeyState.Unavailable>(holder.state())
    assertEquals(2, loads)
  }

  @Test
  fun `a signing failure that is not a loss keeps the key`() {
    var loads = 0
    val failure = KeyStoreException("Keystore is busy")
    val holder = DeviceKeyHolder {
      loads += 1
      JvmDeviceKey.getOrCreate()
    }
    val key = assertIs<DeviceKeyState.Ready>(holder.state()).key

    val result = holder.withKey { throw failure }

    assertSame(failure, result.exceptionOrNull())
    assertSame(key, assertIs<DeviceKeyState.Ready>(holder.state()).key)
    assertEquals(1, loads)
  }

  @Test
  fun `a stale key does not sign with the key that replaced it`() {
    val stale = JvmDeviceKey.getOrCreate()
    stale.delete()
    JvmDeviceKey.getOrCreate()

    assertFailsWith<DeviceKeyLostException> { stale.sign(data) }
  }

  @Test
  fun `a failure after a replacement that is not a loss keeps the replacement`() {
    val busy = KeyStoreException("Keystore is busy")
    val keys =
      ArrayDeque(listOf(FailingDeviceKey(DeviceKeyLostException()), FailingDeviceKey(busy)))
    val holder = DeviceKeyHolder { keys.removeFirst() }
    holder.state()

    val result = holder.withKey { it.sign(data) }

    assertSame(busy, result.exceptionOrNull())
    assertEquals("failing", assertIs<DeviceKeyState.Ready>(holder.state()).key.thumbprint)
    assertTrue(keys.isEmpty())
  }

  @Test
  fun `two operations that find the same key lost replace it once`() {
    var loads = 0
    val holder = DeviceKeyHolder {
      loads += 1
      JvmDeviceKey.getOrCreate()
    }
    val lost = assertIs<DeviceKeyState.Ready>(holder.state()).key
    lost.delete()

    val result = holder.withKey { key ->
      if (key === lost) {
        holder.withKey { it.sign(data) }.getOrThrow()
      }
      key.sign(data)
    }

    assertTrue(result.isSuccess)
    assertEquals(2, loads)
  }

  private class FailingDeviceKey(private val failure: Exception) : DeviceKey {
    override val thumbprint = "failing"
    override val publicJwk = JsonObject(emptyMap())
    override val securityLevel = DeviceKeySecurityLevel.SOFTWARE

    override fun sign(data: ByteArray): ByteArray = throw failure

    override fun delete() = Unit
  }

  private fun verifiesWithPublicJwk(
    publicJwk: JsonObject,
    data: ByteArray,
    rawSignature: ByteArray,
  ): Boolean {
    val point = ECPoint(publicJwk.coordinate("x"), publicJwk.coordinate("y"))
    val publicKey = KeyFactory.getInstance("EC").generatePublic(ECPublicKeySpec(point, p256()))
    return Signature.getInstance("SHA256withECDSA").run {
      initVerify(publicKey)
      update(data)
      verify(derFromRaw(rawSignature))
    }
  }

  private fun JsonObject.coordinate(name: String): BigInteger =
    BigInteger(1, Base64.decode(getValue(name).jsonPrimitive.content, Base64.URL_SAFE))

  private fun p256(): ECParameterSpec =
    AlgorithmParameters.getInstance("EC")
      .apply { init(ECGenParameterSpec("secp256r1")) }
      .getParameterSpec(ECParameterSpec::class.java)

  private fun derFromRaw(rawSignature: ByteArray): ByteArray {
    fun derInteger(half: ByteArray): ByteArray {
      val value = BigInteger(1, half).toByteArray()
      return byteArrayOf(0x02, value.size.toByte()) + value
    }
    val body =
      derInteger(rawSignature.copyOfRange(0, 32)) + derInteger(rawSignature.copyOfRange(32, 64))
    return byteArrayOf(0x30, body.size.toByte()) + body
  }
}
