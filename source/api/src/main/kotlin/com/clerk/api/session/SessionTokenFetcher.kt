package com.clerk.api.session

import com.clerk.api.Clerk
import com.clerk.api.Constants
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.token.TokenResource
import com.clerk.api.network.serialization.ClerkResult
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred

internal class SessionTokenFetcher(private val jwtManager: JWTManager = JWTManagerImpl()) {
  internal companion object {
    internal val shared: SessionTokenFetcher by lazy { SessionTokenFetcher() }

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
  }

  private data class FetchContext(
    val session: Session,
    val cacheKey: String,
    val sessionMinterEnabled: Boolean,
    val runtimeGeneration: Long,
    val sessionGeneration: Long,
  )

  internal data class AuthorizationTokenSelection(
    val token: TokenResource?,
    val fallbackFactorVerificationAge: List<Int>?,
  )

  private val tokenTasks = ConcurrentHashMap<String, CompletableDeferred<TokenResource?>>()
  private val runtimeLock = Any()
  private var runtimeGeneration = 0L
  private val sessionGenerations = mutableMapOf<String, Long>()

  /**
   * Releases deduplicated waiters with a null result and removes requests registered by the
   * previous Clerk runtime.
   */
  internal fun reset() {
    val tasksToRelease =
      synchronized(runtimeLock) {
        runtimeGeneration += 1
        sessionGenerations.clear()
        tokenTasks.values.toList().also { tokenTasks.clear() }
      }
    tasksToRelease.forEach { it.complete(null) }
  }

  /**
   * Invalidates pre-reverification tokens and fences requests already in flight for this session.
   */
  internal fun invalidateSession(sessionId: String) {
    val tasksToRelease =
      synchronized(runtimeLock) {
        sessionGenerations[sessionId] = (sessionGenerations[sessionId] ?: 0L) + 1
        SessionTokensCache.removeTokens(sessionId)
        tokenTasks.keys
          .filter { it.belongsToSession(sessionId) }
          .mapNotNull { tokenTasks.remove(it) }
      }
    tasksToRelease.forEach { it.complete(null) }
  }

  /** Selects an authorization token using the same snapshot eligibility rule as [getToken]. */
  internal fun authorizationTokenSelection(
    session: Session,
    nowMillis: Long = System.currentTimeMillis(),
  ): AuthorizationTokenSelection =
    synchronized(runtimeLock) {
      val requiresNewerOrigin = (sessionGenerations[session.id] ?: 0L) > 0L
      val cached =
        SessionTokensCache.getToken(session.tokenCacheKey(null))?.takeIf {
          TokenFreshness.matches(it, session.id, session.lastActiveOrganizationId)
        }
      val snapshot =
        TokenFreshness.eligibleSnapshot(
          session = session,
          cached = cached,
          requiresNewerOrigin = requiresNewerOrigin,
        )
      val token =
        if (snapshot == null) {
          cached
        } else {
          TokenFreshness.pickFreshest(
            existing = cached,
            incoming = snapshot,
            nowMillis = nowMillis,
            tieBreaker = TokenFreshness.TieBreaker.EXISTING,
          )
        }
      AuthorizationTokenSelection(
        token = token,
        // After invalidation, only an eligible token can supply verification ages.
        fallbackFactorVerificationAge =
          session.factorVerificationAge.takeUnless { requiresNewerOrigin },
      )
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
  ): TokenResource? {
    val context =
      synchronized(runtimeLock) {
        val currentSession =
          Clerk.clientFlow.value?.sessions?.firstOrNull { it.id == session.id } ?: session
        FetchContext(
          session = currentSession,
          cacheKey = currentSession.tokenCacheKey(options.template),
          sessionMinterEnabled = Clerk.environment?.authConfig?.sessionMinter == true,
          runtimeGeneration = runtimeGeneration,
          sessionGeneration = sessionGenerations[session.id] ?: 0L,
        )
      }
    return when {
      context.session.status == Session.SessionStatus.PENDING -> {
        ClerkLog.w(
          "Cannot fetch token for session ${context.session.id}: session is in pending state. " +
            "The user has tasks to complete before the session can be activated."
        )
        null
      }
      else -> fetchTokenWithDeduplication(context, options)
    }
  }

