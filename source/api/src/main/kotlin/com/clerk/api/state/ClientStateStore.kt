package com.clerk.api.state

import com.clerk.api.configuration.CachedClerkState
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.session.Session
import com.clerk.api.storage.StorageHelper
import com.clerk.api.storage.StorageKey
import com.clerk.api.user.User
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
internal class ClientStateStore(
  private val clock: () -> Long = System::currentTimeMillis,
  private val cacheStorage: CachedStateStorage = StorageHelperCachedStateStorage,
  private val cacheConfiguration: () -> CachedStateConfiguration? = { null },
  persistenceDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1),
  private val persistDebounceMillis: Long = DEFAULT_PERSIST_DEBOUNCE_MILLIS,
) {

  internal interface Listener {
    fun onSessionStateCommittedUnderWriteLock(previous: Session?, current: Session?) = Unit

    fun onClientCommittedAfterWriteLock(commit: ClientCommit) = Unit

    fun onEnvironmentCommittedAfterWriteLock(
      previous: Environment?,
      current: Environment,
      restoredFromCache: Boolean,
    ) = Unit
  }

  internal data class ClientCommit(
    val client: Client,
    val serverFetchAtMillis: Long,
    val revision: Long,
    val endedSession: Boolean,
    val restoredFromCache: Boolean,
  )

  /**
   * An HTTP `Date` header has one-second resolution (RFC 9110 section 5.6.7), so [arrival] orders
   * responses that share a `Date` second.
   */
  internal data class ResponseOrder(val serverDateMillis: Long, val arrival: Long) :
    Comparable<ResponseOrder> {
    override fun compareTo(other: ResponseOrder): Int =
      compareValuesBy(this, other, ResponseOrder::serverDateMillis, ResponseOrder::arrival)
  }

  internal data class ClientSnapshot(val client: Client?, val serverFetchAtMillis: Long?)

  private data class State(
    val clientOrEmptyAfterReset: Client? = null,
    val publishedClient: Client? = null,
    val serverFetchAtMillis: Long? = null,
    val environment: Environment? = null,
    val revision: Long = 0,
  )

  @Volatile internal var listener: Listener? = null

  private val lock = Any()
  @Volatile private var state = State()

  private var newestAppliedOrder: ResponseOrder? = null
  private var nextArrival = 0L

  private val _client = MutableStateFlow<Client?>(null)
  private val _sessions = MutableStateFlow<List<Session>>(emptyList())
  private val _session = MutableStateFlow<Session?>(null)
  private val _user = MutableStateFlow<User?>(null)
  private val _organizationLogoUrl = MutableStateFlow<String?>(null)
  private val _multiSessionModeIsEnabled = MutableStateFlow(false)

  val clientFlow: StateFlow<Client?> = _client.asStateFlow()

  val sessionsFlow: StateFlow<List<Session>> = _sessions.asStateFlow()

  val sessionFlow: StateFlow<Session?> = _session.asStateFlow()

  val userFlow: StateFlow<User?> = _user.asStateFlow()

  val organizationLogoUrlFlow: StateFlow<String?> = _organizationLogoUrl.asStateFlow()

  val multiSessionModeIsEnabledFlow: StateFlow<Boolean> = _multiSessionModeIsEnabled.asStateFlow()

  val client: Client?
    get() = state.clientOrEmptyAfterReset

  val environment: Environment?
    get() = state.environment

  val serverFetchAtMillis: Long?
    get() = state.serverFetchAtMillis

  val revision: Long
    get() = state.revision

  fun clientSnapshot(): ClientSnapshot = state.let {
    ClientSnapshot(it.publishedClient, it.serverFetchAtMillis)
  }

  // region Client writes

  fun observeResponse(serverDateMillis: Long?): ResponseOrder? {
    serverDateMillis ?: return null
    return synchronized(lock) {
      ResponseOrder(serverDateMillis, arrival = nextArrival++)
    }
  }

  fun applyClientResponse(client: Client, order: ResponseOrder?): Boolean {
    val commit =
      synchronized(lock) {
        if (isStaleLocked(order)) {
          logDroppedStaleClient(client, order)
          return false
        }
        advanceNewestAppliedOrderLocked(order)
        commitClientLocked(
          client = client,
          serverFetchAtMillis = order?.serverDateMillis ?: clock(),
        )
      }
    dispatch(commit)
    return true
  }

  fun applyClientResponse(client: Client, serverDateMillis: Long?): Boolean =
    applyClientResponse(client, observeResponse(serverDateMillis))

  fun applyClient(client: Client) {
    val commit = synchronized(lock) { commitLocalLocked(client) }
    dispatch(commit)
  }

  fun applyClientIfUnchangedSince(
    expectedRevision: Long,
    client: Client,
    order: ResponseOrder? = null,
  ): Boolean {
    val commit =
      synchronized(lock) {
        if (state.revision != expectedRevision) {
          ClerkLog.d(
            "Dropped client ${client.id} fetched at revision $expectedRevision; " +
              "state is at revision ${state.revision}"
          )
          return false
        }
        advanceNewestAppliedOrderLocked(order)
        if (order != null) {
          commitClientLocked(client = client, serverFetchAtMillis = order.serverDateMillis)
        } else {
          commitLocalLocked(client)
        }
      }
    dispatch(commit)
    return true
  }

  fun mutateClient(transformUnderWriteLock: (Client) -> Client?): Boolean {
    val commit =
      synchronized(lock) {
        state.clientOrEmptyAfterReset?.let(transformUnderWriteLock)?.let { next ->
          commitLocalLocked(next)
        }
      } ?: return false
    dispatch(commit)
    return true
  }

  fun replaceClient(client: Client, serverFetchAtMillis: Long) {
    val commit =
      synchronized(lock) {
        commitClientLocked(client = client, serverFetchAtMillis = serverFetchAtMillis)
      }
    dispatch(commit)
  }

  fun replaceClientIfUnset(client: Client, serverFetchAtMillis: Long): Boolean {
    val commit =
      synchronized(lock) {
        if (state.publishedClient != null) return false
        commitClientLocked(
          client = client,
          serverFetchAtMillis = serverFetchAtMillis,
          revision = state.revision,
          restoredFromCache = true,
        )
      }
    dispatch(commit)
    return true
  }

  fun clearSessionState() {
    synchronized(lock) {
      val previousSession = _session.value
      state = state.copy(revision = state.revision + 1)
      _sessions.value = emptyList()
      _session.value = null
      _user.value = null
      notifySessionStateLocked(previousSession, null)
    }
  }

  fun recomputeSessionState() {
    synchronized(lock) { publishSessionStateLocked(state.clientOrEmptyAfterReset) }
  }

  // endregion

  // region Environment writes

  fun applyEnvironment(environment: Environment) {
    val previous = synchronized(lock) { commitEnvironmentLocked(environment) }
    dispatchEnvironment(previous, environment, restoredFromCache = false)
  }

  fun applyEnvironmentIfUnset(environment: Environment): Boolean {
    val previous =
      synchronized(lock) {
        if (state.environment != null) return false
        commitEnvironmentLocked(environment)
      }
    dispatchEnvironment(previous, environment, restoredFromCache = true)
    return true
  }

  fun overwriteEnvironmentWithoutSideEffects(environment: Environment?) {
    synchronized(lock) { state = state.copy(environment = environment) }
  }

  // endregion

  fun reset() {
    synchronized(lock) {
      val previousSession = _session.value
      newestAppliedOrder = null
      state =
        State(
          clientOrEmptyAfterReset = Client(),
          publishedClient = null,
          serverFetchAtMillis = null,
          environment = null,
          revision = state.revision + 1,
        )
      _client.value = null
      _sessions.value = emptyList()
      _session.value = null
      _user.value = null
      notifySessionStateLocked(previousSession, null)
    }
    clearPersistedState()
  }

  // region Persistence

  private val persistenceScope = CoroutineScope(SupervisorJob() + persistenceDispatcher)
  private val persistPending = AtomicBoolean(false)
  private val persistLock = Any()

  fun flushPersistence() {
    persistPending.set(false)
    writeLatestSnapshot()
  }

  private fun schedulePersist() {
    if (!persistPending.compareAndSet(false, true)) return
    persistenceScope.launch {
      delay(persistDebounceMillis)
      if (persistPending.getAndSet(false)) {
        writeLatestSnapshot()
      }
    }
  }

  private fun writeLatestSnapshot() {
    synchronized(persistLock) {
      runCatching { completeSnapshot()?.let(cacheStorage::save) }
        .onFailure { error -> ClerkLog.w("Failed to cache Clerk state: ${error.message}") }
    }
  }

  private fun clearPersistedState() {
    persistPending.set(false)
    synchronized(persistLock) {
      runCatching { cacheStorage.clear() }
        .onFailure { error -> ClerkLog.w("Failed to clear cached Clerk state: ${error.message}") }
    }
  }

  private fun completeSnapshot(): CachedClerkState? {
    val configuration = cacheConfiguration() ?: return null
    val current = state
    val client = current.publishedClient
    val environment = current.environment
    val serverFetchAtMillis = current.serverFetchAtMillis
    return if (client != null && environment != null && serverFetchAtMillis != null) {
      CachedClerkState(
        publishableKey = configuration.publishableKey,
        baseUrl = configuration.baseUrl,
        client = client,
        environment = environment,
        clientServerFetchAtMillis = serverFetchAtMillis,
      )
    } else {
      null
    }
  }

  // endregion

  // region Internals

  private fun localFetchAtLocked(client: Client): Long {
    val resolved = client.withResolvedActiveSession(previousSession = _session.value)
    return if (state.publishedClient == resolved) state.serverFetchAtMillis ?: clock() else clock()
  }

  private fun isStaleLocked(order: ResponseOrder?): Boolean {
    val newest = newestAppliedOrder
    return order != null && newest != null && order < newest
  }

  private fun advanceNewestAppliedOrderLocked(order: ResponseOrder?) {
    if (order != null) newestAppliedOrder = maxOf(newestAppliedOrder ?: order, order)
  }

  private fun commitLocalLocked(client: Client): ClientCommit =
    commitClientLocked(client = client, serverFetchAtMillis = localFetchAtLocked(client))

  private fun commitClientLocked(
    client: Client,
    serverFetchAtMillis: Long,
    revision: Long = state.revision + 1,
    restoredFromCache: Boolean = false,
  ): ClientCommit {
    val previousSession = state.clientOrEmptyAfterReset?.currentSession()
    val resolvedClient = client.withResolvedActiveSession(previousSession = _session.value)
    state =
      state.copy(
        clientOrEmptyAfterReset = resolvedClient,
        publishedClient = resolvedClient,
        serverFetchAtMillis = serverFetchAtMillis,
        revision = revision,
      )
    _client.value = resolvedClient
    val currentSession = publishSessionStateLocked(resolvedClient)
    return ClientCommit(
      client = resolvedClient,
      serverFetchAtMillis = serverFetchAtMillis,
      revision = revision,
      endedSession = previousSession != null && currentSession == null,
      restoredFromCache = restoredFromCache,
    )
  }

  private fun publishSessionStateLocked(client: Client?): Session? {
    val previousSession = _session.value
    val sessions = client?.sessions.orEmpty()
    val currentSession = client?.currentSession()

    if (currentSession?.status == Session.SessionStatus.PENDING) {
      ClerkLog.w(
        "Session is in pending state. " +
          "The user has tasks to complete before the session can be activated. " +
          "Session tokens cannot be issued for pending sessions."
      )
    }

    _sessions.value = sessions
    _session.value = currentSession
    _user.value = currentSession?.user
    notifySessionStateLocked(previousSession, currentSession)
    return currentSession
  }

  private fun notifySessionStateLocked(previous: Session?, current: Session?) {
    try {
      listener?.onSessionStateCommittedUnderWriteLock(previous, current)
    } catch (e: Exception) {
      ClerkLog.e("${e.message}")
    }
  }

  private fun commitEnvironmentLocked(environment: Environment): Environment? {
    val previous = state.environment
    state = state.copy(environment = environment)
    _organizationLogoUrl.value = environment.displayConfig.logoImageUrl
    _multiSessionModeIsEnabled.value = !environment.authConfig.singleSessionMode
    return previous
  }

  private fun dispatchEnvironment(
    previous: Environment?,
    current: Environment,
    restoredFromCache: Boolean,
  ) {
    listener?.onEnvironmentCommittedAfterWriteLock(previous, current, restoredFromCache)
    schedulePersist()
  }

  private fun dispatch(commit: ClientCommit) {
    listener?.onClientCommittedAfterWriteLock(commit)
    if (commit.endedSession) {
      flushPersistence()
    } else {
      schedulePersist()
    }
  }

  private fun logDroppedStaleClient(client: Client, order: ResponseOrder?) {
    ClerkLog.d(
      "Dropped stale client ${client.id} ordered at $order; newer state is at $newestAppliedOrder"
    )
  }

  // endregion

  internal companion object {
    const val DEFAULT_PERSIST_DEBOUNCE_MILLIS = 100L
  }
}

