package com.clerk.session

import com.clerk.network.model.token.TokenResource
import java.util.concurrent.ConcurrentHashMap

internal object SessionTokensCache {
  private val cache = ConcurrentHashMap<String, TokenResource>()

  /** Returns a session token for the given cache key. */
  internal fun getToken(cacheKey: String): TokenResource? = cache[cacheKey]

  /** Sets a session token for the given cache key. */
  internal fun setToken(cacheKey: String, token: TokenResource) {
    cache[cacheKey] = token
  }

  /** Removes a session token for the given cache key. */
  internal fun removeToken(cacheKey: String): TokenResource? = cache.remove(cacheKey)

  /** Clears all cached tokens. */
  internal fun clear() = cache.clear()

  /** Returns the number of cached tokens. */
  internal val size: Int
    get() = cache.size

  /** Checks if a token exists for the given cache key. */
  internal fun containsKey(cacheKey: String): Boolean = cache.containsKey(cacheKey)
}
