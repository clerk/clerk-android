package com.clerk.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.clerk.Constants.Storage.CLERK_PREFERENCES_FILE_NAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Helper class to manage secure storage of data with improved performance and async initialization.
 * SharedPreferences are used to store data, all keys are held in the [StorageKey] object.
 * 
 * Key improvements:
 * - Asynchronous initialization to avoid main thread blocking
 * - In-memory cache for frequently accessed values
 * - Thread-safe operations with minimal synchronization
 */
internal object StorageHelper {

  private var secureStorage: SharedPreferences? = null
  private val isInitialized = AtomicBoolean(false)
  private val initializationMutex = Mutex()
  
  // In-memory cache for frequently accessed values
  private val cache = ConcurrentHashMap<String, String>()
  private val cacheTimestamps = ConcurrentHashMap<String, Long>()
  private val cacheValidityDuration = 60_000L // 1 minute

  /**
   * Asynchronously initializes the secure storage to avoid blocking the main thread.
   * This method is thread-safe and will only initialize once.
   */
  suspend fun initializeAsync(context: Context) {
    if (isInitialized.get()) return
    
    initializationMutex.withLock {
      if (!isInitialized.get()) {
        withContext(Dispatchers.IO) {
          secureStorage = context.getSharedPreferences(CLERK_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)
          isInitialized.set(true)
        }
      }
    }
  }

  /**
   * Synchronously initializes the secure storage. Used for backward compatibility.
   * We do this synchronously because some legacy code needs immediate storage access.
   */
  fun initialize(context: Context) {
    if (isInitialized.get()) return
    
    synchronized(this) {
      if (!isInitialized.get()) {
        secureStorage = context.getSharedPreferences(CLERK_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)
        isInitialized.set(true)
      }
    }
  }

  /** 
   * Save value of string type to storage with caching
   */
  internal suspend fun saveValue(key: StorageKey, value: String) {
    if (value.isEmpty()) return
    
    ensureInitialized()
    
    withContext(Dispatchers.IO) {
      secureStorage?.edit(commit = true) { putString(key.name, value) }
      // Update cache
      cache[key.name] = value
      cacheTimestamps[key.name] = System.currentTimeMillis()
    }
  }

  /** 
   * Save value synchronously for backward compatibility
   */
  internal fun saveValueSync(key: StorageKey, value: String) {
    if (value.isEmpty()) return
    
    secureStorage?.edit(commit = true) { putString(key.name, value) }
    // Update cache
    cache[key.name] = value
    cacheTimestamps[key.name] = System.currentTimeMillis()
  }

  /** 
   * Load value of string type from storage with caching
   */
  internal suspend fun loadValue(key: StorageKey): String? {
    // Check cache first
    getCachedValue(key.name)?.let { return it }
    
    ensureInitialized()
    
    return withContext(Dispatchers.IO) {
      val value = secureStorage?.getString(key.name, null)
      // Cache the value if it exists
      value?.let {
        cache[key.name] = it
        cacheTimestamps[key.name] = System.currentTimeMillis()
      }
      value
    }
  }

  /** 
   * Load value synchronously for backward compatibility
   */
  internal fun loadValueSync(key: StorageKey): String? {
    // Check cache first
    getCachedValue(key.name)?.let { return it }
    
    val value = secureStorage?.getString(key.name, null)
    // Cache the value if it exists
    value?.let {
      cache[key.name] = it
      cacheTimestamps[key.name] = System.currentTimeMillis()
    }
    return value
  }

  /** 
   * Delete value of string type from storage
   */
  internal suspend fun deleteValue(name: String) {
    ensureInitialized()
    
    withContext(Dispatchers.IO) {
      secureStorage?.edit { remove(name) }
    }
    // Remove from cache
    cache.remove(name)
    cacheTimestamps.remove(name)
  }

  /**
   * Get cached value if it's still valid
   */
  private fun getCachedValue(key: String): String? {
    val timestamp = cacheTimestamps[key] ?: return null
    return if (System.currentTimeMillis() - timestamp < cacheValidityDuration) {
      cache[key]
    } else {
      // Remove expired cache entry
      cache.remove(key)
      cacheTimestamps.remove(key)
      null
    }
  }

  /**
   * Ensure storage is initialized, throw if not
   */
  private fun ensureInitialized() {
    if (!isInitialized.get()) {
      throw IllegalStateException("StorageHelper not initialized. Call initialize() or initializeAsync() first.")
    }
  }

  /**
   * Clear all cached values (useful for testing)
   */
  internal fun clearCache() {
    cache.clear()
    cacheTimestamps.clear()
  }

  /**
   * Check if storage is initialized
   */
  internal fun isInitialized(): Boolean = isInitialized.get()
}

internal enum class StorageKey(val key: String) {
  DEVICE_TOKEN("device_token"),
  DEVICE_ID("device_id"),
}

// Extension functions to maintain backward compatibility
internal fun StorageHelper.saveValue(key: StorageKey, value: String) = saveValueSync(key, value)
internal fun StorageHelper.loadValue(key: StorageKey): String? = loadValueSync(key)
