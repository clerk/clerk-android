package com.clerk.api.devicekey

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.UnrecoverableKeyException
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assume.assumeTrue
import org.junit.Test

class DeviceKeyInstrumentedTest {
  private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
  private val arguments = InstrumentationRegistry.getArguments()
  private val data = "GET https://clerk.example.com/v1/client".toByteArray()

  @Test
  fun createsKeyAndReportsLevel() {
    val holder = DeviceKeyHolder { DeviceKeyFactory.create(context) }

    val key = assertIs<DeviceKeyState.Ready>(holder.state()).key
    assertIs<DeviceKeyState.Ready>(holder.state())
    val keyInfo = storedKeyInfo()
    val (keyInfoLevel, expectedLevel) =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        keyInfo.securityLevel to
          mapOf(
            KeyProperties.SECURITY_LEVEL_SOFTWARE to DeviceKeySecurityLevel.SOFTWARE,
            KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT to DeviceKeySecurityLevel.TEE,
            KeyProperties.SECURITY_LEVEL_STRONGBOX to DeviceKeySecurityLevel.STRONGBOX,
            KeyProperties.SECURITY_LEVEL_UNKNOWN_SECURE to DeviceKeySecurityLevel.TEE,
            KeyProperties.SECURITY_LEVEL_UNKNOWN to DeviceKeySecurityLevel.SOFTWARE,
          )[keyInfo.securityLevel]
      } else {
        @Suppress("DEPRECATION") val insideSecureHardware = keyInfo.isInsideSecureHardware
        insideSecureHardware to
          if (insideSecureHardware) DeviceKeySecurityLevel.TEE else DeviceKeySecurityLevel.SOFTWARE
      }

