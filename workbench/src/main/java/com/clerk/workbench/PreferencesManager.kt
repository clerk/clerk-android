package com.clerk.workbench

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.clerk.workbench.StorageHelper.secureStorage

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
    secureStorage = context.getSharedPreferences("WORKBENCH_STORAGE", Context.MODE_PRIVATE)
  }

  /** Save value of string type to [secureStorage] */
  internal fun saveValue(key: StorageKey, value: String) {
    if (value.isNotEmpty()) {
      secureStorage.edit(commit = true) { putString(key.name, value) }
      return
    }
  }

  /** Load value of string type from [secureStorage] */
  internal fun loadValue(key: StorageKey): String? {
    return secureStorage.getString(key.name, null)
  }

  /** Delete value of string type from [secureStorage] */
  internal fun deleteValue(key: StorageKey) {
    secureStorage.edit { remove(key.name) }
  }
}

internal enum class StorageKey {
  PUBLIC_KEY
}
