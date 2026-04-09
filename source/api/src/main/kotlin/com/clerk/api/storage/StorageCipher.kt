package com.clerk.api.storage

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.clerk.api.Constants.Storage.CLERK_PREFERENCES_FILE_NAME
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

internal interface StorageCipher {
  fun encrypt(plaintext: String): String

  fun decrypt(ciphertext: String): String
}

internal object StorageCipherFactory {
  fun create(keyAlias: String = "$CLERK_PREFERENCES_FILE_NAME.master_key"): StorageCipher {
    return try {
      AndroidKeystoreStorageCipher(keyAlias)
    } catch (error: Exception) {
      if (isRobolectricEnvironment()) {
        JvmAesGcmStorageCipher(keyAlias)
      } else {
        throw error
      }
    }
  }

  private fun isRobolectricEnvironment(): Boolean {
    return Build.FINGERPRINT.contains("robolectric", ignoreCase = true)
  }
}

internal class AndroidKeystoreStorageCipher(
  private val keyAlias: String = "$CLERK_PREFERENCES_FILE_NAME.master_key"
) : StorageCipher {
  @Volatile private var cachedSecretKey: SecretKey? = null
  private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

  override fun encrypt(plaintext: String): String {
    val cipher = Cipher.getInstance(AES_TRANSFORMATION)
    cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
    val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
    return "${encode(cipher.iv)}$IV_SEPARATOR${encode(encrypted)}"
  }

  override fun decrypt(ciphertext: String): String {
    val components = ciphertext.split(IV_SEPARATOR, limit = 2)
    require(components.size == 2) { "Encrypted value is malformed" }

    val iv = decode(components[0])
    val encrypted = decode(components[1])
    val cipher = Cipher.getInstance(AES_TRANSFORMATION)
    cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
    return cipher.doFinal(encrypted).toString(Charsets.UTF_8)
  }

  private fun getOrCreateSecretKey(): SecretKey {
    cachedSecretKey?.let {
      return it
    }

    return synchronized(this) {
      cachedSecretKey?.let {
        return@synchronized it
      }

      val existingKey = keyStore.getKey(keyAlias, null) as? SecretKey
      if (existingKey != null) {
        cachedSecretKey = existingKey
        return@synchronized existingKey
      }

      val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
      val keySpec =
        KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
          )
          .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
          .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
          .setRandomizedEncryptionRequired(true)
          .setUserAuthenticationRequired(false)
          .build()

      keyGenerator.init(keySpec)
      return@synchronized keyGenerator.generateKey().also { generatedKey ->
        cachedSecretKey = generatedKey
      }
    }
  }

  private fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

  private fun decode(encoded: String): ByteArray = Base64.decode(encoded, Base64.NO_WRAP)

  private companion object {
    const val ANDROID_KEYSTORE = "AndroidKeyStore"
    const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
    const val GCM_TAG_BITS = 128
    const val IV_SEPARATOR = ':'
  }
}

internal class JvmAesGcmStorageCipher(private val keyAlias: String) : StorageCipher {
  private val secretKey: SecretKey by lazy {
    val keyBytes = MessageDigest.getInstance("SHA-256").digest(keyAlias.toByteArray(Charsets.UTF_8))
    SecretKeySpec(keyBytes.copyOf(16), KeyProperties.KEY_ALGORITHM_AES)
  }

  override fun encrypt(plaintext: String): String {
    val cipher = Cipher.getInstance(AES_TRANSFORMATION)
    cipher.init(Cipher.ENCRYPT_MODE, secretKey)
    val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
    return "${encode(cipher.iv)}$IV_SEPARATOR${encode(encrypted)}"
  }

  override fun decrypt(ciphertext: String): String {
    val components = ciphertext.split(IV_SEPARATOR, limit = 2)
    require(components.size == 2) { "Encrypted value is malformed" }

    val iv = decode(components[0])
    val encrypted = decode(components[1])
    val cipher = Cipher.getInstance(AES_TRANSFORMATION)
    cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
    return cipher.doFinal(encrypted).toString(Charsets.UTF_8)
  }

  private fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

  private fun decode(encoded: String): ByteArray = Base64.decode(encoded, Base64.NO_WRAP)

  private companion object {
    const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
    const val GCM_TAG_BITS = 128
    const val IV_SEPARATOR = ':'
  }
}
