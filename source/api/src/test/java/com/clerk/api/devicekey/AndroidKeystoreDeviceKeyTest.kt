package com.clerk.api.devicekey

import android.security.keystore.KeyPermanentlyInvalidatedException
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
  fun `a transient Keystore error at signing keeps the key even when the alias reads empty`() {
    val original = readyKey()
    val failure = keystoreFailure(SECURE_HW_BUSY)
    store.failNextSigning(failure)
    store.getKey = { null }
    store.getCertificatePublicKey = { null }

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
  fun `a stored key that Keystore cannot read for a stated reason is kept`() {
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
  fun `a stored key whose certificate reads empty is kept`() {
    val original = AndroidKeystoreDeviceKey.getOrCreate(store)
    store.getCertificatePublicKey = { null }

    assertIs<DeviceKeyState.Unavailable>(holder.state())
    assertEquals(original.thumbprint, storedThumbprint())
  }

  private fun readyKey(): DeviceKey = assertIs<DeviceKeyState.Ready>(holder.state()).key

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

  private fun platformKeystoreException(responseCode: Int): Exception =
    android.security.KeyStoreException::class
      .java
      .getConstructor(Int::class.javaPrimitiveType, String::class.java)
      .newInstance(responseCode, "keystore2 response $responseCode")

  private companion object {
    const val SYSTEM_ERROR = 4
    const val KEY_NOT_FOUND = 7
    const val SECURE_HW_BUSY = -48
  }
}

internal class FakeDeviceKeyStore(private val strongBox: Boolean = false) : DeviceKeyStore {
  var stored: KeyPair? = null
  var getKey: () -> PrivateKey? = { stored?.private }
  var getCertificatePublicKey: () -> PublicKey? = { stored?.public }
  var strongBoxFailure: Exception? = null
  var generationFailure: Exception? = null
  private var nextSigningFailure: Exception? = null
  private val strongBoxKeys = mutableSetOf<PrivateKey>()

  fun failNextSigning(failure: Exception) {
    nextSigningFailure = failure
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
