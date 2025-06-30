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

internal class SessionTokenFetcher(private val jwtManager: JWTManager = JWTManagerImpl()) {
  private val tokenTasks = ConcurrentHashMap<String, Deferred<TokenResource?>>()

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

data class SessionGetTokenOptions(
  val template: String? = null,
  val skipCache: Boolean = false,
  val expirationBuffer: Long = 60, // seconds
)

fun Session.tokenCacheKey(template: String?): String = template?.let { "$id-$it" } ?: id
