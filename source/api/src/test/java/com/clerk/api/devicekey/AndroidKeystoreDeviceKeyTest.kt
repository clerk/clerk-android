package com.clerk.api.devicekey

import android.os.Build
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import java.security.InvalidKeyException
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStoreException
import java.security.PrivateKey
import java.security.ProviderException
import java.security.PublicKey
import java.security.UnrecoverableKeyException
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class AndroidKeystoreDeviceKeyTest {
  private val data = "GET https://clerk.example.com/v1/client".toByteArray()
  private val store = FakeDeviceKeyStore()
  private val holder = DeviceKeyHolder { AndroidKeystoreDeviceKey.getOrCreate(store) }

  @Test
  fun `a key the platform permanently invalidated is replaced at signing`() {
    val original = readyKey()
    store.failNextSigning(KeyPermanentlyInvalidatedException())

    assertReplaced(original, signedBy())
  }

  @Test
  fun `a key another process deleted is replaced at signing`() {
    val original = readyKey()
    store.failNextSigning(keystoreFailure(KEY_NOT_FOUND))
    store.stored = null

    assertReplaced(original, signedBy())
  }

  @Test
  fun `a key another process replaced is replaced at signing`() {
    val original = readyKey()
    store.failNextSigning(keystoreFailure(KEY_NOT_FOUND))
    store.stored = newKeyPair()

    assertReplaced(original, signedBy())
  }

  @Test
  fun `a Keystore error at signing keeps a key the alias still holds`() {
    val original = readyKey()
    val failure = keystoreFailure(SYSTEM_ERROR)
    store.failNextSigning(failure)

    assertSame(failure, holder.withKey { it.sign(data) }.exceptionOrNull())
    assertKept(original)
  }

  @Test
  fun `a key whose blob KeyMint rejects is replaced at signing`() {
    val original = readyKey()
    store.failEverySigning(storedPrivateKey(), keystoreFailure(KM_ERROR_INVALID_KEY_BLOB))

    assertReplaced(original, signedBy())
  }

  @Test
  fun `KeyMint running out of operations at signing keeps the key`() {
    val original = readyKey()
    val failure = keystoreFailure(KM_ERROR_TOO_MANY_OPERATIONS)
    store.failNextSigning(failure)

    assertSame(failure, holder.withKey { it.sign(data) }.exceptionOrNull())
    assertKept(original)
  }

  @Test
  fun `a failed alias read after a signing failure keeps the key and reports the signing failure`() {
    val original = readyKey()
    val signingFailure = ProviderException("Keystore operation failed")
    val readFailure = KeyStoreException(platformKeystoreException(SYSTEM_ERROR))
    store.failNextSigning(signingFailure)
    store.getKey = { throw readFailure }
    store.getCertificatePublicKey = { throw readFailure }

    val failure = holder.withKey { it.sign(data) }.exceptionOrNull()

    assertSame(signingFailure, failure)
    assertTrue(readFailure in signingFailure.suppressed)
    assertKept(original)
  }

  @Test
  fun `from API 31 a stored key that Keystore cannot read for a stated reason is kept`() {
    val original = AndroidKeystoreDeviceKey.getOrCreate(store)
    val failure = unrecoverable(cause = platformKeystoreException(SYSTEM_ERROR))
    store.getKey = { throw failure }

    val state = holder.state()

    assertSame(failure, assertIs<DeviceKeyState.Unavailable>(state).cause)
    assertEquals(original.thumbprint, storedThumbprint())
  }

  @Test
  fun `a stored key that Keystore cannot recover is replaced`() {
    val original = AndroidKeystoreDeviceKey.getOrCreate(store)
    store.getKey = { throw unrecoverable(cause = null) }

    val key = assertIs<DeviceKeyState.Ready>(holder.state()).key

    assertNotEquals(original.thumbprint, key.thumbprint)
    assertEquals(key.thumbprint, storedThumbprint())
  }

  @Test
  fun `a stored key the platform permanently invalidated is replaced`() {
    val original = AndroidKeystoreDeviceKey.getOrCreate(store)
    store.getKey = { throw KeyPermanentlyInvalidatedException() }

    val key = assertIs<DeviceKeyState.Ready>(holder.state()).key

    assertNotEquals(original.thumbprint, key.thumbprint)
    assertEquals(key.thumbprint, storedThumbprint())
  }

  @Test
  fun `from API 31 a stored key whose certificate reads empty is kept`() {
    val original = AndroidKeystoreDeviceKey.getOrCreate(store)
    store.getCertificatePublicKey = { null }

    assertIs<DeviceKeyState.Unavailable>(holder.state())
    assertEquals(original.thumbprint, storedThumbprint())
  }

  @Test
  @Config(sdk = [30])
  fun `below API 31 a stored key that Keystore cannot read is replaced whatever the error`() {
    val original = AndroidKeystoreDeviceKey.getOrCreate(store)
    val unreadable = store.stored
    val failure = unrecoverable(cause = platformKeystoreException(SYSTEM_ERROR))
    store.getKey = { if (store.stored === unreadable) throw failure else store.stored?.private }

    val key = assertIs<DeviceKeyState.Ready>(holder.state()).key

    assertNotEquals(original.thumbprint, key.thumbprint)
    assertEquals(key.thumbprint, storedThumbprint())
    assertEquals(key.thumbprint, nextStartThumbprint())
  }

  @Test
  @Config(sdk = [30])
  fun `below API 31 a stored key left without its certificate is replaced`() {
    val original = AndroidKeystoreDeviceKey.getOrCreate(store)
    val withoutCertificate = store.stored
    store.getCertificatePublicKey = { store.stored.takeIf { it !== withoutCertificate }?.public }

    val key = assertIs<DeviceKeyState.Ready>(holder.state()).key

    assertNotEquals(original.thumbprint, key.thumbprint)
    assertEquals(key.thumbprint, storedThumbprint())
    assertEquals(key.thumbprint, nextStartThumbprint())
  }

  @Test
  fun `a device with StrongBox gets a StrongBox key`() {
    val store = FakeDeviceKeyStore(strongBox = true)

    val key = assertIs<DeviceKeyState.Ready>(DeviceKeyHolder { getOrCreate(store) }.state()).key

    assertEquals(DeviceKeySecurityLevel.STRONGBOX, key.securityLevel)
  }

  @Test
  fun `a StrongBox failure falls back to a key outside StrongBox`() {
    val store = FakeDeviceKeyStore(strongBox = true)
    store.strongBoxFailure = StrongBoxUnavailableException("No StrongBox on this device")

    val key = assertIs<DeviceKeyState.Ready>(DeviceKeyHolder { getOrCreate(store) }.state()).key

    assertNotEquals(DeviceKeySecurityLevel.STRONGBOX, key.securityLevel)
    assertEquals(key.publicJwk, Es256.publicJwk(assertIs<ECPublicKey>(store.stored?.public)))
    assertEquals(64, key.sign(data).size)
  }

  @Test
  fun `a key that fails in and out of StrongBox is unavailable`() {
    val store = FakeDeviceKeyStore(strongBox = true)
    val failure = ProviderException("Keystore is unavailable")
    store.strongBoxFailure = StrongBoxUnavailableException("No StrongBox on this device")
    store.generationFailure = failure

    val state = DeviceKeyHolder { getOrCreate(store) }.state()

    assertSame(failure, assertIs<DeviceKeyState.Unavailable>(state).cause)
    assertEquals(null, store.stored)
  }

  @Test
  fun `KeyInfo security levels map to the level the key reports`() {
    val expected =
      mapOf(
        KeyProperties.SECURITY_LEVEL_STRONGBOX to DeviceKeySecurityLevel.STRONGBOX,
        KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT to DeviceKeySecurityLevel.TEE,
        KeyProperties.SECURITY_LEVEL_UNKNOWN_SECURE to DeviceKeySecurityLevel.TEE,
        KeyProperties.SECURITY_LEVEL_SOFTWARE to DeviceKeySecurityLevel.SOFTWARE,
        KeyProperties.SECURITY_LEVEL_UNKNOWN to DeviceKeySecurityLevel.SOFTWARE,
      )

    assertEquals(
      expected,
      expected.mapValues { (keyInfoLevel, _) -> securityLevelOf(keyInfoLevel) },
    )
  }

  private fun getOrCreate(store: DeviceKeyStore): DeviceKey =
    AndroidKeystoreDeviceKey.getOrCreate(store)

  private fun readyKey(): DeviceKey = assertIs<DeviceKeyState.Ready>(holder.state()).key

  private fun nextStartThumbprint(): String =
    assertIs<DeviceKeyState.Ready>(DeviceKeyHolder { getOrCreate(store) }.state()).key.thumbprint

  private fun signedBy(): String =
    holder
      .withKey {
        it.sign(data)
        it.thumbprint
      }
      .getOrThrow()

  private fun assertReplaced(original: DeviceKey, signer: String) {
    assertNotEquals(original.thumbprint, signer)
    assertEquals(signer, storedThumbprint())
  }

  private fun assertKept(original: DeviceKey) {
    assertSame(original, readyKey())
    assertEquals(original.thumbprint, storedThumbprint())
    assertEquals(original.thumbprint, signedBy())
  }

  private fun storedThumbprint(): String? =
    (store.stored?.public as? ECPublicKey)?.let { Es256.thumbprint(Es256.publicJwk(it)) }

  private fun keystoreFailure(responseCode: Int): InvalidKeyException =
    InvalidKeyException("Keystore operation failed", platformKeystoreException(responseCode))

  private fun unrecoverable(cause: Throwable?): UnrecoverableKeyException =
    UnrecoverableKeyException("Failed to obtain information about key").apply {
      if (cause != null) {
        initCause(cause)
      }
    }

  private fun platformKeystoreException(errorCode: Int): Exception =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      Class.forName("android.security.KeyStore2")
        .getDeclaredMethod("getKeyStoreException", Int::class.javaPrimitiveType, String::class.java)
        .apply { isAccessible = true }
        .invoke(null, errorCode, "keystore2 error $errorCode") as Exception
    } else {
      Class.forName("android.security.KeyStore")
        .getMethod("getKeyStoreException", Int::class.javaPrimitiveType)
        .invoke(null, errorCode) as Exception
    }

  private fun storedPrivateKey(): PrivateKey = checkNotNull(store.stored).private

  private companion object {
    const val SYSTEM_ERROR = 4
    const val KEY_NOT_FOUND = 7
    const val KM_ERROR_TOO_MANY_OPERATIONS = -31
    const val KM_ERROR_INVALID_KEY_BLOB = -33
  }
}

