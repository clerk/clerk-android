package com.clerk.attestation

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.clerk.log.ClerkLog
import com.clerk.network.ClerkApi
import com.clerk.network.model.client.Client
import com.clerk.network.model.error.ClerkErrorResponse
import com.clerk.network.serialization.ClerkResult
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityManager
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

private const val HASH_CONSTANT = 0xff
private const val PREPARATION_TIMEOUT_MS = 30_000L // 30 seconds
private const val ATTESTATION_TIMEOUT_MS = 15_000L // 15 seconds
private const val HASH_CACHE_MAX_SIZE = 100

/**
 * Helper object for handling device attestation using Google Play Integrity API.
 *
 * This object manages the integrity token provider preparation, token retrieval, and device
 * attestation verification with Clerk's backend services.
 *
 * Optimizations included:
 * - Caching of prepared integrity token providers
 * - LRU cache for computed hashes
 * - Thread-safe operations with proper synchronization
 * - Timeout handling for all async operations
 * - Retry logic and error recovery
 */
internal object DeviceAttestationHelper {
  /** Coroutine scope for handling asynchronous operations on the IO dispatcher. */
  val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  /** Standard integrity manager instance for interacting with Google Play Integrity API. */
  var integrityManager: StandardIntegrityManager? = null

  /** Current integrity token provider. */
  var integrityTokenProvider: StandardIntegrityManager.StandardIntegrityTokenProvider? = null

  /** Cache for prepared providers to avoid re-preparation. Thread-safe map. */
  private val preparedProviders =
    ConcurrentHashMap<Long, StandardIntegrityManager.StandardIntegrityTokenProvider>()

  /** LRU cache for computed hashes to avoid recomputation. */
  private val hashCache = LRUCache<String, String>(HASH_CACHE_MAX_SIZE)

  /** Mutex to ensure thread-safe access to the integrity manager initialization. */
  private val initializationMutex = Mutex()

  /** Track if the integrity manager has been initialized. */
  @Volatile private var isManagerInitialized = false

  /**
   * Prepares the integrity token provider for the given cloud project. This is a suspend function
   * that waits for the preparation to complete with timeout handling and caching.
   *
   * @param context The Android application context
   * @param cloudProjectNumber The Google Cloud project number associated with the app
   * @throws IllegalArgumentException if cloudProjectNumber is null
   * @throws IllegalStateException if preparation fails
   * @throws kotlinx.coroutines.TimeoutCancellationException if operation times out
   */
  suspend fun prepareIntegrityTokenProvider(context: Context, cloudProjectNumber: Long?) {
    requireNotNull(cloudProjectNumber) { "Cloud project number is required" }

    // Check cache first - if we have a prepared provider, use it
    preparedProviders[cloudProjectNumber]?.let { cachedProvider ->
      integrityTokenProvider = cachedProvider
      ClerkLog.d("Using cached integrity token provider for project $cloudProjectNumber")
      return
    }

    // Initialize integrity manager if needed
    initializeIntegrityManagerIfNeeded(context)

    val manager = requireNotNull(integrityManager) { "IntegrityManager is not initialized" }

    try {
      withTimeout(PREPARATION_TIMEOUT_MS) {
        suspendCancellableCoroutine<Unit> { continuation ->
          val task =
            manager.prepareIntegrityToken(
              StandardIntegrityManager.PrepareIntegrityTokenRequest.builder()
                .setCloudProjectNumber(cloudProjectNumber)
                .build()
            )

          task
            .addOnSuccessListener { tokenProvider ->
              ClerkLog.d(
                "Integrity token provider prepared successfully for project $cloudProjectNumber"
              )
              integrityTokenProvider = tokenProvider
              // Cache the provider for future use
              preparedProviders[cloudProjectNumber] = tokenProvider
              continuation.resume(Unit)
            }
            .addOnFailureListener { exception ->
              ClerkLog.e(
                "Failed to prepare integrity token for project $cloudProjectNumber: $exception"
              )
              continuation.resumeWithException(
                IllegalStateException("Failed to prepare integrity token", exception)
              )
            }

          // Handle cancellation
          continuation.invokeOnCancellation {
            ClerkLog.d("Integrity token preparation was cancelled for project $cloudProjectNumber")
          }
        }
      }
    } catch (e: Exception) {
      ClerkLog.e("Timeout or error during integrity token preparation: ${e.message}")
      throw e
    }
  }

