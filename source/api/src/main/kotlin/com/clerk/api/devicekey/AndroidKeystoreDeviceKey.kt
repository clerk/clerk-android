package com.clerk.api.devicekey

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.security.KeyStoreException as PlatformKeyStoreException
import android.security.keystore.KeyPermanentlyInvalidatedException
import java.security.GeneralSecurityException
import java.security.KeyPair
import java.security.KeyStoreException
import java.security.PrivateKey
import java.security.ProviderException
import java.security.UnrecoverableKeyException
import java.security.interfaces.ECPublicKey
import kotlinx.serialization.json.JsonObject

internal class AndroidKeystoreDeviceKey
private constructor(
  private val store: DeviceKeyStore,
  private val privateKey: PrivateKey,
  private val publicKey: ECPublicKey,
) : DeviceKey {
  override val publicJwk: JsonObject = Es256.publicJwk(publicKey)
  override val thumbprint: String = Es256.thumbprint(publicJwk)
  override val securityLevel: DeviceKeySecurityLevel = store.securityLevel(privateKey)

  override fun sign(data: ByteArray): ByteArray =
    try {
      rawSignatureVerifiedBy(publicKey, data, store.sign(privateKey, data))
    } catch (e: GeneralSecurityException) {
      throw signingFailure(e)
    } catch (e: ProviderException) {
      throw signingFailure(e)
    }

  override fun delete() {
    if (isStored()) {
      store.delete()
    }
  }

  private fun signingFailure(cause: Exception): Exception =
    when {
      cause is DeviceKeyLostException -> cause
      classify(cause) == KeystoreFailure.LOSS -> DeviceKeyLostException(cause)
      else ->
        catchingKeystoreFailures { isStored() }
          .fold(
            onSuccess = { stored -> if (stored) cause else DeviceKeyLostException(cause) },
            onFailure = { cause.apply { addSuppressed(it) } },
          )
    }

  private fun isStored(): Boolean =
    storedKeyPair(store)?.public?.encoded?.contentEquals(publicKey.encoded) == true

  companion object {
    fun getOrCreate(context: Context): AndroidKeystoreDeviceKey =
      getOrCreate(AndroidDeviceKeyStore(context))

    fun getOrCreate(store: DeviceKeyStore): AndroidKeystoreDeviceKey {
      val keyPair = storedKeyPair(store) ?: generateKeyPair(store)
      val publicKey =
        keyPair.public as? ECPublicKey
          ?: throw KeyStoreException("The device key is not an EC key.")
      return AndroidKeystoreDeviceKey(store, keyPair.private, publicKey)
    }

    private fun storedKeyPair(store: DeviceKeyStore): KeyPair? {
      val privateKey =
        catchingKeystoreFailures { store.storedPrivateKey() }
          .getOrElse { if (classify(it) == KeystoreFailure.LOSS) null else throw it } ?: return null
      // KeyStore.getCertificate returns null for every Keystore error, not only a missing entry
      // (AndroidKeyStoreSpi.java:171-175 in API 37). On API 30 and lower the Keystore service
      // returns no item for every error (keystore/key_store_service.cpp:77-81 in android-7.0.0_r1,
      // 172-177 in android-9.0.0_r1).
      val publicKey = store.storedPublicKey()
      return when {
        publicKey != null -> KeyPair(publicKey, privateKey)
        keygenCanLeaveAKeyWithoutCertificate() -> null
        else -> throw KeyStoreException("The device key's certificate could not be read.")
      }
    }

    private fun generateKeyPair(store: DeviceKeyStore): KeyPair {
      if (store.hasStrongBox()) {
        catchingKeystoreFailures { store.generate(inStrongBox = true) }
          .onSuccess {
            return it
          }
      }
      return store.generate(inStrongBox = false)
    }
  }
}

private enum class KeystoreFailure {
  LOSS,
  KEEP,
}

// android.security.KeyStoreException is public from API 33, but it exists as a hidden class from
// API 23 and the Keystore on API 30 and lower throws it too.
@SuppressLint("NewApi")
private fun classify(failure: Throwable): KeystoreFailure =
  when {
    failure is KeyPermanentlyInvalidatedException -> KeystoreFailure.LOSS
    failure is UnrecoverableKeyException && failure.cause !is PlatformKeyStoreException ->
      KeystoreFailure.LOSS
    generateSequence(failure) { it.cause }
      .filterIsInstance<PlatformKeyStoreException>()
      .any { it.saysTheKeyIsLost() } -> KeystoreFailure.LOSS
    else -> KeystoreFailure.KEEP
  }

// An app cannot read the internal Keystore code: KeyStoreException.getErrorCode is a blocked hidden
// API, and getNumericErrorCode gives KM_ERROR_INVALID_KEY_BLOB the same public code as transient
// KeyMint errors. The platform starts the message with a fixed text per code instead. On API 30
// and lower KeyStore.getKeyStoreException writes "Key not found" for KEY_NOT_FOUND, "Key blob
// corrupted" for VALUE_CORRUPTED, and KeymasterDefs.getErrorMessage for KeyMint codes. From API 31
// KeyStore2.java:400-404 and 429 (API 37) do the same, and KeymasterDefs.java:412 names
// KM_ERROR_INVALID_KEY_BLOB "Invalid key blob". On API 24 to 26 builds without the Keystore fix
// in android-7.1.2_r39 and android-8.1.0_r1, getKeyForName reports every failed read of a key file
// that exists as KEY_NOT_FOUND (keystore/keystore.cpp:506-512 in android-7.0.0_r1, 541-549 in
// android-8.0.0_r36), a transient SYSTEM_ERROR from open() or close() included
// (keystore/blob.cpp:155-165 in android-7.0.0_r1), so there a transient read error also replaces
// the key. The fixed builds return the read's own code (keystore.cpp:544-553 in android-7.1.2_r39,
// 572-581 in android-8.1.0_r1). From API 31 the platform itself reads KEY_NOT_FOUND as no key
// (AndroidKeyStoreSpi.java:130-134, KeyStoreCryptoOperationUtils.java:117).
private val LOST_KEY_MESSAGES = listOf("Invalid key blob", "Key blob corrupted", "Key not found")

@SuppressLint("NewApi")
private fun PlatformKeyStoreException.saysTheKeyIsLost(): Boolean =
  LOST_KEY_MESSAGES.any { message?.startsWith(it) == true } ||
    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      numericErrorCode == PlatformKeyStoreException.ERROR_KEY_CORRUPTED)

// On API 30 and lower AndroidKeyStoreKeyPairGeneratorSpi.generateKeyPair stores the certificate in
// a Keystore write of its own after the key, so a process death between the two leaves a key with
// no certificate. From API 31 one generateKey call stores both
// (AndroidKeyStoreKeyPairGeneratorSpi.java:815-820 in API 37).
private fun keygenCanLeaveAKeyWithoutCertificate(): Boolean =
  Build.VERSION.SDK_INT < Build.VERSION_CODES.S
