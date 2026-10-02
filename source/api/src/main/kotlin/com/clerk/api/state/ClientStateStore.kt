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

/**
 * The only writer of the SDK's client, session, user, and environment state.
 *
 * Every write runs under one lock, so concurrent writers from OkHttp, IO, and main threads are
 * serialized and readers always observe a complete commit. Reads are lock-free: synchronous getters
 * and the [StateFlow]s reflect a commit as soon as the writing call returns.
 *
 * Server responses are ordered by their `Date` header ("server fetch time"). The store keeps the
 * newest server time it has applied and drops a response that was fetched before it, so a slow
 * response that started before a newer one cannot overwrite the newer state. Only server-issued
 * times feed this watermark; local clock readings never do, so device clock skew cannot make the
 * store reject fresh responses. A caller that re-applies a decoded response body (for example the
 * result of `Client.get()`) is ordered by the fetch time the network layer recorded for that body
 * through [recordClientResponse]. Local mutations and authoritative replacements (cache hydration,
 * shared-session sync) always apply.
 *
 * The offline cache is persisted off the calling thread and coalesced: a burst of commits produces
 * one encrypted write of the latest state. Commits that end the current session flush the cache
 * synchronously, and [reset] clears it, so a signed-in snapshot cannot outlive a sign-out.
 *
 * ### Read API for other internal components
 * Components that need to react to client state (for example a redirect/auth-completion
 * coordinator) should observe [clientFlow], [sessionFlow], or [userFlow] instead of polling
 * `Clerk.client`. Each emission is a fully committed state.
 */
