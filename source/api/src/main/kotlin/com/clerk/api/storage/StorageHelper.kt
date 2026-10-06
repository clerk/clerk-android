package com.clerk.api.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import androidx.core.content.edit
import com.clerk.api.Constants.Storage.CLERK_PREFERENCES_FILE_NAME
import com.clerk.api.log.ClerkLog

@Suppress("TooManyFunctions")
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

  /** Guards device-token read-compare-write sequences; listeners are notified outside it. */
  private val deviceTokenLock = Any()

  internal fun saveValue(key: StorageKey, value: String) {
    if (key != StorageKey.DEVICE_TOKEN) {
      writeValue(key, value)
      return
    }
    val previousValue: String?
    val saved: Boolean
    synchronized(deviceTokenLock) {
      previousValue = loadValue(key)
      saved = writeValue(key, value)
    }
    if (saved) notifyDeviceTokenChange(previousValue, value)
  }

  /**
   * Atomically replaces the device token with [value] (deleting it when null) only if the stored
   * token equals [expected]. Returns true when the stored token equals [value] after this call.
   *
   * [onChangedLocked] runs inside the device-token lock, only when the stored token actually
   * changed, so callers can publish side effects (such as a response fence) atomically with the
   * swap. It must be cheap and must not take other locks.
   */
  internal fun compareAndSetDeviceToken(
    expected: String?,
    value: String?,
    onChangedLocked: () -> Unit = {},
  ): Boolean {
    val key = StorageKey.DEVICE_TOKEN
    val previousValue: String?
    val swapped: Boolean
    synchronized(deviceTokenLock) {
      previousValue = loadValue(key)
      swapped =
        when {
          previousValue != expected -> false
          previousValue == value -> true
          value == null -> removeValue(key)
          else -> writeValue(key, value)
        }
      if (swapped && previousValue != value) onChangedLocked()
    }
    if (swapped) notifyDeviceTokenChange(previousValue, value)
    return swapped
  }

  private fun writeValue(key: StorageKey, value: String): Boolean {
    val prefs = secureStorage
    val cipher = storageCipher

    return when {
      prefs == null -> {
        ClerkLog.w(
          "StorageHelper.saveValue called before initialization, ignoring save for key: ${key.name}"
        )
        false
      }
      value.isEmpty() -> false
      cipher == null -> {
        ClerkLog.w("Encrypted storage is unavailable, ignoring save for key: ${key.name}")
        false
      }
      else -> {
        runCatching { ENCRYPTED_VALUE_PREFIX + cipher.encrypt(value) }
          .onFailure { error ->
            ClerkLog.w("Failed to encrypt value for key ${key.name}: ${error.message}")
          }
          .map { encryptedValue ->
            commit(prefs, key, cachedDeviceToken = value) { putString(key.name, encryptedValue) }
          }
          .getOrDefault(false)
      }
    }
  }

  internal fun loadValue(key: StorageKey): String? =
    if (key == StorageKey.DEVICE_TOKEN) {
      DeviceTokenCache.getOrLoad(load = { readValue(key) }, canCache = ::isEncryptedStorageReady)
    } else {
      readValue(key)
    }

  private fun isEncryptedStorageReady(): Boolean = secureStorage != null && storageCipher != null

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
            removeIfStillStored(prefs, key, storedValue)
          }
          .getOrNull()
      }
    }
  }

  internal fun deleteValue(key: StorageKey) {
    if (key != StorageKey.DEVICE_TOKEN) {
      removeValue(key)
      return
    }
    val previousValue: String?
    val removed: Boolean
    synchronized(deviceTokenLock) {
      previousValue = loadValue(key)
      removed = removeValue(key)
    }
    if (removed) notifyDeviceTokenChange(previousValue, null)
  }

  private fun removeValue(key: StorageKey): Boolean {
    val prefs = secureStorage
    if (prefs == null) {
      ClerkLog.w(
        "StorageHelper.deleteValue called before initialization, ignoring delete for key: ${key.name}"
      )
      return false
    }
    return commit(prefs, key, cachedDeviceToken = null) { remove(key.name) }
  }

  private fun removeIfStillStored(prefs: SharedPreferences, key: StorageKey, storedValue: String) {
    val isStillStored = { prefs.getString(key.name, null) == storedValue }
    val remove = { commitEdit(prefs, key) { remove(key.name) } }
    if (key == StorageKey.DEVICE_TOKEN) {
      DeviceTokenCache.removeIf(isStillStored, remove)
    } else if (isStillStored()) {
      remove()
    }
  }

  private inline fun commit(
    prefs: SharedPreferences,
    key: StorageKey,
    cachedDeviceToken: String?,
    crossinline edit: SharedPreferences.Editor.() -> Unit,
  ): Boolean =
    if (key == StorageKey.DEVICE_TOKEN) {
      DeviceTokenCache.write(cachedDeviceToken) { commitEdit(prefs, key) { edit() } }
    } else {
      commitEdit(prefs, key, edit)
    }

  private fun notifyDeviceTokenChange(previousValue: String?, value: String?) {
    if (previousValue != value) {
      valueChangeListener?.invoke(StorageKey.DEVICE_TOKEN, previousValue, value)
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

/** Commits synchronously and reports whether the write reached disk. */
private inline fun commitEdit(
  prefs: SharedPreferences,
  key: StorageKey,
  edit: SharedPreferences.Editor.() -> Unit,
): Boolean {
  val committed = prefs.edit().apply(edit).commit()
  if (!committed) ClerkLog.w("Failed to commit storage change for key: ${key.name}")
  return committed
}

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

  fun write(value: String?, commit: () -> Boolean): Boolean =
    synchronized(lock) {
      val committed = commit()
      generation += 1
      entry = if (committed) Entry(value) else null
      committed
    }

  fun removeIf(condition: () -> Boolean, remove: () -> Boolean): Boolean =
    synchronized(lock) { condition() && write(value = null, commit = remove) }

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
