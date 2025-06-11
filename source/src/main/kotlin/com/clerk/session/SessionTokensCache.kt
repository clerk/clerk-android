package com.clerk.session

import com.clerk.network.model.token.TokenResource
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal object SessionTokensCache {
  private val cache = ConcurrentHashMap<String, TokenResource>()
  private val mutex = Mutex()

  /**
   * Returns a session token for the given cache key.
   *
   * @param cacheKey The cache key to retrieve the session token for.
   * @return The session token, or null if no token is found for the given cache key.
   */
  internal suspend fun getToken(cacheKey: String): TokenResource? {
    return mutex.withLock { cache[cacheKey] }
  }

  /**
   * Sets a session token for the given cache key.
   *
   * @param cacheKey The cache key to set the session token for.
   * @param token The session token to set.
   */
  internal suspend fun setToken(cacheKey: String, token: TokenResource) {
    mutex.withLock { cache[cacheKey] = token }
  }
}
