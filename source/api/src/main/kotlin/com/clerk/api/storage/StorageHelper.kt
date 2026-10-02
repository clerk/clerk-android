package com.clerk.api.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import androidx.core.content.edit
import com.clerk.api.Constants.Storage.CLERK_PREFERENCES_FILE_NAME
import com.clerk.api.log.ClerkLog

internal object StorageHelper {
  private const val ENCRYPTED_VALUE_PREFIX = "clerk:v1:"

  @Volatile private var secureStorage: SharedPreferences? = null
  @Volatile private var storageCipher: StorageCipher? = null

  @VisibleForTesting internal var storageCipherFactoryOverride: (() -> StorageCipher)? = null

  @Volatile
  internal var valueChangeListener:
    ((key: StorageKey, previous: String?, value: String?) -> Unit)? =
    null

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

  internal fun saveValue(key: StorageKey, value: String) {
    val prefs = secureStorage
    val cipher = storageCipher
    val previousValue = if (key == StorageKey.DEVICE_TOKEN) loadValue(key) else null

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
            commit(prefs, key, cachedDeviceToken = value) { putString(key.name, encryptedValue) }
            if (key == StorageKey.DEVICE_TOKEN && previousValue != value) {
              valueChangeListener?.invoke(key, previousValue, value)
            }
          }
          .onFailure { error ->
            ClerkLog.w("Failed to encrypt value for key ${key.name}: ${error.message}")
          }
      }
    }
  }

  internal fun loadValue(key: StorageKey): String? =
    if (key == StorageKey.DEVICE_TOKEN) {
      DeviceTokenCache.getOrLoad(load = { readValue(key) }, canCache = { secureStorage != null })
    } else {
      readValue(key)
    }

  private fun readValue(key: StorageKey): String? {
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

  internal fun deleteValue(key: StorageKey) {
    val prefs = secureStorage
    if (prefs == null) {
      ClerkLog.w(
        "StorageHelper.deleteValue called before initialization, ignoring delete for key: ${key.name}"
      )
      return
    }
    val previousValue = if (key == StorageKey.DEVICE_TOKEN) loadValue(key) else null
    commit(prefs, key, cachedDeviceToken = null) { remove(key.name) }
    if (key == StorageKey.DEVICE_TOKEN && previousValue != null) {
      valueChangeListener?.invoke(key, previousValue, null)
    }
  }

  /**
   * Commits [edit]; for [StorageKey.DEVICE_TOKEN] the in-memory copy becomes [cachedDeviceToken].
   * Listeners are invoked by callers after this returns, outside the cache lock.
   */
  private inline fun commit(
    prefs: SharedPreferences,
    key: StorageKey,
    cachedDeviceToken: String?,
    crossinline edit: SharedPreferences.Editor.() -> Unit,
  ) {
    if (key == StorageKey.DEVICE_TOKEN) {
      DeviceTokenCache.write(cachedDeviceToken) { prefs.edit(commit = true) { edit() } }
    } else {
      prefs.edit(commit = true) { edit() }
    }
  }

  @VisibleForTesting
  internal fun reset(context: Context) {
    clearStoredState()
    initialize(context)
  }

  @VisibleForTesting
  internal fun resetToUninitializedForTesting() {
    clearStoredState()
    secureStorage = null
  }

  private fun clearStoredState() {
    valueChangeListener = null
    secureStorage?.edit()?.clear()?.commit()
    storageCipher = null
    DeviceTokenCache.invalidate()
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

/**
 * Plaintext copy of the device token, which several interceptors read on every request. Every write
 * replaces it under [lock] together with the disk commit, so concurrent writers cannot leave it
 * disagreeing with disk; [generation] lets a slow cache-miss read detect that a write overtook it
 * and skip caching the value it decrypted.
 */
private object DeviceTokenCache {
  private class Entry(val value: String?)

  private val lock = Any()
  @Volatile private var entry: Entry? = null
  @Volatile private var generation = 0L

  fun getOrLoad(load: () -> String?, canCache: () -> Boolean): String? {
    entry?.let {
      return it.value
    }
    val readGeneration = generation
    val value = load()
    synchronized(lock) {
      if (readGeneration == generation && canCache()) {
        entry = Entry(value)
      }
    }
    return value
  }

  fun write(value: String?, commit: () -> Unit) {
    synchronized(lock) {
      commit()
      generation += 1
      entry = Entry(value)
    }
  }

  fun invalidate() {
    synchronized(lock) {
      generation += 1
      entry = null
    }
  }
}

internal enum class StorageKey {
  DEVICE_TOKEN,
  DEVICE_ID,
  PENDING_NATIVE_MAGIC_LINK_FLOW,
  SHARED_SESSION_SYNC_SNAPSHOT,
  CACHED_CLERK_STATE,
  TRUSTED_DEVICE_CREDENTIALS,
  PENDING_TRUSTED_DEVICE_CREDENTIAL_CLEANUP,
}
