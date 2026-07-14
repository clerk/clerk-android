package com.clerk.api.sharedsession

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.clerk.api.log.ClerkLog
import com.clerk.api.storage.StorageHelper
import com.clerk.api.storage.StorageKey

internal interface SharedSessionSyncTransport {
  fun loadLocalSnapshot(): SharedSessionSyncSnapshot?

  fun loadPeerSnapshots(): List<SharedSessionSyncSnapshot>

  fun saveLocalSnapshot(snapshot: SharedSessionSyncSnapshot, notifyPeers: Boolean)

  fun start(onPeerChange: () -> Unit)

  fun close()
}

internal class ContentProviderSharedSessionSyncTransport(context: Context) :
  SharedSessionSyncTransport {
  private val context = context.applicationContext
  private val resolver = this.context.contentResolver
  private val packageManager = this.context.packageManager
  private val localUri =
    SharedSessionSyncContract.snapshotUri(
      this.context.packageName + SharedSessionSyncContract.AUTHORITY_SUFFIX
    )
  private val observers = mutableMapOf<Uri, ContentObserver>()
  private var peerChangeHandler: (() -> Unit)? = null

  override fun loadLocalSnapshot(): SharedSessionSyncSnapshot? =
    StorageHelper.loadValue(StorageKey.SHARED_SESSION_SYNC_SNAPSHOT)?.decodeSnapshot()

  override fun loadPeerSnapshots(): List<SharedSessionSyncSnapshot> {
    val peerUris = discoverPeerUris()
    registerObservers(peerUris)
    return peerUris.mapNotNull(::querySnapshot)
  }

  override fun saveLocalSnapshot(snapshot: SharedSessionSyncSnapshot, notifyPeers: Boolean) {
    StorageHelper.saveValue(
      StorageKey.SHARED_SESSION_SYNC_SNAPSHOT,
      SharedSessionSyncSnapshot.encode(snapshot),
    )
    if (notifyPeers) {
      resolver.notifyChange(localUri, null)
    }
  }

  override fun start(onPeerChange: () -> Unit) {
    SharedSessionSyncProvider.setEnabled(context, true)
    peerChangeHandler = onPeerChange
    registerObservers(discoverPeerUris())
  }

  override fun close() {
    observers.values.forEach { observer -> resolver.unregisterContentObserver(observer) }
    observers.clear()
    peerChangeHandler = null
    SharedSessionSyncProvider.setEnabled(context, false)
  }

  private fun discoverPeerUris(): List<Uri> {
    val intent = Intent(SharedSessionSyncContract.DISCOVERY_ACTION)
    return packageManager
      .queryIntentContentProviders(intent, 0)
      .asSequence()
      .mapNotNull { resolveInfo -> resolveInfo.providerInfo }
      .filter { providerInfo -> providerInfo.packageName != context.packageName }
      .filter { providerInfo ->
        packageManager.checkSignatures(context.packageName, providerInfo.packageName) ==
          PackageManager.SIGNATURE_MATCH
      }
      .flatMap { providerInfo -> providerInfo.authority.orEmpty().split(';').asSequence() }
      .filter { authority -> authority.isNotBlank() }
      .map(SharedSessionSyncContract::snapshotUri)
      .distinct()
      .toList()
  }

  private fun registerObservers(peerUris: List<Uri>) {
    val handler = Handler(Looper.getMainLooper())
    peerUris.filterNot(observers::containsKey).forEach { uri ->
      val observer =
        object : ContentObserver(handler) {
          override fun onChange(selfChange: Boolean) {
            onChange(selfChange, null)
          }

          override fun onChange(selfChange: Boolean, changedUri: Uri?) {
            peerChangeHandler?.invoke()
          }
        }
      runCatching { resolver.registerContentObserver(uri, false, observer) }
        .onSuccess { observers[uri] = observer }
        .onFailure { error ->
          ClerkLog.w("Failed to observe shared Clerk state at $uri: ${error.message}")
        }
    }
  }

  private fun querySnapshot(uri: Uri): SharedSessionSyncSnapshot? =
    runCatching {
        resolver
          .query(uri, arrayOf(SharedSessionSyncContract.SNAPSHOT_COLUMN), null, null, null)
          ?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val columnIndex = cursor.getColumnIndex(SharedSessionSyncContract.SNAPSHOT_COLUMN)
            if (columnIndex < 0 || cursor.isNull(columnIndex)) return@use null
            cursor.getString(columnIndex).decodeSnapshot()
          }
      }
      .onFailure { error ->
        ClerkLog.w("Failed to read shared Clerk state from $uri: ${error.message}")
      }
      .getOrNull()

  private fun String.decodeSnapshot(): SharedSessionSyncSnapshot? =
    runCatching { SharedSessionSyncSnapshot.decode(this) }
      .onFailure { error -> ClerkLog.w("Failed to decode shared Clerk state: ${error.message}") }
      .getOrNull()
}