internal class FakeDeviceKeyStore(private val strongBox: Boolean = false) : DeviceKeyStore {
  var stored: KeyPair? = null
  var getKey: () -> PrivateKey? = { stored?.private }
  var getCertificatePublicKey: () -> PublicKey? = { stored?.public }
  var strongBoxFailure: Exception? = null
  var generationFailure: Exception? = null
  private var nextSigningFailure: Exception? = null
  private val signingFailures = mutableMapOf<PrivateKey, Exception>()
  private val strongBoxKeys = mutableSetOf<PrivateKey>()

  fun failNextSigning(failure: Exception) {
    nextSigningFailure = failure
  }

  fun failEverySigning(privateKey: PrivateKey, failure: Exception) {
    signingFailures[privateKey] = failure
  }

  override fun hasStrongBox(): Boolean = strongBox

  override fun storedPrivateKey(): PrivateKey? = getKey()

  override fun storedPublicKey(): PublicKey? = getCertificatePublicKey()

  override fun generate(inStrongBox: Boolean): KeyPair {
    (if (inStrongBox) strongBoxFailure else generationFailure)?.let { throw it }
    return newKeyPair().also {
      if (inStrongBox) {
        strongBoxKeys += it.private
      }
      stored = it
    }
  }

  override fun securityLevel(privateKey: PrivateKey): DeviceKeySecurityLevel =
    if (privateKey in strongBoxKeys) DeviceKeySecurityLevel.STRONGBOX
    else DeviceKeySecurityLevel.TEE

  override fun sign(privateKey: PrivateKey, data: ByteArray): ByteArray {
    nextSigningFailure?.let {
      nextSigningFailure = null
      throw it
    }
    signingFailures[privateKey]?.let { throw it }
    return derSignature(privateKey, data)
  }

  override fun delete() {
    stored = null
  }
}

private fun newKeyPair(): KeyPair =
  KeyPairGenerator.getInstance("EC")
    .apply { initialize(ECGenParameterSpec("secp256r1")) }
    .generateKeyPair()