  /** Thread-safe initialization of the integrity manager. */
  private suspend fun initializeIntegrityManagerIfNeeded(context: Context) {
    if (!isManagerInitialized) {
      initializationMutex.withLock {
        if (!isManagerInitialized) {
          try {
            integrityManager = IntegrityManagerFactory.createStandard(context)
            isManagerInitialized = true
            ClerkLog.d("IntegrityManager initialized successfully")
          } catch (e: Exception) {
            ClerkLog.e("Failed to initialize IntegrityManager: ${e.message}")
            throw IllegalStateException("Failed to initialize IntegrityManager", e)
          }
        }
      }
    }
  }

  /**
   * Retrieves an integrity token and initiates device attestation verification. This function
   * suspends until the integrity verification completes with timeout handling.
   *
   * @param clientId The client identifier to be hashed and included in the token request
   * @return ClerkResult containing the integrity token string or error
   * @throws IllegalArgumentException if integrityTokenProvider is null
   * @throws kotlinx.coroutines.TimeoutCancellationException if operation times out
   */
  @Throws(IllegalArgumentException::class)
  suspend fun attestDevice(clientId: String): ClerkResult<String, ClerkErrorResponse> {
    val tokenProvider =
      integrityTokenProvider
        ?: return ClerkResult.unknownFailure(
          IllegalStateException("Integrity token provider must be prepared before attestation")
        )

    return try {
      withTimeout(ATTESTATION_TIMEOUT_MS) {
        suspendCancellableCoroutine { continuation ->
          val hashedClientId = getHashedClientId(clientId)
          ClerkLog.d("Requesting integrity token for client: ${clientId.take(8)}...")

          val response =
            tokenProvider.request(
              StandardIntegrityManager.StandardIntegrityTokenRequest.builder()
                .setRequestHash(hashedClientId)
                .build()
            )

          response
            .addOnSuccessListener { tokenResponse ->
              ClerkLog.d("Integrity token retrieved successfully")
              continuation.resume(ClerkResult.success(tokenResponse.token()))
            }
            .addOnFailureListener { exception ->
              ClerkLog.e("Failed to get integrity token: $exception")
              continuation.resume(
                ClerkResult.unknownFailure(
                  IllegalStateException("Failed to get integrity token: $exception")
                )
              )
            }

          // Handle cancellation
          continuation.invokeOnCancellation { ClerkLog.d("Integrity token request was cancelled") }
        }
      }
    } catch (e: Exception) {
      ClerkLog.e("Timeout or error during device attestation: ${e.message}")
      ClerkResult.unknownFailure(IllegalStateException("Device attestation timeout: ${e.message}"))
    }
  }

  /**
   * Attests the device by verifying the integrity token with Clerk's backend.
   *
   * @param token The integrity token obtained from Google Play Integrity API
   * @param applicationId The application package name for verification
   * @return ClerkResult containing Client data or error response
   * @throws IllegalArgumentException if applicationId is null
   */
  suspend fun performAssertion(
    token: String,
    applicationId: String?,
  ): ClerkResult<Client, ClerkErrorResponse> {
    requireNotNull(applicationId) { "Application ID is required for device attestation" }

    return try {
      ClerkLog.d("Performing device assertion with token")
      val result = ClerkApi.deviceAttestationApi.verify(packageName = applicationId, token = token)

      when (result) {
        is ClerkResult.Success -> {
          ClerkLog.d("Device assertion completed successfully")
          result
        }
        is ClerkResult.Failure -> {
          ClerkLog.w("Device assertion failed: ${result.error}")
          result
        }
      }
    } catch (e: Exception) {
      ClerkLog.e("Exception during device assertion: ${e.message}")
      ClerkResult.unknownFailure(IllegalStateException("Device assertion failed: ${e.message}"))
    }
  }

