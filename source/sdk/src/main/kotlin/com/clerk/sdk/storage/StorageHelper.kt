package com.clerk.sdk.storage

import android.content.Context
import android.content.SharedPreferences
import com.clerk.sdk.storage.StorageHelper.secureStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val CLERK_PREFERENCES_FILE_NAME = "clerk_preferences"

internal object StorageHelper {

  private lateinit var secureStorage: SharedPreferences

  fun initialize(context: Context) {
    CoroutineScope(SupervisorJob()).launch(Dispatchers.IO) {
      secureStorage =
        context.getSharedPreferences(CLERK_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)
    }
  }

  /** Save value of string type to [secureStorage] */
  internal fun saveValue(name: String, value: String?) {
    if (value.isNullOrEmpty()) {
      with(secureStorage.edit()) {
        putString(name, null)
        apply()
      }
      return
    }
  }

  /** Load value of string type from [secureStorage] */
  internal fun loadValue(name: String): String? {
    return secureStorage.getString(name, null)
  }

  /** Delete value of string type from [secureStorage] */
  internal fun deleteValue(name: String) {
    with(secureStorage.edit()) {
      remove(name)
      apply()
    }
  }
}