private fun Client.currentSession(): Session? = sessions.firstOrNull {
  it.id == lastActiveSessionId
}

private fun Client.withResolvedActiveSession(previousSession: Session?): Client {
  val currentActiveSessionId = lastActiveSessionId?.takeIf { activeSessionId ->
    sessions.any { it.id == activeSessionId }
  }
  val resolvedActiveSessionId =
    currentActiveSessionId
      ?: previousSession?.id?.takeIf { previousSessionId ->
        sessions.any { it.id == previousSessionId }
      }
      ?: if (lastActiveSessionId == null) sessions.singleOrNull()?.id else null
  return if (resolvedActiveSessionId == lastActiveSessionId || resolvedActiveSessionId == null) {
    this
  } else {
    copy(lastActiveSessionId = resolvedActiveSessionId)
  }
}

internal data class CachedStateConfiguration(val publishableKey: String, val baseUrl: String)

internal interface CachedStateStorage {
  fun save(state: CachedClerkState)

  fun clear()
}

internal object StorageHelperCachedStateStorage : CachedStateStorage {
  override fun save(state: CachedClerkState) {
    val encoded = ClerkApi.json.encodeToString(CachedClerkState.serializer(), state)
    StorageHelper.saveValue(StorageKey.CACHED_CLERK_STATE, encoded)
  }

  override fun clear() {
    StorageHelper.deleteValue(StorageKey.CACHED_CLERK_STATE)
  }
}
