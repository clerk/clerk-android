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
 * Server responses are ordered by a [ResponseOrder]: their `Date` header, then the order in which
 * they arrived. The store keeps the newest order it has applied and drops a response ordered before
 * it, so a slow response that started before a newer one cannot overwrite the newer state, even
 * when both carry the same one-second `Date`. Only server-issued times feed this watermark; local
 * clock readings never do, so device clock skew cannot make the store reject fresh responses.
 *
 * Every commit (server response, local mutation, authoritative replacement, [reset]) increments
 * [revision]. A caller that re-applies a decoded response body after awaiting it (for example the
 * result of `Client.get()`) captures [revision] before starting the request and applies through
 * [applyClientIfUnchangedSince], so the body is dropped if anything was committed while it was in
 * flight. Local mutations and authoritative replacements (cache hydration, shared-session sync)
 * always apply.
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

  /**
   * The position of a server response in the store's ordering: its `Date` header, then its arrival
   * sequence for responses that share a `Date` second. Obtained from [observeResponse] when the
   * response arrives, before its body is read.
   */
  internal data class ResponseOrder(val serverDateMillis: Long, val arrival: Long) :
    Comparable<ResponseOrder> {
    override fun compareTo(other: ResponseOrder): Int =
      compareValuesBy(this, other, ResponseOrder::serverDateMillis, ResponseOrder::arrival)
  }

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

  /** Newest response order applied; guarded by [lock]. */
  private var watermark: ResponseOrder? = null
  /** Arrival sequence handed to the next observed response; guarded by [lock]. */
  private var nextArrival = 0L

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

  /** Increments with every commit, including [reset] and [clearSessionState]. */
  val revision: Long
    get() = state.revision

  fun clientSnapshot(): ClientSnapshot = state.let {
    ClientSnapshot(it.publishedClient, it.serverFetchAtMillis)
  }

  // region Client writes

  /**
   * Assigns the next arrival position to a response that just arrived, before its body is read.
   *
   * @param serverDateMillis the response's `Date` header, or `null` if it had none.
   * @return the response's order, or `null` for a response without a server date.
   */
  fun observeResponse(serverDateMillis: Long?): ResponseOrder? {
    serverDateMillis ?: return null
    return synchronized(lock) {
      ResponseOrder(serverDateMillis, arrival = nextArrival++)
    }
  }

  /**
   * Applies a client decoded by the network layer from a server response.
   *
   * @param order the response's order from [observeResponse], or `null` if it had no server date. A
   *   response without one is applied without ordering.
   * @return `false` if the response was ordered before already-applied state and was dropped.
   */
  fun applyClientResponse(client: Client, order: ResponseOrder?): Boolean {
    val commit =
      synchronized(lock) {
        if (isStaleLocked(order)) {
          logDroppedStaleClient(client, order)
          return false
        }
        if (order != null) watermark = maxOf(watermark ?: order, order)
        commitClientLocked(
          client = client,
          serverFetchAtMillis = order?.serverDateMillis ?: clock(),
        )
      }
    dispatch(commit)
    return true
  }

  /** Observes and applies a response in one step; see [observeResponse]. */
  fun applyClientResponse(client: Client, serverDateMillis: Long?): Boolean =
    applyClientResponse(client, observeResponse(serverDateMillis))

  /**
   * Applies a local mutation of the client, or a mutation response its caller applies itself.
   * Always applies, and bumps [revision] so a fetch awaited since before it is dropped.
   */
  fun applyClient(client: Client) {
    val commit = synchronized(lock) { commitLocalLocked(client) }
    dispatch(commit)
  }

  /**
   * Applies a response body its caller awaited, only if nothing was committed since [revision]
   * returned [expectedRevision]. The check and the commit happen atomically under the write lock.
   *
   * @return `true` if [client] was committed.
   */
  fun applyClientIfUnchangedSince(expectedRevision: Long, client: Client): Boolean {
    val commit =
      synchronized(lock) {
        if (state.revision != expectedRevision) {
          ClerkLog.d(
            "Dropped client ${client.id} fetched at revision $expectedRevision; " +
              "state is at revision ${state.revision}"
          )
          return false
        }
        commitLocalLocked(client)
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
        state.client?.let(transform)?.let { next -> commitLocalLocked(next) }
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
        commitClientLocked(client = client, serverFetchAtMillis = serverFetchAtMillis)
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
      // A response awaited since before this clear must not re-publish the session.
      state = state.copy(revision = state.revision + 1)
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
      watermark = null
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
    // Clearing the cache outside the write lock is safe: any snapshot write that starts after this
    // point reads the reset state, which has no published client and is never persisted, and a
    // write that read the old state holds persistLock, so this clear waits for it and runs last.
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
      // Runs on the persistence scope; a failure must be logged, never thrown into the scope.
      runCatching { persistableState()?.let(cacheStorage::save) }
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

  /** A local write keeps the current fetch time if it changes nothing, otherwise it is "now". */
  private fun localFetchAtLocked(client: Client): Long {
    val resolved = client.withResolvedActiveSession(previousSession = _session.value)
    return if (state.publishedClient == resolved) state.serverFetchAtMillis ?: clock() else clock()
  }

  private fun isStaleLocked(order: ResponseOrder?): Boolean {
    val watermark = watermark
    return order != null && watermark != null && order < watermark
  }

  private fun commitLocalLocked(client: Client): ClientCommit =
    commitClientLocked(client = client, serverFetchAtMillis = localFetchAtLocked(client))

  private fun commitClientLocked(client: Client, serverFetchAtMillis: Long): ClientCommit {
    val previousSession = state.client?.currentSession()
    val resolvedClient = client.withResolvedActiveSession(previousSession = _session.value)
    val revision = state.revision + 1
    state =
      state.copy(
        client = resolvedClient,
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
    )
  }

  /** Publishes the session flows for [client] and returns the current session. */
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

  private fun logDroppedStaleClient(client: Client, order: ResponseOrder?) {
    ClerkLog.d("Dropped stale client ${client.id} ordered at $order; newer state is at $watermark")
  }

  // endregion

  internal companion object {
    const val DEFAULT_PERSIST_DEBOUNCE_MILLIS = 100L
  }
}

/** The session matching [Client.lastActiveSessionId], regardless of status. */
private fun Client.currentSession(): Session? = sessions.firstOrNull {
  it.id == lastActiveSessionId
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
