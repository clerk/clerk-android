package com.clerk.workbench

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.interfaces.RSAPublicKey

internal object KeystoreFault {
  private const val SDK_DEVICE_KEY_ALIAS = "com.clerk.device_key.v1"
  private const val ANDROID_KEY_STORE = "AndroidKeyStore"

  fun apply(enabled: Boolean) {
    val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
    val planted = keyStore.getCertificate(SDK_DEVICE_KEY_ALIAS)?.publicKey is RSAPublicKey
    when {
      enabled && !planted -> plantRsaKeyAtSdkAlias()
      !enabled && planted -> keyStore.deleteEntry(SDK_DEVICE_KEY_ALIAS)
    }
  }

  private fun plantRsaKeyAtSdkAlias() {
    KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEY_STORE)
      .apply {
        initialize(
          KeyGenParameterSpec.Builder(SDK_DEVICE_KEY_ALIAS, KeyProperties.PURPOSE_SIGN)
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
            .build()
        )
      }
      .generateKeyPair()
  }
}
