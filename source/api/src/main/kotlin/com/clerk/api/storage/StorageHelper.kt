package com.clerk.api.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import androidx.core.content.edit
import com.clerk.api.Constants.Storage.CLERK_PREFERENCES_FILE_NAME
import com.clerk.api.log.ClerkLog

/**
 * Helper class to manage secure storage of data. SharedPreferences are used as the persistence
 * backend, while values are encrypted before they are written to disk.
 */
internal object StorageHelper {
  private const val ENCRYPTED_VALUE_PREFIX = "clerk:v1:"

  @Volatile private var secureStorage: SharedPreferences? = null
  @Volatile private var storageCipher: StorageCipher? = null

  @VisibleForTesting internal var storageCipherFactoryOverride: (() -> StorageCipher)? = null

  /**
   * Synchronously initializes the secure storage. We do this synchronously because we need to
   * ensure that the storage is initialized before we generate a device ID.
   */
  @Synchronized
  fun initialize(context: Context) {
    if (secureStorage == null) {
      secureStorage =
        context.applicationContext.getSharedPreferences(
          CLERK_PREFERENCES_FILE_NAME,
          Context.MODE_PRIVATE,
        )
    }
    if (storageCipher == null) {
      storageCipher =
        runCatching { storageCipherFactoryOverride?.invoke() ?: StorageCipherFactory.create() }
          .onFailure { error ->
            ClerkLog.w("Failed to initialize encrypted storage: ${error.message}")
          }
          .getOrNull()
    }
  }

  /** Save value of string type to [secureStorage] */
  internal fun saveValue(key: StorageKey, value: String) {
    val prefs = secureStorage
    val cipher = storageCipher

    when {
      prefs == null -> {
        ClerkLog.w(
          "StorageHelper.saveValue called before initialization, ignoring save for key: ${key.name}"
        )
      }
      value.isEmpty() -> Unit
      cipher == null -> {
        ClerkLog.w("Encrypted storage is unavailable, ignoring save for key: ${key.name}")
      }
      else -> {
        runCatching { ENCRYPTED_VALUE_PREFIX + cipher.encrypt(value) }
          .onSuccess { encryptedValue ->
            prefs.edit(commit = true) { putString(key.name, encryptedValue) }
          }
          .onFailure { error ->
            ClerkLog.w("Failed to encrypt value for key ${key.name}: ${error.message}")
          }
      }
    }
  }

  /** Load value of string type from [secureStorage] */
  internal fun loadValue(key: StorageKey): String? {
    val prefs = secureStorage
    val storedValue = prefs?.getString(key.name, null)
    val cipher = storageCipher

    return when {
      prefs == null -> {
        ClerkLog.w(
          "StorageHelper.loadValue called before initialization, returning null for key: ${key.name}"
        )
        null
      }
      storedValue == null -> null
      !storedValue.startsWith(ENCRYPTED_VALUE_PREFIX) -> {
        migrateLegacyPlaintextValue(key, storedValue)
        storedValue
      }
      cipher == null -> {
        ClerkLog.w("Encrypted storage is unavailable, returning null for key: ${key.name}")
        null
      }
      else -> {
        runCatching { cipher.decrypt(storedValue.removePrefix(ENCRYPTED_VALUE_PREFIX)) }
          .onFailure { error ->
            ClerkLog.w("Failed to decrypt stored value for key ${key.name}: ${error.message}")
            prefs.edit(commit = true) { remove(key.name) }
          }
          .getOrNull()
      }
    }
  }

  /** Delete value of string type from [secureStorage] */
  internal fun deleteValue(key: StorageKey) {
    val prefs = secureStorage
    if (prefs == null) {
      ClerkLog.w(
        "StorageHelper.deleteValue called before initialization, ignoring delete for key: ${key.name}"
      )
      return
    }
    prefs.edit { remove(key.name) }
  }

  /**
   * Resets the storage helper for testing purposes. This method should only be used in tests.
   * Clears all stored values. To test uninitialized state, tests should call this and then test
   * methods before calling initialize().
   */
  @VisibleForTesting
  internal fun reset(context: Context? = null) {
    val prefs = secureStorage
    if (prefs != null) {
      prefs.edit().clear().commit()
    }
    storageCipher = null
    if (context != null) {
      // Reinitialize to ensure clean state
      initialize(context)
    } else {
      // Allow tests to simulate uninitialized state.
      secureStorage = null
    }
  }

  private fun migrateLegacyPlaintextValue(key: StorageKey, value: String) {
    if (value.isEmpty()) {
      return
    }

    val cipher = storageCipher ?: return
    runCatching { ENCRYPTED_VALUE_PREFIX + cipher.encrypt(value) }
      .onSuccess { encryptedValue ->
        secureStorage?.edit(commit = true) { putString(key.name, encryptedValue) }
      }
      .onFailure { error ->
        ClerkLog.w("Failed to migrate plaintext value for key ${key.name}: ${error.message}")
      }
  }
}

internal enum class StorageKey {
  DEVICE_TOKEN,
  DEVICE_ID,
}
