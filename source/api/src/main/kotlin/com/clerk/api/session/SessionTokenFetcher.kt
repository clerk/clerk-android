package com.clerk.api.session

import com.clerk.api.Clerk
import com.clerk.api.Constants
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.token.TokenResource
import com.clerk.api.network.serialization.ClerkResult
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel

/**
 * Internal service for fetching and managing session tokens.
 *
 * This class handles the retrieval of authentication tokens for sessions, including caching,
 * concurrent request deduplication, and token validation. It ensures that multiple concurrent
 * requests for the same token are deduplicated and that tokens are cached appropriately to reduce
 * network requests.
 *
 * The fetcher uses a concurrent task map to prevent multiple simultaneous requests for the same
 * token, improving performance and reducing server load. The map is shared across instances,
 * because callers construct a fetcher per call.
 *
 * @param jwtManager The JWT manager used for token parsing and validation
 * @param backoff Failure backoff shared across instances, keyed by token cache key
 * @param fetchScope Scope that owns the shared request, so one caller walking away cannot cancel
 *   the request every other caller is waiting on
 */
internal class SessionTokenFetcher(
  private val jwtManager: JWTManager = JWTManagerImpl(),
  private val backoff: SessionTokenBackoff = SessionTokenBackoff.shared,
  private val fetchScope: CoroutineScope = activeFetchScope(),
) {
  /** An in-flight request together with the invalidation generation it was started under. */
  private class TokenTask(
    val generation: SessionTokensCache.Generation,
    val deferred: Deferred<TokenResource?>,
  )

  companion object {
    /** Task-key prefix that keeps a forced request from joining a plain one already in flight. */
    private const val FORCED_TASK_KEY_PREFIX = "force:"

    private val sessionInvalidationErrorCodes =
      setOf(
        "session_revoked",
        "session_expired",
        "session_ended",
        "session_removed",
        "session_replaced",
        "session_not_found",
        "session_invalid",
        "authentication_invalid",
      )

    /**
     * Map of task keys to deferred token fetch tasks for request deduplication.
     *
     * Shared state: a per-instance map would never deduplicate anything because every call site
     * builds a new fetcher.
     */
    private val tokenTasks = ConcurrentHashMap<String, TokenTask>()

    private fun newFetchScope() = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val scopeLock = Any()

    /**
     * Monotonic counter bumped whenever the configuration is tectonically replaced (via
     * [com.clerk.api.Clerk.reset] / switchConfiguration). A request captures it at admission and
     * rechecks it before caching or returning its result: a token minted under a configuration that
     * has since been replaced is discarded rather than leaked into the new one.
     */
    private val configurationEpoch = AtomicLong()

    /** Scope that owns the shared request; replaced wholesale on [resetForNewConfiguration]. */
    private var sharedFetchScope = newFetchScope()

    /** The current scope, read under the swap lock so a caller never sees a torn value. */
    private fun activeFetchScope(): CoroutineScope = synchronized(scopeLock) { sharedFetchScope }

    /**
     * Bumps the configuration epoch, drops the dedupe map, installs a fresh scope, then cancels the
     * old one.
     *
     * Called from [com.clerk.api.Clerk.reset] as the final teardown step, after the Retrofit
     * service is reset, so any request that could still reach the old service was admitted before
     * this bump and therefore captured the old epoch: its result is discarded rather than cached.
     * The epoch bump and the scope swap share [scopeLock], so a caller that observes the fresh
     * scope also observes the new epoch. The fresh scope is published before the old one is
     * cancelled, so a concurrent reader of [activeFetchScope] never lands on the cancelled scope.
     */
    internal fun resetForNewConfiguration() {
      tokenTasks.clear()
      val previous =
        synchronized(scopeLock) {
          configurationEpoch.incrementAndGet()
          val old = sharedFetchScope
          sharedFetchScope = newFetchScope()
          old
        }
      previous.cancel()
    }

    /** The scope in-flight requests run on. Exposed for tests to observe the reset. */
    internal val currentFetchScope: CoroutineScope
      get() = activeFetchScope()

    /**
     * The JWT this session last held, which the minter mints the next token from.
     *
     * The cache is read with the same key the response will be written to, so a templated token
     * can never seed the default request; the current client's persisted `lastActiveToken` covers
     * a cold start. The fallback is looked up on the live client by session id, never on a
     * [Session] instance the caller may have retained from before an org switch, whose token would
     * carry the old scope past the same-org edge check. A blank or undecodable candidate is skipped
     * rather than sent, and does not block the one behind it.
     */
    private fun mintingSeed(sessionId: String, cacheKey: String): String? {
      val currentSessionToken =
        if (Clerk.clientInitialized) {
          Clerk.client.sessions.firstOrNull { it.id == sessionId }?.lastActiveToken?.jwt
        } else {
          null
        }
      return listOfNotNull(SessionTokensCache.getToken(cacheKey)?.jwt, currentSessionToken)
        .firstOrNull { it.isNotBlank() && JwtFreshness.isReadable(it) }
    }
  }

  /**
   * Retrieves a token for the specified session with the given options.
   *
   * This method implements request deduplication to ensure that multiple concurrent requests for
   * the same token are handled efficiently. It first checks if a task for the same token is already
   * in progress and waits for that result instead of starting a new request.
   *
   * Note: Pending sessions cannot issue tokens. If the session status is
   * [Session.SessionStatus.PENDING], this method will log a warning and return null.
   *
   * @param session The session to get the token for
   * @param options Options for token retrieval including template and caching behavior
   * @return The token resource, or null if the token could not be retrieved or session is pending
   */
  suspend fun getToken(
    session: Session,
    options: GetTokenOptions = GetTokenOptions(),
  ): TokenResource? =
    when {
      session.status == Session.SessionStatus.PENDING -> {
        ClerkLog.w(
          "Cannot fetch token for session ${session.id}: session is in pending state. " +
            "The user has tasks to complete before the session can be activated."
        )
        null
      }
      else -> fetchTokenWithDeduplication(session, options)
    }

  private suspend fun fetchTokenWithDeduplication(
    session: Session,
    options: GetTokenOptions,
  ): TokenResource? {
    val cacheKey = session.tokenCacheKey(options.template)
    val taskKey = if (options.skipCache) "$FORCED_TASK_KEY_PREFIX$cacheKey" else cacheKey
    val generation = SessionTokensCache.generation(session.id)
    val epoch = configurationEpoch.get()
    ClerkLog.d(
      "Fetching token for session ${session.id} with options: $options and cache key: $cacheKey"
    )

    // Awaiting rather than running the request inline: a caller that is cancelled while waiting
    // detaches from the shared request instead of cancelling it for everyone else.
    return sharedTask(taskKey, session, options, generation, epoch).await()
  }

  /**
   * Returns the in-flight request for [taskKey], or starts one.
   *
   * A caller only joins a running task whose generation still matches the generation read inside
   * the atomic map update, not the caller's earlier-captured one: a task begun before a sign-out or
   * org switch would return a token carrying the previous state, so a caller that reaches the map
   * after the switch must start its own request. The caller's earlier-captured [generation] still
   * fences the task it installs (its cache write and backoff record).
   *
   * A task whose deferred is cancelling is never joinable: it is checked with `isActive`, not
   * `!isCompleted`, so a cancelling task left in the map by an in-progress reset cannot hand a
   * fresh caller a [CancellationException].
   */
  private fun sharedTask(
    taskKey: String,
    session: Session,
    options: GetTokenOptions,
    generation: SessionTokensCache.Generation,
    epoch: Long,
  ): Deferred<TokenResource?> {
    val started =
      fetchScope.async(start = CoroutineStart.LAZY) {
        fetchToken(session, options, generation, epoch)
      }
    val newTask = TokenTask(generation, started)
    val winner =
      tokenTasks.compute(taskKey) { _, existing ->
        val current = SessionTokensCache.generation(session.id)
        val joinable =
          existing != null && existing.generation == current && existing.deferred.isActive
        if (joinable) existing else newTask
      }!!

    if (winner !== newTask) {
      started.cancel()
      return winner.deferred
    }

    started.invokeOnCompletion { tokenTasks.remove(taskKey, newTask) }
    started.start()
    return started
  }

  /**
   * Internal method to fetch a token from cache or network.
   *
   * This method first checks the token cache (unless skipCache is true) and validates any cached
   * token. If no valid cached token exists, it makes a network request to fetch a new token and
   * caches the result. Requests are suppressed while the cache key is inside the backoff window
   * opened by an earlier failure, unless the caller asked to skip the cache: that is a deliberate
   * recovery attempt, and refusing it for up to a minute is worse than the extra request.
   *
   * @param session The session to fetch the token for
   * @param options Options controlling the fetch behavior
   * @return The token resource, or null if the fetch failed
   */
  private suspend fun fetchToken(
    session: Session,
    options: GetTokenOptions,
    generation: SessionTokensCache.Generation,
    epoch: Long,
  ): TokenResource? {
    val cacheKey = session.tokenCacheKey(options.template)
    val cachedToken = if (options.skipCache) null else validCachedToken(session, cacheKey, options)

    return when {
      cachedToken != null -> cachedToken
      !options.skipCache && backoff.isBackingOff(cacheKey) -> {
        ClerkLog.w("Not requesting a token for $cacheKey yet: backing off after repeated failures")
        null
      }
      else -> requestToken(session, options, cacheKey, generation, epoch)
    }
  }

  private fun validCachedToken(
    session: Session,
    cacheKey: String,
    options: GetTokenOptions,
  ): TokenResource? {
    val cachedToken = SessionTokensCache.getToken(cacheKey) ?: return null
    val isValid = isTokenValid(cachedToken, options.expirationBuffer)
    if (isValid) {
      ClerkLog.d("Cached token is still valid for session ${session.id}")
    } else {
      ClerkLog.d("Cached token is expired for session ${session.id}")
    }
    return cachedToken.takeIf { isValid }
  }

  private suspend fun requestToken(
    session: Session,
    options: GetTokenOptions,
    cacheKey: String,
    generation: SessionTokensCache.Generation,
    epoch: Long,
  ): TokenResource? {
    return try {
      val tokensRequest = postTokens(session, options, cacheKey)
      // The POST may already be out, but a token minted under a configuration that has since been
      // torn down must never be cached, recorded, or returned into the configuration that replaced
      // it. Cheap atomic read, no lock.
      if (configurationEpoch.get() != epoch) {
        ClerkLog.w("Discarding token for $cacheKey: the configuration was replaced mid-request")
        null
      } else {
        when (tokensRequest) {
          is ClerkResult.Success -> {
            recordOutcome(cacheKey, session.id, generation, succeeded = true)
            SessionTokensCache.setTokenIfCurrent(
              cacheKey = cacheKey,
              sessionId = session.id,
              generation = generation,
              token = tokensRequest.value,
            )
            tokensRequest.value
          }
          is ClerkResult.Failure -> {
            recordOutcome(cacheKey, session.id, generation, succeeded = false)
            handleSessionInvalidationOnFailure(session, tokensRequest)
            null
          }
        }
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      if (configurationEpoch.get() == epoch) {
        recordOutcome(cacheKey, session.id, generation, succeeded = false)
      }
      ClerkLog.e("Failed to fetch token: ${e.message}")
      null
    }
  }

  /**
   * Records the fetch outcome into the failure backoff, unless the session was invalidated after
   * this request started. A stale failure would open a window against the new state, and a stale
   * success would clear a window the new state legitimately opened. The generation check and the
   * backoff mutation run under the cache's write lock, the same lock invalidation bumps the
   * generation and clears the window under, so the pair is atomic against a concurrent switch.
   */
  private fun recordOutcome(
    cacheKey: String,
    sessionId: String,
    generation: SessionTokensCache.Generation,
    succeeded: Boolean,
  ) {
    SessionTokensCache.recordBackoffOutcomeIfCurrent(
      cacheKey = cacheKey,
      sessionId = sessionId,
      generation = generation,
      succeeded = succeeded,
      backoff = backoff,
    )
  }

  /**
   * Posts the tokens request.
   *
   * Minter fields are only attached for instances the environment reports as minter-enabled, and
   * never on the templated route, which the edge does not mint. With nothing to attach the request
   * stays exactly the bodyless POST the SDK has always sent.
   */
  private suspend fun postTokens(
    session: Session,
    options: GetTokenOptions,
    cacheKey: String,
  ): ClerkResult<TokenResource, ClerkErrorResponse> {
    if (options.template != null) {
      return ClerkApi.session.tokens(userId = session.id, templateType = options.template)
    }

    val minterIsEnabled = Clerk.sessionMinterIsEnabled
    val previousSessionToken = if (minterIsEnabled) mintingSeed(session.id, cacheKey) else null
    val forceOrigin = if (minterIsEnabled && options.skipCache) true else null

    return if (previousSessionToken == null && forceOrigin == null) {
      ClerkApi.session.tokens(sessionId = session.id)
    } else {
      ClerkApi.session.mintTokens(
        sessionId = session.id,
        previousSessionToken = previousSessionToken,
        forceOrigin = forceOrigin,
      )
    }
  }

  private fun handleSessionInvalidationOnFailure(
    session: Session,
    failure: ClerkResult.Failure<ClerkErrorResponse>,
  ) {
    val shouldClearSessionState =
      failure.error
        ?.errors
        .orEmpty()
        .mapNotNull { it.code?.lowercase() }
        .any { it in sessionInvalidationErrorCodes }

    if (!shouldClearSessionState) return

    if (Clerk.session?.id == session.id) {
      ClerkLog.w(
        "Session ${session.id} can no longer issue tokens. Clearing local session and user state."
      )
      Clerk.clearSessionAndUserState()
    }
  }

  /**
   * Validates whether a token is still valid based on its expiration time.
   *
   * This method parses the JWT token to extract the expiration time and compares it against the
   * current time plus a buffer to determine if the token is still valid for use.
   *
   * @param token The token resource to validate
   * @param bufferSeconds The buffer time in seconds before expiration to consider invalid
   * @return true if the token is valid, false otherwise
   */
  private fun isTokenValid(token: TokenResource, bufferSeconds: Long): Boolean {
    return try {
      val expiresAt = jwtManager.createFromString(token.jwt).expiresAt
      val currentTime = System.currentTimeMillis()
      val bufferMs = bufferSeconds * Constants.Config.DEFAULT_EXPIRATION_BUFFER

      expiresAt?.let {
        val timeUntilExpiry = it.time - currentTime
        val isValid = timeUntilExpiry > bufferMs
        isValid
      } == true
    } catch (e: Exception) {
      ClerkLog.w("Failed to parse JWT expiration: ${e.message}")
      false
    }
  }
}

/**
 * Options for configuring session token retrieval behavior.
 *
 * This data class allows customization of how tokens are fetched, including template usage, cache
 * behavior, and expiration buffer settings.
 *
 * @property template Optional template name for custom token generation
 * @property skipCache Whether to bypass the token cache and always fetch from network
 * @property expirationBuffer Buffer time in seconds before token expiration to consider it invalid
 */
data class GetTokenOptions(
  /** Optional template name for custom token generation */
  val template: String? = null,

  /** Whether to bypass the token cache and always fetch from network */
  val skipCache: Boolean = false,

  /** Buffer time in seconds before token expiration to consider it invalid */
  val expirationBuffer: Long = 10, // seconds
)

/**
 * Extension function to generate a cache key for session tokens.
 *
 * This function creates a unique cache key based on the session ID and optional template name. This
 * ensures that tokens for different templates are cached separately.
 *
 * @param template Optional template name to include in the cache key
 * @return A unique cache key string for the session and template combination
 */
internal fun Session.tokenCacheKey(template: String?): String = template?.let { "$id-$it" } ?: id
