package com.clerk.session

import com.clerk.Constants
import com.clerk.log.ClerkLog
import com.clerk.network.ClerkApi
import com.clerk.network.model.token.TokenResource
import com.clerk.network.serialization.successOrElse
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Internal service for fetching and managing session tokens.
 *
 * This class handles the retrieval of authentication tokens for sessions, including
 * caching, concurrent request deduplication, and token validation. It ensures that
 * multiple concurrent requests for the same token are deduplicated and that tokens
 * are cached appropriately to reduce network requests.
 *
 * The fetcher uses a concurrent task map to prevent multiple simultaneous requests
 * for the same token, improving performance and reducing server load.
 *
 * @param jwtManager The JWT manager used for token parsing and validation
 */
internal class SessionTokenFetcher(private val jwtManager: JWTManager = JWTManagerImpl()) {
  /** Map of cache keys to deferred token fetch tasks for request deduplication */
  private val tokenTasks = ConcurrentHashMap<String, Deferred<TokenResource?>>()

  /**
   * Retrieves a token for the specified session with the given options.
   *
   * This method implements request deduplication to ensure that multiple concurrent
   * requests for the same token are handled efficiently. It first checks if a task
   * for the same token is already in progress and waits for that result instead of
   * starting a new request.
   *
   * @param session The session to get the token for
   * @param options Options for token retrieval including template and caching behavior
   * @return The token resource, or null if the token could not be retrieved
   */
  suspend fun getToken(
    session: Session,
    options: SessionGetTokenOptions = SessionGetTokenOptions(),
  ): TokenResource? {
    val cacheKey = session.tokenCacheKey(options.template)

    // Fast path: check if task already exists
    tokenTasks[cacheKey]?.let {
      return it.await()
    }

    // Create new task
    return coroutineScope {
      val deferred = async { fetchToken(session, options) }

      // Atomic put-if-absent operation
      val existingTask = tokenTasks.putIfAbsent(cacheKey, deferred)
      if (existingTask != null) {
        // Another coroutine beat us to it, cancel our task and use theirs
        deferred.cancel()
        return@coroutineScope existingTask.await()
      }

      // We're the first, execute our task
      try {
        deferred.await()
      } finally {
        tokenTasks.remove(cacheKey)
      }
    }
  }

  /**
   * Internal method to fetch a token from cache or network.
   *
   * This method first checks the token cache (unless skipCache is true) and validates
   * any cached token. If no valid cached token exists, it makes a network request
   * to fetch a new token and caches the result.
   *
   * @param session The session to fetch the token for
   * @param options Options controlling the fetch behavior
   * @return The token resource, or null if the fetch failed
   */
  private suspend fun fetchToken(
    session: Session,
    options: SessionGetTokenOptions,
  ): TokenResource? {
    val cacheKey = session.tokenCacheKey(options.template)

    // Check cache first (unless skipped)
    if (!options.skipCache) {
      SessionTokensCache.getToken(cacheKey)?.let { token ->
        if (isTokenValid(token, options.expirationBuffer)) {
          return token
        }
      }
    }

    // Fetch from network
    return try {
      val tokensRequest =
        if (options.template != null) {
          ClerkApi.session.tokens(session.id, options.template)
        } else {
          ClerkApi.session.tokens(session.id)
        }

      val result =
        tokensRequest.successOrElse { throw IllegalStateException("Failed to fetch token") }

      SessionTokensCache.setToken(cacheKey, result)
      result
    } catch (e: Exception) {
      ClerkLog.e("Failed to fetch token: ${e.message}")
      null
    }
  }

  /**
   * Validates whether a token is still valid based on its expiration time.
   *
   * This method parses the JWT token to extract the expiration time and compares
   * it against the current time plus a buffer to determine if the token is still
   * valid for use.
   *
   * @param token The token resource to validate
   * @param bufferSeconds The buffer time in seconds before expiration to consider invalid
   * @return true if the token is valid, false otherwise
   */
  private fun isTokenValid(token: TokenResource, bufferSeconds: Long): Boolean {
    return try {
      val expiresAt = jwtManager.createFromString(token.jwt).expiresAt
      expiresAt?.let {
        (it.time - System.currentTimeMillis()) >
          bufferSeconds * Constants.Config.DEFAULT_EXPIRATION_BUFFER
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
 * This data class allows customization of how tokens are fetched, including
 * template usage, cache behavior, and expiration buffer settings.
 *
 * @property template Optional template name for custom token generation
 * @property skipCache Whether to bypass the token cache and always fetch from network
 * @property expirationBuffer Buffer time in seconds before token expiration to consider it invalid
 */
data class SessionGetTokenOptions(
  /** Optional template name for custom token generation */
  val template: String? = null,
  
  /** Whether to bypass the token cache and always fetch from network */
  val skipCache: Boolean = false,
  
  /** Buffer time in seconds before token expiration to consider it invalid */
  val expirationBuffer: Long = 60, // seconds
)

/**
 * Extension function to generate a cache key for session tokens.
 *
 * This function creates a unique cache key based on the session ID and optional
 * template name. This ensures that tokens for different templates are cached
 * separately.
 *
 * @param template Optional template name to include in the cache key
 * @return A unique cache key string for the session and template combination
 */
fun Session.tokenCacheKey(template: String?): String = template?.let { "$id-$it" } ?: id
