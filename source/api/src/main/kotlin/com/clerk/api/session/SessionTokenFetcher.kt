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
import kotlinx.coroutines.Deferred

/**
 * Internal service for fetching and managing session tokens.
 *
 * This class handles the retrieval of authentication tokens for sessions, including caching,
 * concurrent request deduplication, and token validation. It ensures that multiple concurrent
 * requests for the same token are deduplicated and that tokens are cached appropriately to reduce
 * network requests.
 *
 * The fetcher uses a concurrent task map to prevent multiple simultaneous requests for the same
 * token, improving performance and reducing server load.
 *
 * @param jwtManager The JWT manager used for token parsing and validation
 */
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
  )

  /** Map of cache keys to deferred token fetch tasks for request deduplication */
  private val tokenTasks = ConcurrentHashMap<String, Deferred<TokenResource?>>()

  /**
   * Releases deduplicated waiters and removes requests registered by the previous Clerk runtime.
   */
  internal fun reset() {
    tokenTasks.values.forEach { it.cancel() }
    tokenTasks.clear()
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
    val context = makeFetchContext(session, options.template)
    ClerkLog.d(
      "Fetching token for session ${context.session.id} with options: $options and cache key: " +
        context.cacheKey
    )

    if (options.skipCache) {
      return fetchToken(context, options)
    }

    return tokenTasks[context.cacheKey]?.await()
      ?: run {
        val deferred = CompletableDeferred<TokenResource?>()
        val existingTask = tokenTasks.putIfAbsent(context.cacheKey, deferred)

        existingTask?.await()
          ?: try {
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

  private fun makeFetchContext(session: Session, template: String?): FetchContext {
    val currentSession =
      Clerk.clientFlow.value?.sessions?.firstOrNull { it.id == session.id } ?: session
    return FetchContext(
      session = currentSession,
      cacheKey = currentSession.tokenCacheKey(template),
      sessionMinterEnabled = Clerk.environment?.authConfig?.sessionMinter == true,
    )
  }

  /**
   * Internal method to fetch a token from cache or network.
   *
   * This method first checks the token cache (unless skipCache is true) and validates any cached
   * token. If no valid cached token exists, it makes a network request to fetch a new token and
   * caches the result.
   *
   * @param session The session to fetch the token for
   * @param options Options controlling the fetch behavior
   * @return The token resource, or null if the fetch failed
   */
  private suspend fun fetchToken(context: FetchContext, options: GetTokenOptions): TokenResource? {
    val session = context.session
    val cacheKey = context.cacheKey

    if (options.template == null) {
      session.lastActiveToken
        ?.takeIf {
          TokenFreshness.matches(it, session.id, session.lastActiveOrganizationId)
        }
        ?.let { SessionTokensCache.hydrate(cacheKey, it) }
    }

    // Check cache first (unless skipped)
    if (!options.skipCache) {
      SessionTokensCache.getToken(cacheKey)?.let { token ->
        ClerkLog.d("Found cached token for session ${session.id}")
        if (isTokenValid(token, options.expirationBuffer)) {
          ClerkLog.d("Cached token is still valid for session ${session.id}")
          return token
        } else {
          ClerkLog.d("Cached token is expired for session ${session.id}")
        }
      }
    }

    // Fetch from network
    return try {
      val tokensRequest =
        if (options.template != null) {
          ClerkApi.session.tokens(session.id, options.template)
        } else {
          val cachedToken = SessionTokensCache.getToken(cacheKey)
          val previousToken =
            cachedToken?.let {
              TokenFreshness.pickFreshest(
                existing = session.lastActiveToken,
                incoming = it,
              )
            } ?: session.lastActiveToken
          ClerkApi.session.tokens(
            sessionId = session.id,
            organizationId = session.lastActiveOrganizationId.orEmpty(),
            token = previousToken?.jwt.takeIf { context.sessionMinterEnabled },
            forceOrigin = "true".takeIf { context.sessionMinterEnabled && options.skipCache },
          )
        }

      when (tokensRequest) {
        is ClerkResult.Success -> {
          SessionTokensCache.storeIfFresher(cacheKey, tokensRequest.value)
          tokensRequest.value
        }
        is ClerkResult.Failure -> {
          handleSessionInvalidationOnFailure(session, tokensRequest)
          null
        }
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      ClerkLog.e("Failed to fetch token: ${e.message}")
      null
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
 * This function creates a unique cache key based on the session ID and either its active
 * organization or the optional template name.
 *
 * @param template Optional template name to include in the cache key
 * @return A unique cache key string for the session and template combination
 */
internal fun Session.tokenCacheKey(template: String?): String =
  template?.let { "$id-template-$it" } ?: "$id-organization-${lastActiveOrganizationId.orEmpty()}"
