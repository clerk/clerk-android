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
      cause.provesKeyLost() -> DeviceKeyLostException(cause)
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
          .getOrElse { if (it.provesKeyLost()) null else throw it } ?: return null
      // KeyStore.getCertificate returns null for every Keystore error, not only a missing entry.
      val publicKey = store.storedPublicKey()
      return when {
        publicKey != null -> KeyPair(publicKey, privateKey)
        keystoreTellsAFailedReadFromLoss() ->
          throw KeyStoreException("The device key's certificate could not be read.")
        else -> null
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

// On API 31 and later, AndroidKeyStoreSpi.engineGetKey returns null only for a missing key. It
// wraps every other Keystore error, transient ones included, in an UnrecoverableKeyException caused
// by an android.security.KeyStoreException. That class is public from API 33, but it exists as a
// hidden class from API 23 and the older Keystore throws it too.
@SuppressLint("NewApi")
private fun Throwable.provesKeyLost(): Boolean =
  when (this) {
    is KeyPermanentlyInvalidatedException -> true
    is UnrecoverableKeyException ->
      cause !is PlatformKeyStoreException || !keystoreTellsAFailedReadFromLoss()
    else ->
      generateSequence(this) { it.cause }
        .filterIsInstance<PlatformKeyStoreException>()
        .any { error -> KeyMintErrorThatEndsTheKey.entries.any { it.isNamedBy(error) } }
  }

// Below API 31 the Keystore reports a failed binder call the same way as a key blob it cannot read,
// and a process death between keygen and storing the certificate leaves a key with no certificate.
private fun keystoreTellsAFailedReadFromLoss(): Boolean =
  Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

// An app cannot read the KeyMint error code. KeyStoreException.getErrorCode is a blocked hidden
// API, and getNumericErrorCode gives KM_ERROR_INVALID_KEY_BLOB the same ERROR_KEYMINT_FAILURE as
// KM_ERROR_TOO_MANY_OPERATIONS. On every API level the platform starts the exception's message
// with KeymasterDefs.getErrorMessage(code).
private enum class KeyMintErrorThatEndsTheKey(private val platformMessage: String) {
  KM_ERROR_INVALID_KEY_BLOB("Invalid key blob");

  fun isNamedBy(error: Throwable): Boolean = error.message?.startsWith(platformMessage) == true
}
