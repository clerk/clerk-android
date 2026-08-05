package com.clerk.api.session

import com.clerk.api.log.ClerkLog
import com.clerk.api.network.model.token.TokenResource
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

internal object SessionTokensCache {
  private val cache = ConcurrentHashMap<String, TokenResource>()
  private val sessionGenerations = ConcurrentHashMap<String, Long>()
  private val globalGeneration = AtomicLong()
  private val writeLock = Any()

  /**
   * Invalidation counter for one session, captured before a request goes out and rechecked before
   * its response is cached.
   */
  internal data class Generation(val global: Long, val session: Long)

  /** Returns the current invalidation counter for [sessionId]. */
  internal fun generation(sessionId: String): Generation =
    Generation(global = globalGeneration.get(), session = sessionGenerations[sessionId] ?: 0L)

  /** Returns a session token for the given cache key. */
  internal fun getToken(cacheKey: String): TokenResource? = cache[cacheKey]

  /**
   * Sets a session token for the given cache key.
   *
   * Writes are monotonic: a token whose claims were assembled before the cached one's is dropped.
   * Responses can arrive out of order, and a cached token also seeds the next mint, so an older JWT
   * overwriting a newer one would chain backwards.
   */
  internal fun setToken(cacheKey: String, token: TokenResource) {
    synchronized(writeLock) { cache[cacheKey] = freshest(cache[cacheKey], token, cacheKey) }
  }

  /**
   * Sets a session token only if the session was not invalidated since [generation] was captured.
   *
   * Sign-out and org switches clear the cache while a request is already in flight; without this
   * fence that response lands afterwards and repopulates the entry that was just dropped.
   */
  internal fun setTokenIfCurrent(
    cacheKey: String,
    sessionId: String,
    generation: Generation,
    token: TokenResource,
  ) {
    synchronized(writeLock) {
      if (generation != generation(sessionId)) {
        ClerkLog.d("Discarding token for $cacheKey: the cache was invalidated while in flight")
        return
      }
      cache[cacheKey] = freshest(cache[cacheKey], token, cacheKey)
    }
  }

  /** Removes a session token for the given cache key. */
  internal fun removeToken(cacheKey: String): TokenResource? = cache.remove(cacheKey)

  /**
   * Removes the default and every templated token cached for the given session, and lets its next
   * request through immediately by resetting the failure schedule for the same keys.
   *
   * The generation bump and the backoff reset share this object's write lock so that a token
   * request recording its outcome through [recordBackoffOutcomeIfCurrent] cannot interleave between
   * them.
   */
  internal fun removeTokensForSession(sessionId: String) {
    synchronized(writeLock) {
      sessionGenerations[sessionId] = (sessionGenerations[sessionId] ?: 0L) + 1L
      cache.keys.removeAll { it == sessionId || it.startsWith("$sessionId-") }
      SessionTokenBackoff.shared.clearForSession(sessionId)
    }
  }

  /** Clears all cached tokens and every failure schedule. */
  internal fun clear() {
    synchronized(writeLock) {
      globalGeneration.incrementAndGet()
      sessionGenerations.clear()
      cache.clear()
      SessionTokenBackoff.shared.clear()
    }
  }

  /**
   * Records a token request's outcome into [backoff], but only while the session's generation still
   * matches the one captured when the request started.
   *
   * The check and the mutation run under the same write lock invalidation uses, so a stale outcome
   * can never open a window against the post-switch state, and a stale success can never clear a
   * window the post-switch state legitimately opened.
   */
  internal fun recordBackoffOutcomeIfCurrent(
    cacheKey: String,
    sessionId: String,
    generation: Generation,
    succeeded: Boolean,
    backoff: SessionTokenBackoff,
  ) {
    synchronized(writeLock) {
      if (generation != generation(sessionId)) return
      if (succeeded) backoff.recordSuccess(cacheKey) else backoff.recordFailure(cacheKey)
    }
  }

  /** Returns the number of cached tokens. */
  internal val size: Int
    get() = cache.size

  /** Checks if a token exists for the given cache key. */
  internal fun containsKey(cacheKey: String): Boolean = cache.containsKey(cacheKey)

  private fun freshest(
    existing: TokenResource?,
    incoming: TokenResource,
    cacheKey: String,
  ): TokenResource {
    if (existing == null || JwtFreshness.incomingWins(existing.jwt, incoming.jwt)) return incoming
    ClerkLog.d("Keeping the fresher cached session token for cache key $cacheKey")
    return existing
  }
}
