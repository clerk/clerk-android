package com.clerk.ui.auth

import android.content.SharedPreferences

internal class InMemorySharedPreferences : SharedPreferences {
  private val values = mutableMapOf<String, Any?>()

  override fun getAll(): MutableMap<String, *> = values.toMutableMap()

  override fun getString(key: String?, defValue: String?): String? =
    values[key] as? String ?: defValue

  @Suppress("UNCHECKED_CAST")
  override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
    values[key] as? MutableSet<String> ?: defValues

  override fun getInt(key: String?, defValue: Int): Int = values[key] as? Int ?: defValue

  override fun getLong(key: String?, defValue: Long): Long = values[key] as? Long ?: defValue

  override fun getFloat(key: String?, defValue: Float): Float = values[key] as? Float ?: defValue

  override fun getBoolean(key: String?, defValue: Boolean): Boolean =
    values[key] as? Boolean ?: defValue

  override fun contains(key: String?): Boolean = values.containsKey(key)

  override fun edit(): SharedPreferences.Editor = Editor()

  override fun registerOnSharedPreferenceChangeListener(
    listener: SharedPreferences.OnSharedPreferenceChangeListener?
  ) = Unit

  override fun unregisterOnSharedPreferenceChangeListener(
    listener: SharedPreferences.OnSharedPreferenceChangeListener?
  ) = Unit

  private inner class Editor : SharedPreferences.Editor {
    private val updates = mutableMapOf<String, Any?>()
    private val removals = mutableSetOf<String>()
    private var shouldClear = false

    override fun putString(key: String?, value: String?): SharedPreferences.Editor =
      putValue(key, value)

    override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor =
      putValue(key, values)

    override fun putInt(key: String?, value: Int): SharedPreferences.Editor = putValue(key, value)

    override fun putLong(key: String?, value: Long): SharedPreferences.Editor = putValue(key, value)

    override fun putFloat(key: String?, value: Float): SharedPreferences.Editor =
      putValue(key, value)

    override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor =
      putValue(key, value)

    override fun remove(key: String?): SharedPreferences.Editor = apply {
      key?.let { removals += it }
    }

    override fun clear(): SharedPreferences.Editor = apply { shouldClear = true }

    override fun commit(): Boolean {
      apply()
      return true
    }

    override fun apply() {
      if (shouldClear) values.clear()
      removals.forEach(values::remove)
      values.putAll(updates)
    }

    private fun putValue(key: String?, value: Any?): SharedPreferences.Editor = apply {
      key?.let { updates[it] = value }
    }
  }
}
