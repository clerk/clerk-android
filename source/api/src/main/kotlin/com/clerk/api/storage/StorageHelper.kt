package com.clerk.api.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.clerk.api.Constants.Storage.CLERK_PREFERENCES_FILE_NAME
import com.clerk.api.log.ClerkLog

/**
 * Helper class to manage secure storage of data. SharedPreferences are used to store data, all keys
 * are held in the [StorageKey] object.
 */
internal object StorageHelper {

  private val storageLock = Any()
  @Volatile private var secureStorage: SharedPreferences? = null

  /**
   * Synchronously initializes the secure storage. We do this synchronously because we need to
   * ensure that the storage is initialized before we generate a device ID.
   */
  fun initialize(context: Context) {
    if (secureStorage != null) {
      return
    }

    synchronized(storageLock) {
      if (secureStorage == null) {
        secureStorage =
          context.applicationContext.getSharedPreferences(
            CLERK_PREFERENCES_FILE_NAME,
            Context.MODE_PRIVATE,
          )
        ClerkLog.d("StorageHelper initialized")
      }
    }
  }

  /** Save value of string type to [secureStorage] */
  internal fun saveValue(key: StorageKey, value: String) {
    if (value.isEmpty()) {
      return
    }

    val storage = secureStorage
    if (storage == null) {
      ClerkLog.w("Attempted to save ${key.name} before storage initialization")
      return
    }

    storage.edit(commit = true) { putString(key.name, value) }
  }

  /** Load value of string type from [secureStorage] */
  internal fun loadValue(key: StorageKey): String? {
    val storage = secureStorage
    if (storage == null) {
      ClerkLog.w("Attempted to load ${key.name} before storage initialization")
      return null
    }

    return storage.getString(key.name, null)
  }

  /** Delete value of string type from [secureStorage] */
  internal fun deleteValue(name: String) {
    val storage = secureStorage
    if (storage == null) {
      ClerkLog.w("Attempted to delete $name before storage initialization")
      return
    }

    storage.edit { remove(name) }
  }

  internal fun isInitialized(): Boolean = secureStorage != null
}

internal enum class StorageKey(val key: String) {
  DEVICE_TOKEN("device_token"),
  DEVICE_ID("device_id"),
}
