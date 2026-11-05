package com.clerk.api.devicekey

import android.annotation.SuppressLint
import android.content.Context
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
      val publicKey =
        store.storedPublicKey()
          ?: throw KeyStoreException("The device key's certificate could not be read.")
      return KeyPair(publicKey, privateKey)
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

// AndroidKeyStoreSpi.engineGetKey returns null only for a missing key. It wraps every other
// Keystore error, transient ones included, in an UnrecoverableKeyException caused by an
// android.security.KeyStoreException. That class is public from API 33, but it exists as a hidden
// class from API 23 and the older Keystore throws it too.
@SuppressLint("NewApi")
private fun Throwable.provesKeyLost(): Boolean =
  when (this) {
    is KeyPermanentlyInvalidatedException -> true
    is UnrecoverableKeyException -> cause !is PlatformKeyStoreException
    else -> false
  }