    report(
      "level=${key.securityLevel.wireName} keyinfo_level=$keyInfoLevel " +
        "api=${Build.VERSION.SDK_INT} thumbprint=${key.thumbprint}"
    )
    assertIs<AndroidKeystoreDeviceKey>(key)
    assertEquals(expectedLevel, key.securityLevel, "KeyInfo level $keyInfoLevel")
    assertTrue(verifies(key.publicJwk, data, key.sign(data)))
  }

  @Test
  fun reusesKeyAcrossProcesses() {
    val previous = arguments.getString("previousThumbprint")
    assumeTrue("pass the thumbprint of an earlier process as previousThumbprint", previous != null)

    val key = AndroidKeystoreDeviceKey.getOrCreate(context)

    report("thumbprint=${key.thumbprint} previousThumbprint=$previous")
    assertEquals(previous, key.thumbprint)
  }

  @Test
  fun deleteThenCreateRotates() {
    val first = AndroidKeystoreDeviceKey.getOrCreate(context)
    val second = AndroidKeystoreDeviceKey.getOrCreate(context)
    first.delete()
    assertFailsWith<DeviceKeyLostException> { first.sign(data) }
    val third = AndroidKeystoreDeviceKey.getOrCreate(context)
    assertFailsWith<DeviceKeyLostException> { first.sign(data) }
    second.delete()
    val fourth = AndroidKeystoreDeviceKey.getOrCreate(context)

    report("first=${first.thumbprint} second=${second.thumbprint} third=${third.thumbprint}")
    assertEquals(first.thumbprint, second.thumbprint)
    assertNotEquals(first.thumbprint, third.thumbprint)
    assertEquals(third.thumbprint, fourth.thumbprint)
  }

  @Test
  fun holderReplacesDeletedKey() {
    val holder = DeviceKeyHolder { DeviceKeyFactory.create(context) }
    val lost = assertIs<DeviceKeyState.Ready>(holder.state()).key
    lost.delete()

    val (publicJwk, signature) = holder.withKey { it.publicJwk to it.sign(data) }.getOrThrow()

    report("lost=${lost.thumbprint} replacement=${Es256.thumbprint(publicJwk)}")
    assertNotEquals(lost.publicJwk, publicJwk)
    assertTrue(verifies(publicJwk, data, signature))
  }

  @Test
  fun replacesStoredKeyKeystoreCannotRecover() {
    val platformError = Class.forName("android.security.KeyStoreException")
    val keystore = AndroidDeviceKeyStore(context)
    val original = AndroidKeystoreDeviceKey.getOrCreate(keystore)
    val unrecoverable =
      object : DeviceKeyStore by keystore {
        override fun storedPrivateKey(): PrivateKey? =
          throw UnrecoverableKeyException("Key algorithm unknown").apply {
            initCause(IllegalStateException("not a Keystore error"))
          }
      }

    val replacement = AndroidKeystoreDeviceKey.getOrCreate(unrecoverable)

    report(
      "platform_error_class=${platformError.name} original=${original.thumbprint} " +
        "replacement=${replacement.thumbprint}"
    )
    assertTrue(Exception::class.java.isAssignableFrom(platformError))
    assertNotEquals(original.thumbprint, replacement.thumbprint)
    assertEquals(replacement.thumbprint, AndroidKeystoreDeviceKey.getOrCreate(context).thumbprint)
  }

  @Test
  fun signaturesVerifyWithOpenssl() {
    val key = AndroidKeystoreDeviceKey.getOrCreate(context)
    val directory =
      File(context.getExternalFilesDir(null), "device-key-signatures").apply {
        deleteRecursively()
        mkdirs()
      }

    File(directory, "jwk.json").writeText(key.publicJwk.toString())
    repeat(SIGNATURE_COUNT) { index ->
      val message = "message $index ${UUID.randomUUID()}".toByteArray()
      val signature = key.sign(message)
      assertTrue(verifies(key.publicJwk, message, signature))
      File(directory, "$index.msg").writeBytes(message)
      File(directory, "$index.sig").writeBytes(signature)
    }

    report("wrote $SIGNATURE_COUNT signatures to ${directory.absolutePath}")
  }

  @Test
  fun signsWhileLocked() {
    assumeTrue(
      "set a PIN, lock the screen, and pass expectLocked=true",
      arguments.getString("expectLocked") == "true",
    )
    val keyguardManager = context.getSystemService(KeyguardManager::class.java)
    assertTrue(keyguardManager.isDeviceLocked)

    val key = AndroidKeystoreDeviceKey.getOrCreate(context)
    val signature = key.sign(data)

    report(
      "signed while locked=${keyguardManager.isDeviceLocked} level=${key.securityLevel.wireName}"
    )
    assertTrue(verifies(key.publicJwk, data, signature))
  }

  @Test
  fun timings() {
    AndroidKeystoreDeviceKey.getOrCreate(context).delete()

    val (key, keygenMs) = timed { AndroidKeystoreDeviceKey.getOrCreate(context) }
    val (_, loadMs) = timed { AndroidKeystoreDeviceKey.getOrCreate(context) }
    val signMs = List(SIGNATURE_COUNT) { timed { key.sign(data) }.second }.sorted()
    val signP50Ms = signMs[signMs.size / 2]
    val signBudgetMs =
      if (key.securityLevel == DeviceKeySecurityLevel.STRONGBOX) {
        STRONGBOX_SIGN_P50_BUDGET_MS
      } else {
        SIGN_P50_BUDGET_MS
      }

    report(
      "level=${key.securityLevel.wireName} api=${Build.VERSION.SDK_INT} keygen_ms=$keygenMs " +
        "load_ms=$loadMs sign_p50_ms=$signP50Ms " +
        "sign_p90_ms=${signMs[signMs.size * 9 / 10]} sign_max_ms=${signMs.last()}"
    )
    assertTrue(keygenMs < KEYGEN_BUDGET_MS, "keygen took $keygenMs ms, over $KEYGEN_BUDGET_MS ms")
    assertTrue(signP50Ms < signBudgetMs, "sign p50 took $signP50Ms ms, over $signBudgetMs ms")
  }

  private fun storedKeyInfo(): KeyInfo {
    val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
    val privateKey = keyStore.getKey(DEVICE_KEY_ALIAS, null) as PrivateKey
    return KeyFactory.getInstance(privateKey.algorithm, ANDROID_KEY_STORE)
      .getKeySpec(privateKey, KeyInfo::class.java)
  }

  private fun <T> timed(block: () -> T): Pair<T, Double> {
    val start = SystemClock.elapsedRealtimeNanos()
    val value = block()
    return value to (SystemClock.elapsedRealtimeNanos() - start) / NANOS_PER_MILLI
  }

  private fun report(line: String) {
    Log.i(TAG, line)
  }

  private fun verifies(
    publicJwk: JsonObject,
    message: ByteArray,
    rawSignature: ByteArray,
  ): Boolean {
    val point = ECPoint(publicJwk.coordinate("x"), publicJwk.coordinate("y"))
    val publicKey = KeyFactory.getInstance("EC").generatePublic(ECPublicKeySpec(point, p256()))
    return Signature.getInstance("SHA256withECDSA").run {
      initVerify(publicKey)
      update(message)
      verify(derFromRaw(rawSignature))
    }
  }

  private fun JsonObject.coordinate(name: String): BigInteger =
    BigInteger(1, Base64.decode(getValue(name).jsonPrimitive.content, Base64.URL_SAFE))

  // API 24 has no EC AlgorithmParameters, so take the curve from a software P-256 key.
  private fun p256(): ECParameterSpec =
    (KeyPairGenerator.getInstance("EC")
        .apply { initialize(ECGenParameterSpec("secp256r1")) }
        .generateKeyPair()
        .public as ECPublicKey)
      .params

  private fun derFromRaw(rawSignature: ByteArray): ByteArray {
    fun derInteger(half: ByteArray): ByteArray {
      val value = BigInteger(1, half).toByteArray()
      return byteArrayOf(0x02, value.size.toByte()) + value
    }
    val body =
      derInteger(rawSignature.copyOfRange(0, 32)) + derInteger(rawSignature.copyOfRange(32, 64))
    return byteArrayOf(0x30, body.size.toByte()) + body
  }

  private companion object {
    const val TAG = "DeviceKeyTest"
    const val SIGNATURE_COUNT = 100
    const val NANOS_PER_MILLI = 1_000_000.0
    const val KEYGEN_BUDGET_MS = 500.0
    const val SIGN_P50_BUDGET_MS = 10.0
    const val STRONGBOX_SIGN_P50_BUDGET_MS = 60.0
    const val DEVICE_KEY_ALIAS = "com.clerk.device_key.v1"
    const val ANDROID_KEY_STORE = "AndroidKeyStore"
  }
}
