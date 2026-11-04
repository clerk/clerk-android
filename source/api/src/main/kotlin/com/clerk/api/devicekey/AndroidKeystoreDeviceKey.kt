package com.clerk.api.devicekey

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import java.security.GeneralSecurityException
import java.security.InvalidKeyException
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.PrivateKey
import java.security.ProviderException
import java.security.UnrecoverableKeyException
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import kotlinx.serialization.json.JsonObject

internal class AndroidKeystoreDeviceKey
private constructor(private val privateKey: PrivateKey, private val publicKey: ECPublicKey) :
  DeviceKey {
  override val publicJwk: JsonObject = Es256.publicJwk(publicKey)
  override val thumbprint: String = Es256.thumbprint(publicJwk)
  override val securityLevel: DeviceKeySecurityLevel = securityLevelOf(privateKey)

  override fun sign(data: ByteArray): ByteArray =
    try {
      signAndVerify(privateKey, publicKey, data)
    } catch (e: GeneralSecurityException) {
      throw signingFailure(e)
    } catch (e: ProviderException) {
      throw signingFailure(e)
    }

  override fun delete() {
    val keyStore = keyStore()
    if (isHeldBy(keyStore)) {
      keyStore.deleteEntry(ALIAS)
    }
  }

  private fun signingFailure(cause: Exception): Exception =
    when {
      cause is DeviceKeyLostException -> cause
      cause is InvalidKeyException || !isHeldBy(keyStore()) -> DeviceKeyLostException(cause)
      else -> cause
    }

  private fun isHeldBy(keyStore: KeyStore): Boolean =
    keyStore.getCertificate(ALIAS)?.publicKey?.encoded?.contentEquals(publicKey.encoded) == true

  companion object {
    private const val ALIAS = "com.clerk.device_key.v1"
    private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    private const val EC_CURVE = "secp256r1"

    fun getOrCreate(context: Context): AndroidKeystoreDeviceKey {
      val keyPair = usableStoredKeyPair(keyStore()) ?: generateKeyPair(context)
      val publicKey =
        keyPair.public as? ECPublicKey ?: throw KeyStoreException("$ALIAS does not hold an EC key.")
      return AndroidKeystoreDeviceKey(keyPair.private, publicKey)
    }

    private fun usableStoredKeyPair(keyStore: KeyStore): KeyPair? {
      val privateKey =
        try {
          keyStore.getKey(ALIAS, null) as? PrivateKey
        } catch (_: UnrecoverableKeyException) {
          null
        }
      val certificate = keyStore.getCertificate(ALIAS)
      return if (privateKey != null && certificate != null) {
        KeyPair(certificate.publicKey, privateKey)
      } else {
        null
      }
    }

    private fun generateKeyPair(context: Context): KeyPair {
      if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
          context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)
      ) {
        catchingKeystoreFailures { generateKeyPair(keySpec().setIsStrongBoxBacked(true).build()) }
          .onSuccess {
            return it
          }
      }
      return generateKeyPair(keySpec().build())
    }

    private fun keySpec(): KeyGenParameterSpec.Builder =
      KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN)
        .setAlgorithmParameterSpec(ECGenParameterSpec(EC_CURVE))
        .setDigests(KeyProperties.DIGEST_SHA256)
        .setUserAuthenticationRequired(false)

    private fun generateKeyPair(spec: KeyGenParameterSpec): KeyPair =
      KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEY_STORE)
        .apply { initialize(spec) }
        .generateKeyPair()

    private fun securityLevelOf(privateKey: PrivateKey): DeviceKeySecurityLevel {
      val keyInfo =
        KeyFactory.getInstance(privateKey.algorithm, ANDROID_KEY_STORE)
          .getKeySpec(privateKey, KeyInfo::class.java)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        return when (keyInfo.securityLevel) {
          KeyProperties.SECURITY_LEVEL_STRONGBOX -> DeviceKeySecurityLevel.STRONGBOX
          KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT,
          KeyProperties.SECURITY_LEVEL_UNKNOWN_SECURE -> DeviceKeySecurityLevel.TEE
          else -> DeviceKeySecurityLevel.SOFTWARE
        }
      }
      @Suppress("DEPRECATION")
      return if (keyInfo.isInsideSecureHardware) {
        DeviceKeySecurityLevel.TEE
      } else {
        DeviceKeySecurityLevel.SOFTWARE
      }
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
  }
}
