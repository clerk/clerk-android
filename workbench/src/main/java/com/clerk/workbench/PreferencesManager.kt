package com.clerk.workbench

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.clerk.workbench.StorageHelper.preferences

internal object StorageHelper {

  private lateinit var preferences: SharedPreferences

  fun initialize(context: Context) {
    preferences = context.getSharedPreferences("WORKBENCH_STORAGE", Context.MODE_PRIVATE)
  }

  internal fun saveValue(key: StorageKey, value: String) {
    if (value.isNotEmpty()) {
      preferences.edit(commit = true) { putString(key.name, value) }
      return
    }
  }

  internal fun loadValue(key: StorageKey): String? {
    return preferences.getString(key.name, null)
  }

  internal fun deleteValue(key: StorageKey) {
    preferences.edit { remove(key.name) }
  }
}

internal enum class StorageKey {
  PUBLIC_KEY,
  PROXY_URL,
}
