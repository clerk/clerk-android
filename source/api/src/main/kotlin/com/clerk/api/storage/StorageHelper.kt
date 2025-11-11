package com.clerk.api.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import androidx.core.content.edit
import com.clerk.api.Constants.Storage.CLERK_PREFERENCES_FILE_NAME
import com.clerk.api.log.ClerkLog
import com.clerk.api.storage.StorageHelper.secureStorage

/**
 * Helper class to manage secure storage of data. SharedPreferences are used to store data, all keys
 * are held in the [StorageKey] object.
 */
internal object StorageHelper {

  private lateinit var secureStorage: SharedPreferences

  /**
   * Synchronously initializes the secure storage. We do this synchronously because we need to
   * ensure that the storage is initialized before we generate a device ID.
   */
  fun initialize(context: Context) {
    secureStorage = context.getSharedPreferences(CLERK_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)
  }

  /**
   * Checks if storage has been initialized.
   */
  private fun isInitialized(): Boolean {
    return ::secureStorage.isInitialized
  }

  /** Save value of string type to [secureStorage] */
  internal fun saveValue(key: StorageKey, value: String) {
    if (!isInitialized()) {
      ClerkLog.w("StorageHelper.saveValue called before initialization, ignoring save for key: ${key.name}")
      return
    }
    if (value.isNotEmpty()) {
      secureStorage.edit(commit = true) { putString(key.name, value) }
      return
    }
  }

  /** Load value of string type from [secureStorage] */
  internal fun loadValue(key: StorageKey): String? {
    if (!isInitialized()) {
      ClerkLog.w("StorageHelper.loadValue called before initialization, returning null for key: ${key.name}")
      return null
    }
    return secureStorage.getString(key.name, null)
  }

  /** Delete value of string type from [secureStorage] */
  internal fun deleteValue(key: StorageKey) {
    if (!isInitialized()) {
      ClerkLog.w("StorageHelper.deleteValue called before initialization, ignoring delete for key: ${key.name}")
      return
    }
    secureStorage.edit { remove(key.name) }
  }

  /**
   * Resets the storage helper for testing purposes.
   * This method should only be used in tests.
   * Clears all stored values. To test uninitialized state, tests should call this
   * and then test methods before calling initialize().
   */
  @VisibleForTesting
  internal fun reset(context: Context? = null) {
    if (::secureStorage.isInitialized) {
      secureStorage.edit().clear().commit()
    }
    // If context is provided and storage is initialized, reinitialize to ensure clean state
    context?.let {
      initialize(it)
    }
  }
}

internal enum class StorageKey(val key: String) {
  DEVICE_TOKEN("device_token"),
  DEVICE_ID("device_id"),
}