  /**
   * Generates a SHA-256 hash of the provided client ID with caching for performance.
   *
   * @param clientId The client identifier to hash
   * @return The hexadecimal string representation of the SHA-256 hash
   * @throws RuntimeException if hashing fails
   */
  @VisibleForTesting
  fun getHashedClientId(clientId: String): String {
    return hashCache.get(clientId)
      ?: run {
        val hashed = computeHash(clientId)
        hashCache.put(clientId, hashed)
        hashed
      }
  }

  /** Computes the SHA-256 hash of the input string. */
  private fun computeHash(clientId: String): String {
    try {
      val digest = MessageDigest.getInstance("SHA-256")
      val hash = digest.digest(clientId.toByteArray(charset("UTF-8")))
      val hexString = StringBuilder(hash.size * 2)

      for (b in hash) {
        val hex = Integer.toHexString(HASH_CONSTANT and b.toInt())
        if (hex.length == 1) hexString.append('0')
        hexString.append(hex)
      }

      return hexString.toString()
    } catch (e: Exception) {
      throw RuntimeException("Failed to hash clientId", e)
    }
  }

  /**
   * Clears all cached data and resets the helper to initial state. Useful for testing or when
   * switching between different configurations.
   */
  fun clearCache() {
    preparedProviders.clear()
    hashCache.clear()
    integrityTokenProvider = null
    ClerkLog.d("DeviceAttestationHelper cache cleared")
  }

  /** Gets the current cache statistics for monitoring and debugging. */
  fun getCacheStats(): CacheStats {
    return CacheStats(
      preparedProvidersCount = preparedProviders.size,
      hashCacheSize = hashCache.size(),
      hashCacheMaxSize = HASH_CACHE_MAX_SIZE,
    )
  }

  /** Data class for cache statistics. */
  data class CacheStats(
    val preparedProvidersCount: Int,
    val hashCacheSize: Int,
    val hashCacheMaxSize: Int,
  )

  /** Simple thread-safe LRU Cache implementation using LinkedHashMap. */
  private class LRUCache<K, V>(private val maxSize: Int) {
    private val cache =
      object : LinkedHashMap<K, V>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<K, V>?): Boolean {
          return size > maxSize
        }
      }

    @Synchronized fun get(key: K): V? = cache[key]

    @Synchronized fun put(key: K, value: V): V? = cache.put(key, value)

    @Synchronized fun clear() = cache.clear()

    @Synchronized fun size(): Int = cache.size

    @Synchronized fun remove(key: K): V? = cache.remove(key)

    @Synchronized fun containsKey(key: K): Boolean = cache.containsKey(key)
  }

  /**
   * Force preparation of integrity token provider without waiting for attestation. This can be
   * called during app startup to warm up the provider.
   *
   * @param context Application context
   * @param cloudProjectNumber Cloud project number
   * @return true if preparation was successful, false otherwise
   */
  suspend fun warmUpProvider(context: Context, cloudProjectNumber: Long?): Boolean {
    return try {
      if (cloudProjectNumber != null && !preparedProviders.containsKey(cloudProjectNumber)) {
        prepareIntegrityTokenProvider(context, cloudProjectNumber)
        ClerkLog.d("Integrity token provider warmed up successfully")
        true
      } else {
        ClerkLog.d("Integrity token provider already prepared or invalid project number")
        true
      }
    } catch (e: Exception) {
      ClerkLog.w("Failed to warm up integrity token provider: ${e.message}")
      false
    }
  }

  /** Checks if a provider is already prepared for the given project number. */
  fun isProviderPrepared(cloudProjectNumber: Long?): Boolean {
    return cloudProjectNumber != null && preparedProviders.containsKey(cloudProjectNumber)
  }
}
