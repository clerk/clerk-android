package com.clerk.api.sharedsession

import android.content.Context
import com.clerk.api.Clerk
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.storage.StorageHelper
import com.clerk.api.storage.StorageKey
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("TooManyFunctions")
internal class SharedSessionSyncCoordinator
internal constructor(
  private val instanceId: String,
  private val transport: SharedSessionSyncTransport,
  private val clock: () -> Long = System::currentTimeMillis,
) {
  constructor(
    context: Context,
    publishableKey: String,
  ) : this(
    instanceId = SharedSessionSyncSnapshot.instanceId(publishableKey),
    transport = ContentProviderSharedSessionSyncTransport(context),
  )

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val stateLock = Any()
  private var localSnapshot: SharedSessionSyncSnapshot? = null
  private var isApplyingSharedStorage = false
  private val storageListener = { key: StorageKey, previous: String?, value: String? ->
    if (key == StorageKey.DEVICE_TOKEN) {
      handleDeviceTokenChange(previous = previous, value = value)
    }
  }

  fun start() {
    StorageHelper.valueChangeListener = storageListener
    transport.start { scope.launch { reloadFromSharedStorage(force = false) } }
  }

  fun close() {
    if (StorageHelper.valueChangeListener === storageListener) {
      StorageHelper.valueChangeListener = null
    }
    transport.close()
    scope.cancel()
  }

  suspend fun reloadFromSharedStorage(force: Boolean): Boolean =
    withContext(Dispatchers.IO) { reloadBlocking(force) }

  fun handleClientChange(client: Client, serverFetchAtMillis: Long) {
    synchronized(stateLock) {
      if (isApplyingSharedStorage) return
      val state =
        if (client == Client()) SharedSessionSyncSnapshot.State.CLEARED
        else SharedSessionSyncSnapshot.State.SET
      val authSnapshot =
        SharedSessionSyncSnapshot.AuthSnapshot(
          state = state,
          version = UUID.randomUUID().toString(),
          serverFetchAtMillis = serverFetchAtMillis,
          client = client.takeIf { state == SharedSessionSyncSnapshot.State.SET },
        )
      persistLocalSnapshot(baseSnapshot().copy(auth = authSnapshot), notifyPeers = true)
    }
  }

  fun handleEnvironmentChange(previous: Environment?, environment: Environment) {
    synchronized(stateLock) {
      if (isApplyingSharedStorage || previous == environment) return
      val environmentSnapshot =
        SharedSessionSyncSnapshot.EnvironmentSnapshot(
          version = UUID.randomUUID().toString(),
          changedAtMillis = clock(),
          value = environment,
        )
      persistLocalSnapshot(
        baseSnapshot().copy(environment = environmentSnapshot),
        notifyPeers = true,
      )
    }
  }

  @Suppress("UNUSED_PARAMETER")
  private fun reloadBlocking(force: Boolean): Boolean {
    val storedLocalSnapshot = transport.loadLocalSnapshot()?.takeIf(::isCompatible)
    val peerSnapshots = transport.loadPeerSnapshots().filter(::isCompatible)

    synchronized(stateLock) {
      localSnapshot = storedLocalSnapshot ?: localSnapshot
      val snapshots = listOfNotNull(localSnapshot) + peerSnapshots
      if (snapshots.isEmpty()) return false

      var didChange = false
      selectLatestAuthSnapshot(snapshots)?.let { incoming ->
        didChange = applyAuthSnapshot(incoming) || didChange
      }
      snapshots
        .mapNotNull { it.environment }
        .maxByOrNull { it.changedAtMillis }
        ?.let { incoming -> didChange = applyEnvironmentSnapshot(incoming) || didChange }
      snapshots
        .mapNotNull { it.deviceToken }
        .maxByOrNull { it.changedAtMillis }
        ?.let { incoming -> didChange = applyDeviceTokenSnapshot(incoming) || didChange }
      return didChange
    }
  }

  private fun applyAuthSnapshot(incoming: SharedSessionSyncSnapshot.AuthSnapshot): Boolean {
    val current = currentAuthSnapshot()
    return when (decideSharedClientSnapshot(incoming = incoming, current = current)) {
      SharedSessionSyncClientDecision.APPLY -> {
        val changed =
          current?.state != incoming.state ||
            current.client != incoming.client ||
            current.serverFetchAtMillis != incoming.serverFetchAtMillis
        applyingSharedStorage {
          Clerk.updateClient(
            client = incoming.client ?: Client(),
            serverFetchAtMillis = incoming.serverFetchAtMillis ?: clock(),
          )
        }
        replicateSnapshot(baseSnapshot().copy(auth = incoming))
        changed
      }
      SharedSessionSyncClientDecision.IGNORE -> {
        if (localSnapshot?.auth?.version != incoming.version) {
          replicateSnapshot(baseSnapshot().copy(auth = incoming))
        }
        false
      }
      SharedSessionSyncClientDecision.REJECT_STALE -> {
        repairSharedAuthSnapshot()
        false
      }
    }
  }

  private fun applyEnvironmentSnapshot(
    incoming: SharedSessionSyncSnapshot.EnvironmentSnapshot
  ): Boolean {
    val localEnvironment = localSnapshot?.environment
    if (localEnvironment != null && localEnvironment.changedAtMillis > incoming.changedAtMillis) {
      return false
    }

    val changed = Clerk.environment != incoming.value
    if (changed) {
      applyingSharedStorage { Clerk.updateEnvironment(incoming.value) }
    }
    if (localEnvironment?.version != incoming.version) {
      replicateSnapshot(baseSnapshot().copy(environment = incoming))
    }
    return changed
  }

  @Suppress("ReturnCount")
  private fun applyDeviceTokenSnapshot(
    incoming: SharedSessionSyncSnapshot.DeviceTokenSnapshot
  ): Boolean {
    val localDeviceToken = localSnapshot?.deviceToken
    if (localDeviceToken != null && localDeviceToken.changedAtMillis > incoming.changedAtMillis) {
      return false
    }
    if (localDeviceToken?.version == incoming.version) return false

    val previousToken = StorageHelper.loadValue(StorageKey.DEVICE_TOKEN)
    applyingSharedStorage {
      when (incoming.state) {
        SharedSessionSyncSnapshot.State.SET -> {
          incoming.value?.let { value -> StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, value) }
        }
        SharedSessionSyncSnapshot.State.CLEARED ->
          StorageHelper.deleteValue(StorageKey.DEVICE_TOKEN)
      }
    }
    Clerk.fenceClientResponsesAfterSharedDeviceTokenChange()
    replicateSnapshot(baseSnapshot().copy(deviceToken = incoming))
    return previousToken != incoming.value || localDeviceToken?.version != incoming.version
  }

  private fun handleDeviceTokenChange(previous: String?, value: String?) {
    synchronized(stateLock) {
      if (isApplyingSharedStorage || previous == value) return
      val deviceTokenSnapshot =
        SharedSessionSyncSnapshot.DeviceTokenSnapshot(
          state =
            if (value == null) SharedSessionSyncSnapshot.State.CLEARED
            else SharedSessionSyncSnapshot.State.SET,
          version = UUID.randomUUID().toString(),
          changedAtMillis = clock(),
          value = value,
        )
      persistLocalSnapshot(
        baseSnapshot().copy(deviceToken = deviceTokenSnapshot),
        notifyPeers = true,
      )
    }
  }

  private fun repairSharedAuthSnapshot() {
    val current = currentAuthSnapshot() ?: return
    val repaired = current.copy(version = UUID.randomUUID().toString())
    persistLocalSnapshot(baseSnapshot().copy(auth = repaired), notifyPeers = true)
  }

  private fun currentAuthSnapshot(): SharedSessionSyncSnapshot.AuthSnapshot? {
    val currentClient = Clerk.clientFlow.value
    val serverFetchAtMillis = Clerk.lastClientServerFetchAtMillis
    if (currentClient == null && serverFetchAtMillis == null) return null

    val state =
      if (currentClient == null || currentClient == Client()) {
        SharedSessionSyncSnapshot.State.CLEARED
      } else {
        SharedSessionSyncSnapshot.State.SET
      }
    return SharedSessionSyncSnapshot.AuthSnapshot(
      state = state,
      version = localSnapshot?.auth?.version.orEmpty(),
      serverFetchAtMillis = serverFetchAtMillis,
      client = currentClient.takeIf { state == SharedSessionSyncSnapshot.State.SET },
    )
  }

  private fun selectLatestAuthSnapshot(
    snapshots: List<SharedSessionSyncSnapshot>
  ): SharedSessionSyncSnapshot.AuthSnapshot? {
    return snapshots
      .mapNotNull { it.auth }
      .reduceOrNull { latest, candidate ->
        when (decideSharedClientSnapshot(incoming = candidate, current = latest)) {
          SharedSessionSyncClientDecision.APPLY -> candidate
          SharedSessionSyncClientDecision.IGNORE,
          SharedSessionSyncClientDecision.REJECT_STALE -> latest
        }
      }
  }

  private fun baseSnapshot(): SharedSessionSyncSnapshot =
    localSnapshot
      ?: transport.loadLocalSnapshot()?.takeIf(::isCompatible)
      ?: SharedSessionSyncSnapshot(instanceId = instanceId)

  private fun persistLocalSnapshot(snapshot: SharedSessionSyncSnapshot, notifyPeers: Boolean) {
    localSnapshot = snapshot
    runCatching { transport.saveLocalSnapshot(snapshot, notifyPeers) }
      .onFailure { error ->
        ClerkLog.w("Failed to persist shared Clerk session state: ${error.message}")
      }
  }

  private fun replicateSnapshot(snapshot: SharedSessionSyncSnapshot) {
    persistLocalSnapshot(snapshot = snapshot, notifyPeers = false)
  }

  private fun isCompatible(snapshot: SharedSessionSyncSnapshot): Boolean =
    snapshot.schemaVersion == SharedSessionSyncSnapshot.CURRENT_SCHEMA_VERSION &&
      snapshot.instanceId == instanceId

  private inline fun applyingSharedStorage(block: () -> Unit) {
    val wasApplyingSharedStorage = isApplyingSharedStorage
    isApplyingSharedStorage = true
    try {
      block()
    } finally {
      isApplyingSharedStorage = wasApplyingSharedStorage
    }
  }
}
