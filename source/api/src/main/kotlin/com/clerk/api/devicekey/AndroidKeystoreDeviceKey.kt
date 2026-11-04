package com.clerk.api.devicekey

import android.content.Context
import java.security.GeneralSecurityException
import java.security.InvalidKeyException
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
      cause is InvalidKeyException || !isStored() -> DeviceKeyLostException(cause)
      else -> cause
    }

  private fun isStored(): Boolean =
    store.storedPublicKey()?.encoded?.contentEquals(publicKey.encoded) == true

  companion object {
    fun getOrCreate(context: Context): AndroidKeystoreDeviceKey =
      getOrCreate(AndroidDeviceKeyStore(context))

    fun getOrCreate(store: DeviceKeyStore): AndroidKeystoreDeviceKey {
      val keyPair = usableStoredKeyPair(store) ?: generateKeyPair(store)
      val publicKey =
        keyPair.public as? ECPublicKey
          ?: throw KeyStoreException("The device key is not an EC key.")
      return AndroidKeystoreDeviceKey(store, keyPair.private, publicKey)
    }

    private fun usableStoredKeyPair(store: DeviceKeyStore): KeyPair? {
      val privateKey =
        try {
          store.storedPrivateKey()
        } catch (_: UnrecoverableKeyException) {
          null
        }
      val publicKey = store.storedPublicKey()
      return if (privateKey != null && publicKey != null) {
        KeyPair(publicKey, privateKey)
      } else {
        null
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
