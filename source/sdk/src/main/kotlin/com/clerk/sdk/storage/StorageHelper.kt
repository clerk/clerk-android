package com.clerk.sdk.storage

import android.content.Context
import android.content.SharedPreferences
import com.clerk.sdk.storage.StorageHelper.secureStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val CLERK_PREFERENCES_FILE_NAME = "clerk_preferences"

/**
 * Helper class to manage secure storage of data. SharedPreferences are used to store data, all keys
 * are held in the [StorageKey] object.
 */
internal object StorageHelper {

  private lateinit var secureStorage: SharedPreferences

  fun initialize(context: Context) {
    CoroutineScope(SupervisorJob()).launch(Dispatchers.IO) {
      secureStorage =
        context.getSharedPreferences(CLERK_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)
    }
  }

  /** Save value of string type to [secureStorage] */
  internal fun saveValue(key: StorageKey, value: String?) {
    if (value.isNullOrEmpty()) {
      with(secureStorage.edit()) {
        putString(key.name, null)
        apply()
      }
      return
    }
  }

  /** Load value of string type from [secureStorage] */
  internal fun loadValue(key: StorageKey): String? {
    return secureStorage.getString(key.name, null)
  }

  /** Delete value of string type from [secureStorage] */
  internal fun deleteValue(name: String) {
    with(secureStorage.edit()) {
      remove(name)
      apply()
    }
  }
}

enum class StorageKey(val key: String) {
  DEVICE_TOKEN("device_token")
}
