package com.clerk.api.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import androidx.core.content.edit
import com.clerk.api.Constants.Storage.CLERK_PREFERENCES_FILE_NAME
import com.clerk.api.log.ClerkLog

/**
 * Helper class to manage secure storage of data. SharedPreferences are used to store data, all keys
 * are held in the [StorageKey] object.
 */
internal object StorageHelper {

  @Volatile private var secureStorage: SharedPreferences? = null

  /**
   * Synchronously initializes the secure storage. We do this synchronously because we need to
   * ensure that the storage is initialized before we generate a device ID.
   */
  fun initialize(context: Context) {
    secureStorage = context.getSharedPreferences(CLERK_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)
  }

  /** Save value of string type to [secureStorage] */
  internal fun saveValue(key: StorageKey, value: String) {
    val prefs = secureStorage
    if (prefs == null) {
      ClerkLog.w(
        "StorageHelper.saveValue called before initialization, ignoring save for key: ${key.name}"
      )
      return
    }
    if (value.isNotEmpty()) {
      prefs.edit(commit = true) { putString(key.name, value) }
      return
    }
  }

  /** Load value of string type from [secureStorage] */
  internal fun loadValue(key: StorageKey): String? {
    val prefs = secureStorage
    if (prefs == null) {
      ClerkLog.w(
        "StorageHelper.loadValue called before initialization, returning null for key: ${key.name}"
      )
      return null
    }
    return prefs.getString(key.name, null)
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
    if (context != null) {
      // Reinitialize to ensure clean state
      initialize(context)
    } else {
      // Allow tests to simulate uninitialized state.
      secureStorage = null
    }
  }
}

internal enum class StorageKey {
  DEVICE_TOKEN,
  DEVICE_ID,
}
