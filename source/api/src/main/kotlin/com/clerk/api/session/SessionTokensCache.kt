package com.clerk.api.session

import com.clerk.api.network.model.token.TokenResource
import java.util.concurrent.ConcurrentHashMap

internal object SessionTokensCache {
  private val cache = ConcurrentHashMap<String, TokenResource>()

  internal data class StoreResult(
    val canonicalToken: TokenResource,
    val didChangeCanonicalToken: Boolean,
  )

  internal fun getToken(cacheKey: String): TokenResource? = cache[cacheKey]

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

  internal fun removeToken(cacheKey: String): TokenResource? = cache.remove(cacheKey)

  internal fun removeTokens(sessionId: String) {
    cache.keys.removeAll { it.belongsToSession(sessionId) }
  }

  internal fun clear() = cache.clear()

  internal val size: Int
    get() = cache.size

  internal fun containsKey(cacheKey: String): Boolean = cache.containsKey(cacheKey)
}

internal fun String.belongsToSession(sessionId: String): Boolean =
  startsWith("$sessionId-organization-") || startsWith("$sessionId-template-")
