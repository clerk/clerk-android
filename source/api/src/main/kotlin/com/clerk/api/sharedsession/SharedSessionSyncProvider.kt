package com.clerk.api.sharedsession

import android.content.ComponentName
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Binder
import android.os.CancellationSignal
import android.os.Process
import com.clerk.api.storage.StorageHelper
import com.clerk.api.storage.StorageKey

/** Read-only IPC boundary for same-signed sibling apps participating in shared-session sync. */
internal class SharedSessionSyncProvider : ContentProvider() {
  override fun onCreate(): Boolean {
    val providerContext = context ?: return false
    StorageHelper.initialize(providerContext)
    return true
  }

  override fun query(
    uri: Uri,
    projection: Array<out String>?,
    selection: String?,
    selectionArgs: Array<out String>?,
    sortOrder: String?,
  ): Cursor {
    enforceSameSignature()
    require(uri.path == SharedSessionSyncContract.SNAPSHOT_PATH) { "Unsupported URI: $uri" }
    require(selection == null && selectionArgs == null && sortOrder == null) {
      "Shared-session snapshots do not support query parameters"
    }

    return MatrixCursor(arrayOf(SharedSessionSyncContract.SNAPSHOT_COLUMN), 1).apply {
      addRow(arrayOf(StorageHelper.loadValue(StorageKey.SHARED_SESSION_SYNC_SNAPSHOT)))
    }
  }

  override fun query(
    uri: Uri,
    projection: Array<out String>?,
    queryArgs: android.os.Bundle?,
    cancellationSignal: CancellationSignal?,
  ): Cursor = query(uri, projection, null, null, null)

  override fun getType(uri: Uri): String = SharedSessionSyncContract.MIME_TYPE

  override fun insert(uri: Uri, values: ContentValues?): Uri? = readOnly()

  override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
    readOnly()

  override fun update(
    uri: Uri,
    values: ContentValues?,
    selection: String?,
    selectionArgs: Array<out String>?,
  ): Int = readOnly()

  private fun enforceSameSignature() {
    val providerContext = requireNotNull(context)
    val callingUid = Binder.getCallingUid()
    if (callingUid == Process.myUid()) return

    val result = providerContext.packageManager.checkSignatures(callingUid, Process.myUid())
    if (result != PackageManager.SIGNATURE_MATCH) {
      throw SecurityException("Shared Clerk session state is available only to same-signed apps")
    }
  }

  private fun <T> readOnly(): T {
    throw UnsupportedOperationException("Shared Clerk session state is read-only")
  }

  companion object {
    internal fun setEnabled(context: Context, enabled: Boolean) {
      context.applicationContext.packageManager.setComponentEnabledSetting(
        ComponentName(context.applicationContext, SharedSessionSyncProvider::class.java),
        if (enabled) {
          PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
          PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        },
        PackageManager.DONT_KILL_APP,
      )
    }
  }
}

internal object SharedSessionSyncContract {
  const val DISCOVERY_ACTION = "com.clerk.api.action.SHARED_SESSION_SYNC"
  const val AUTHORITY_SUFFIX = ".clerk.shared-session-sync"
  const val SNAPSHOT_PATH = "/snapshot"
  const val SNAPSHOT_COLUMN = "snapshot"
  const val MIME_TYPE = "vnd.android.cursor.item/vnd.com.clerk.shared-session-sync"

  fun snapshotUri(authority: String): Uri =
    Uri.Builder().scheme("content").authority(authority).path(SNAPSHOT_PATH).build()
}