  private suspend fun fetchTokenWithDeduplication(
    context: FetchContext,
    options: GetTokenOptions,
  ): TokenResource? {
    ClerkLog.d(
      "Fetching token for session ${context.session.id} with options: $options and cache key: " +
        context.cacheKey
    )

    return if (options.skipCache) {
      fetchToken(context, options)
    } else {
      val deferred = CompletableDeferred<TokenResource?>()
      val existingTask =
        synchronized(runtimeLock) {
          if (!isCurrentRuntime(context)) return null
          tokenTasks.putIfAbsent(context.cacheKey, deferred)
        }
      if (existingTask != null) {
        // Invalidation can happen after completion but before this waiter resumes.
        existingTask.await()?.takeIf { isCurrentRuntime(context) }
      } else {
        try {
          fetchToken(context, options).also { deferred.complete(it) }
        } catch (e: CancellationException) {
          deferred.cancel(e)
          throw e
        } catch (t: Throwable) {
          deferred.completeExceptionally(t)
          throw t
        } finally {
          tokenTasks.remove(context.cacheKey, deferred)
        }
      }
    }
  }

  private fun isCurrentRuntime(context: FetchContext): Boolean =
    synchronized(runtimeLock) {
      context.runtimeGeneration == runtimeGeneration &&
        context.sessionGeneration == (sessionGenerations[context.session.id] ?: 0L)
    }

  private suspend fun fetchToken(context: FetchContext, options: GetTokenOptions): TokenResource? {
    return if (!isCurrentRuntime(context)) {
      null
    } else {
      // After reverification, snapshots must be newer than a token fetched in this generation.
      if (options.template == null) {
        synchronized(runtimeLock) {
          if (isCurrentRuntime(context)) {
            TokenFreshness.eligibleSnapshot(
                session = context.session,
                cached = SessionTokensCache.getToken(context.cacheKey),
                requiresNewerOrigin = context.sessionGeneration > 0L,
              )
              ?.let { SessionTokensCache.hydrate(context.cacheKey, it) }
          }
        }
      }

      val validCachedToken =
        if (options.skipCache) {
          null
        } else {
          SessionTokensCache.getToken(context.cacheKey)?.takeIf { token ->
            ClerkLog.d("Found cached token for session ${context.session.id}")
            isTokenValid(token, options.expirationBuffer).also { isValid ->
              val cacheStatus = if (isValid) "still valid" else "expired"
              ClerkLog.d("Cached token is $cacheStatus for session ${context.session.id}")
            }
          }
        }

      validCachedToken?.takeIf { isCurrentRuntime(context) }
        ?: if (!isCurrentRuntime(context)) {
          null
        } else {
          try {
            reconcileTokenResponse(context, requestToken(context, options))
          } catch (e: CancellationException) {
            throw e
          } catch (e: Exception) {
            ClerkLog.e("Failed to fetch token: ${e.message}")
            null
          }
        }
    }
  }

  private suspend fun requestToken(
    context: FetchContext,
    options: GetTokenOptions,
  ): ClerkResult<TokenResource, ClerkErrorResponse> {
    val session = context.session
    return if (options.template != null) {
      ClerkApi.session.tokens(session.id, options.template)
    } else {
      val cachedToken = SessionTokensCache.getToken(context.cacheKey)
      val previousToken =
        if (context.sessionGeneration > 0L) {
          cachedToken ?: session.lastActiveToken
        } else {
          cachedToken?.let {
            TokenFreshness.pickFreshest(existing = session.lastActiveToken, incoming = it)
          } ?: session.lastActiveToken
        }
      ClerkApi.session.tokens(
        sessionId = session.id,
        organizationId = session.lastActiveOrganizationId.orEmpty(),
        token = previousToken?.jwt.takeIf { context.sessionMinterEnabled },
        forceOrigin =
          "true"
            .takeIf {
              context.sessionMinterEnabled &&
                (options.skipCache || (context.sessionGeneration > 0L && cachedToken == null))
            },
      )
    }
  }

  private fun reconcileTokenResponse(
    context: FetchContext,
    tokensRequest: ClerkResult<TokenResource, ClerkErrorResponse>,
  ): TokenResource? =
    synchronized(runtimeLock) {
      if (!isCurrentRuntime(context)) return@synchronized null

      when (tokensRequest) {
        is ClerkResult.Success -> {
          SessionTokensCache.storeIfFresher(context.cacheKey, tokensRequest.value)
          // Match Clerk JS: each forced refresh returns its own mint while the shared cache stays
          // monotonic when responses complete out of order.
          tokensRequest.value
        }
        is ClerkResult.Failure -> {
          val invalidSession =
            tokensRequest.error?.errors.orEmpty().any {
              it.code?.lowercase() in sessionInvalidationErrorCodes
            }
          if (invalidSession && Clerk.session?.id == context.session.id) {
            ClerkLog.w(
              "Session ${context.session.id} can no longer issue tokens. Clearing local session and user state."
            )
            Clerk.clearSessionAndUserState()
          }
          null
        }
      }
    }

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

internal fun Session.tokenCacheKey(template: String?): String =
  template?.let { "$id-template-$it" } ?: "$id-organization-${lastActiveOrganizationId.orEmpty()}"
