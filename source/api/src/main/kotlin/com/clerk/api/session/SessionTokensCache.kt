package com.clerk.api.session

import com.clerk.api.network.model.token.TokenResource
import java.util.concurrent.ConcurrentHashMap

internal object SessionTokensCache {
  private val cache = ConcurrentHashMap<String, TokenResource>()

  internal data class StoreResult(
    val canonicalToken: TokenResource,
    val didChangeCanonicalToken: Boolean,
  )

  /** Returns a session token for the given cache key. */
  internal fun getToken(cacheKey: String): TokenResource? = cache[cacheKey]

  /** Sets a session token for the given cache key. */
  internal fun setToken(cacheKey: String, token: TokenResource) {
    cache[cacheKey] = token
  }

  /** Reconciles a session snapshot without replacing an equally fresh canonical token. */
  internal fun hydrate(cacheKey: String, token: TokenResource) {
    cache.compute(cacheKey) { _, existing ->
      TokenFreshness.pickFreshest(
        existing = existing,
        incoming = token,
        tieBreaker = TokenFreshness.TieBreaker.EXISTING,
      )
    }
  }

  /** Atomically stores [token] unless the cache already contains a fresher token. */
  internal fun storeIfFresher(
    cacheKey: String,
    token: TokenResource,
    nowMillis: Long = System.currentTimeMillis(),
  ): StoreResult {
    var didChangeCanonicalToken = false
    val canonicalToken =
      checkNotNull(
        cache.compute(cacheKey) { _, existing ->
          TokenFreshness.pickFreshest(existing, token, nowMillis).also { canonical ->
            didChangeCanonicalToken = existing?.jwt != canonical.jwt
          }
        }
      )
    return StoreResult(canonicalToken, didChangeCanonicalToken)
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
