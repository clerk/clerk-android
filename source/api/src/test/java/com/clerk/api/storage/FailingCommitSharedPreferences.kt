package com.clerk.api.storage

import android.content.SharedPreferences

/**
 * Reads from [delegate] but drops every edit: [SharedPreferences.Editor.commit] returns false and
 * nothing reaches [delegate], as when a synchronous write fails to reach disk.
 */
internal class FailingCommitSharedPreferences(private val delegate: SharedPreferences) :
  SharedPreferences by delegate {
  override fun edit(): SharedPreferences.Editor = FailingCommitEditor()

  private class FailingCommitEditor : SharedPreferences.Editor {
    override fun putString(key: String?, value: String?) = this

    override fun putStringSet(key: String?, values: MutableSet<String>?) = this

    override fun putInt(key: String?, value: Int) = this

    override fun putLong(key: String?, value: Long) = this

    override fun putFloat(key: String?, value: Float) = this

    override fun putBoolean(key: String?, value: Boolean) = this

    override fun remove(key: String?) = this

    override fun clear() = this

    override fun commit() = false

    override fun apply() = Unit
  }
}

/**
 * Makes every [StorageHelper] write fail to commit until the returned restore function is invoked.
 * [StorageHelper] must already be initialized.
 */
internal fun StorageHelper.failCommitsForTesting(): () -> Unit {
  val field = StorageHelper::class.java.getDeclaredField("secureStorage")
  field.isAccessible = true
  val original = requireNotNull(field.get(this) as SharedPreferences?) { "Storage not initialized" }
  field.set(this, FailingCommitSharedPreferences(original))
  return { field.set(this, original) }
}