@Suppress("TooManyFunctions")
internal class ClientStateStore(
  private val clock: () -> Long = System::currentTimeMillis,
  private val cacheStorage: CachedStateStorage = StorageHelperCachedStateStorage,
  private val cacheConfiguration: () -> CachedStateConfiguration? = { null },
  persistenceDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1),
  private val persistDebounceMillis: Long = DEFAULT_PERSIST_DEBOUNCE_MILLIS,
) {

  /** Callbacks the store invokes around commits. */
  internal interface Listener {
    /**
     * Called under the write lock after the session flows changed, in commit order. Must be cheap
     * and must not call back into the store's writers from another thread.
     */
    fun onSessionStateCommitted(previous: Session?, current: Session?) = Unit

    /**
     * Called after the write lock is released. Use [ClientCommit.revision] to discard stale calls.
     */
    fun onClientCommitted(commit: ClientCommit) = Unit

    /** Called after the write lock is released. */
    fun onEnvironmentCommitted(previous: Environment?, current: Environment) = Unit
  }

  /**
   * A committed client write. [revision] increases with every commit, including [reset].
   * [endedSession] is `true` when the commit replaced a current session with none.
   */
  internal data class ClientCommit(
    val client: Client,
    val serverFetchAtMillis: Long,
    val revision: Long,
    val endedSession: Boolean,
  )

  /** An atomic read of the client together with the server fetch time it was recorded with. */
  internal data class ClientSnapshot(val client: Client?, val serverFetchAtMillis: Long?)

  private data class State(
    /** Backs `Clerk.client`; stays [Client] after [reset] to match the former `lateinit` field. */
    val client: Client? = null,
    /** Backs [clientFlow]; `null` before the first client and after [reset]. */
    val publishedClient: Client? = null,
    val serverFetchAtMillis: Long? = null,
    val environment: Environment? = null,
    val revision: Long = 0,
  )

  @Volatile internal var listener: Listener? = null

  private val lock = Any()
  @Volatile private var state = State()

  /** Newest server `Date` applied; guarded by [lock]. */
  private var serverTimeWatermarkMillis: Long? = null
  /** Recently observed response bodies and their server fetch times; guarded by [lock]. */
  private val recentResponses = ArrayDeque<Pair<Client, Long>>()

  private val _client = MutableStateFlow<Client?>(null)
  private val _sessions = MutableStateFlow<List<Session>>(emptyList())
  private val _session = MutableStateFlow<Session?>(null)
  private val _user = MutableStateFlow<User?>(null)
  private val _organizationLogoUrl = MutableStateFlow<String?>(null)
  private val _multiSessionModeIsEnabled = MutableStateFlow(false)

  /** The committed client; `null` before the first client and after [reset]. */
  val clientFlow: StateFlow<Client?> = _client.asStateFlow()

  /** All sessions on the committed client. */
  val sessionsFlow: StateFlow<List<Session>> = _sessions.asStateFlow()

  /** The session matching the committed client's `lastActiveSessionId`, regardless of status. */
  val sessionFlow: StateFlow<Session?> = _session.asStateFlow()

  /** The user of [sessionFlow]'s session. */
  val userFlow: StateFlow<User?> = _user.asStateFlow()

  val organizationLogoUrlFlow: StateFlow<String?> = _organizationLogoUrl.asStateFlow()

  val multiSessionModeIsEnabledFlow: StateFlow<Boolean> = _multiSessionModeIsEnabled.asStateFlow()

  /** The client backing `Clerk.client`, or `null` if no client was ever committed. */
  val client: Client?
    get() = state.client

  val environment: Environment?
    get() = state.environment

  val serverFetchAtMillis: Long?
    get() = state.serverFetchAtMillis

  fun clientSnapshot(): ClientSnapshot = state.let {
    ClientSnapshot(it.publishedClient, it.serverFetchAtMillis)
  }

  // region Client writes

  /**
   * Applies a client decoded by the network layer from a server response.
   *
   * @param serverDateMillis the response's `Date` header, or `null` if it had none. A response
   *   without a server date is applied without ordering.
   * @return `false` if the response was fetched before already-applied state and was dropped.
   */
  fun applyClientResponse(client: Client, serverDateMillis: Long?): Boolean {
    val commit =
      synchronized(lock) {
        if (serverDateMillis != null) recordLocked(client, serverDateMillis)
        if (isStaleLocked(serverDateMillis)) {
          logDroppedStaleClient(client, serverDateMillis)
          return false
        }
        commitClientLocked(
          client = client,
          serverFetchAtMillis = serverDateMillis ?: clock(),
          serverDateMillis = serverDateMillis,
        )
      }
    dispatch(commit)
    return true
  }

  /**
   * Records the server fetch time of a response body without applying it, so that a caller that
   * later applies the same body through [applyClient] is ordered against other responses.
   */
  fun recordClientResponse(client: Client, serverDateMillis: Long) {
    synchronized(lock) { recordLocked(client, serverDateMillis) }
  }

  /**
   * Applies a client supplied by SDK code: a response body re-applied by its caller or a local
   * mutation of the current client.
   *
   * If [client] equals a response body recorded with a server fetch time, it is ordered by that
   * time and dropped when newer state was already applied. Anything else always applies.
   *
   * @return `false` if the client was dropped as stale.
   */
  fun applyClient(client: Client): Boolean {
    val commit =
      synchronized(lock) {
        val serverDateMillis = recentResponses.lastOrNull { it.first == client }?.second
        if (isStaleLocked(serverDateMillis)) {
          logDroppedStaleClient(client, serverDateMillis)
          return false
        }
        commitClientLocked(
          client = client,
          serverFetchAtMillis = serverDateMillis ?: localFetchAtLocked(client),
          serverDateMillis = serverDateMillis,
        )
      }
    dispatch(commit)
    return true
  }

  /**
   * Atomically derives a new client from the current one, so a local mutation cannot overwrite a
   * commit that landed between reading and writing the client. [transform] runs under the write
   * lock; it must be cheap and return `null` to leave the state unchanged.
   *
   * @return `true` if a client was committed.
   */
  fun mutateClient(transform: (Client) -> Client?): Boolean {
    val commit =
      synchronized(lock) {
        state.client?.let(transform)?.let { next ->
          commitClientLocked(
            client = next,
            serverFetchAtMillis = localFetchAtLocked(next),
            serverDateMillis = null,
          )
        }
      } ?: return false
    dispatch(commit)
    return true
  }

  /**
   * Replaces the client unconditionally with an explicit fetch time. Used for authoritative sources
   * that do their own ordering: cached-state hydration and shared-session sync.
   */
  fun replaceClient(client: Client, serverFetchAtMillis: Long) {
    val commit =
      synchronized(lock) {
        commitClientLocked(
          client = client,
          serverFetchAtMillis = serverFetchAtMillis,
          serverDateMillis = null,
        )
      }
    dispatch(commit)
  }

  /**
   * Clears the session, sessions, and user flows without changing the client, for example when the
   * server reports that the current session can no longer issue tokens.
   */
  fun clearSessionState() {
    synchronized(lock) {
      val previousSession = _session.value
      _sessions.value = emptyList()
      _session.value = null
      _user.value = null
      notifySessionStateLocked(previousSession, null)
    }
  }

  /** Re-derives the session, sessions, and user flows from the current client. */
  fun recomputeSessionState() {
    synchronized(lock) { publishSessionStateLocked(state.client) }
  }

  // endregion

  // region Environment writes

  fun applyEnvironment(environment: Environment) {
    val previous =
      synchronized(lock) {
        val previous = state.environment
        state = state.copy(environment = environment)
        _organizationLogoUrl.value = environment.displayConfig.logoImageUrl
        _multiSessionModeIsEnabled.value = !environment.authConfig.singleSessionMode
        previous
      }
    listener?.onEnvironmentCommitted(previous, environment)
    schedulePersist()
  }

  /**
   * Sets the environment without notifying listeners, updating derived flows, or persisting. Kept
   * for the `Clerk.environment` setter, which never had side effects.
   */
  fun overwriteEnvironment(environment: Environment?) {
    synchronized(lock) { state = state.copy(environment = environment) }
  }

  // endregion

  /**
   * Clears all in-memory state and the persisted offline cache.
   *
   * The client backing `Clerk.client` becomes an empty [Client] and [clientFlow] emits `null`. The
   * environment-derived flows keep their last values, as before this store existed.
   */
  fun reset() {
    synchronized(lock) {
      val previousSession = _session.value
      serverTimeWatermarkMillis = null
      recentResponses.clear()
      state =
        State(
          client = Client(),
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

  /** Writes the latest complete snapshot to the offline cache on the calling thread. */
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

  /**
   * Reads and writes under [persistLock] so a write can never land after [clearPersistedState] with
   * state read before it.
   */
  private fun writeLatestSnapshot() {
    synchronized(persistLock) {
      val snapshot = persistableState() ?: return
      runCatching { cacheStorage.save(snapshot) }
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

  /**
   * A complete client/environment snapshot lets a future cold start restore the same readiness
   * contract as a network initialization; caching only the environment would leave
   * `Clerk.isInitialized` false and host applications stuck behind their loading gates.
   */
  private fun persistableState(): CachedClerkState? {
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

  private fun recordLocked(client: Client, serverDateMillis: Long) {
    recentResponses.addLast(client to serverDateMillis)
    while (recentResponses.size > MAX_RECENT_RESPONSES) {
      recentResponses.removeFirst()
    }
  }

  /** A local write keeps the current fetch time if it changes nothing, otherwise it is "now". */
  private fun localFetchAtLocked(client: Client): Long {
    val resolved = client.withResolvedActiveSession(previousSession = _session.value)
    return if (state.publishedClient == resolved) state.serverFetchAtMillis ?: clock() else clock()
  }

  private fun isStaleLocked(serverDateMillis: Long?): Boolean {
    val watermark = serverTimeWatermarkMillis
    return serverDateMillis != null && watermark != null && serverDateMillis < watermark
  }

  private fun commitClientLocked(
    client: Client,
    serverFetchAtMillis: Long,
    serverDateMillis: Long?,
  ): ClientCommit {
    val resolvedClient = client.withResolvedActiveSession(previousSession = _session.value)
    if (serverDateMillis != null) {
      serverTimeWatermarkMillis =
        maxOf(serverTimeWatermarkMillis ?: serverDateMillis, serverDateMillis)
    }
    val revision = state.revision + 1
    state =
      state.copy(
        client = resolvedClient,
        publishedClient = resolvedClient,
        serverFetchAtMillis = serverFetchAtMillis,
        revision = revision,
      )
    _client.value = resolvedClient
    val previousSession = _session.value
    val currentSession = publishSessionStateLocked(resolvedClient)
    return ClientCommit(
      client = resolvedClient,
      serverFetchAtMillis = serverFetchAtMillis,
      revision = revision,
      endedSession = previousSession != null && currentSession == null,
    )
  }

  /** Publishes the session flows for [client] and returns the current session. */
  private fun publishSessionStateLocked(client: Client?): Session? {
    val previousSession = _session.value
    val sessions = client?.sessions.orEmpty()
    val currentSession = sessions.firstOrNull { it.id == client?.lastActiveSessionId }

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
      listener?.onSessionStateCommitted(previous, current)
    } catch (e: Exception) {
      ClerkLog.e("${e.message}")
    }
  }

  private fun dispatch(commit: ClientCommit) {
    listener?.onClientCommitted(commit)
    if (commit.endedSession) {
      // Persist a sign-out before returning so a process death cannot leave the signed-in
      // snapshot to be restored on the next cold start.
      flushPersistence()
    } else {
      schedulePersist()
    }
  }

  private fun logDroppedStaleClient(client: Client, serverDateMillis: Long?) {
    ClerkLog.d(
      "Dropped stale client ${client.id} fetched at $serverDateMillis; " +
        "newer state was fetched at $serverTimeWatermarkMillis"
    )
  }

  // endregion

  internal companion object {
    const val DEFAULT_PERSIST_DEBOUNCE_MILLIS = 100L
    private const val MAX_RECENT_RESPONSES = 16
  }
}

/**
 * Resolves the session that should be current for this client: the server's `lastActiveSessionId`
 * if hydrated, otherwise the previously current session if still present, otherwise the sole
 * session when the server omitted an active id.
 */
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

/** The configuration a persisted snapshot belongs to. */
internal data class CachedStateConfiguration(val publishableKey: String, val baseUrl: String)

/** Storage for the offline client/environment snapshot. */
internal interface CachedStateStorage {
  fun save(state: CachedClerkState)

  fun clear()
}

/** Encrypted [StorageHelper]-backed offline cache. */
internal object StorageHelperCachedStateStorage : CachedStateStorage {
  override fun save(state: CachedClerkState) {
    val encoded = ClerkApi.json.encodeToString(CachedClerkState.serializer(), state)
    StorageHelper.saveValue(StorageKey.CACHED_CLERK_STATE, encoded)
  }

  override fun clear() {
    StorageHelper.deleteValue(StorageKey.CACHED_CLERK_STATE)
  }
}
