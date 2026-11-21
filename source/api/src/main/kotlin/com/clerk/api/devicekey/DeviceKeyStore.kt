package com.clerk.api.devicekey

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.ECGenParameterSpec

internal interface DeviceKeyStore {
  fun hasStrongBox(): Boolean

  fun storedPrivateKey(): PrivateKey?

  fun storedPublicKey(): PublicKey?

  fun generate(inStrongBox: Boolean): KeyPair

  fun securityLevel(privateKey: PrivateKey): DeviceKeySecurityLevel

  fun sign(privateKey: PrivateKey, data: ByteArray): ByteArray

  fun delete()
}

internal class AndroidDeviceKeyStore(private val context: Context) : DeviceKeyStore {
  override fun hasStrongBox(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
      context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)

  override fun storedPrivateKey(): PrivateKey? = keyStore().getKey(ALIAS, null) as? PrivateKey

  override fun storedPublicKey(): PublicKey? = keyStore().getCertificate(ALIAS)?.publicKey

  override fun generate(inStrongBox: Boolean): KeyPair {
    val spec =
      KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN)
        .setAlgorithmParameterSpec(ECGenParameterSpec(EC_CURVE))
        .setDigests(KeyProperties.DIGEST_SHA256)
        .setUserAuthenticationRequired(false)
    if (inStrongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      spec.setIsStrongBoxBacked(true)
    }
    return KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEY_STORE)
      .apply { initialize(spec.build()) }
      .generateKeyPair()
  }

  override fun securityLevel(privateKey: PrivateKey): DeviceKeySecurityLevel {
    val keyInfo =
      KeyFactory.getInstance(privateKey.algorithm, ANDROID_KEY_STORE)
        .getKeySpec(privateKey, KeyInfo::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      return securityLevelOf(keyInfo.securityLevel)
    }
    @Suppress("DEPRECATION")
    return if (keyInfo.isInsideSecureHardware) {
      DeviceKeySecurityLevel.TEE
    } else {
      DeviceKeySecurityLevel.SOFTWARE
    }
  }

  override fun sign(privateKey: PrivateKey, data: ByteArray): ByteArray =
    derSignature(privateKey, data)

  override fun delete() {
    keyStore().deleteEntry(ALIAS)
  }

  private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }

  private companion object {
    const val ALIAS = "com.clerk.device_key.v1"
    const val ANDROID_KEY_STORE = "AndroidKeyStore"
    const val EC_CURVE = "secp256r1"
  }
}

@RequiresApi(Build.VERSION_CODES.S)
internal fun securityLevelOf(keyInfoSecurityLevel: Int): DeviceKeySecurityLevel =
  when (keyInfoSecurityLevel) {
    KeyProperties.SECURITY_LEVEL_STRONGBOX -> DeviceKeySecurityLevel.STRONGBOX
    KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT,
    KeyProperties.SECURITY_LEVEL_UNKNOWN_SECURE -> DeviceKeySecurityLevel.TEE
    else -> DeviceKeySecurityLevel.SOFTWARE
  }
