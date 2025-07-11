package com.clerk.configuration

import androidx.annotation.VisibleForTesting
import com.clerk.log.ClerkLog
import com.clerk.storage.StorageHelper
import com.clerk.storage.StorageKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

/**
 * Internal utility for generating and managing unique device identifiers with improved performance.
 *
 * This object provides thread-safe generation and caching of device IDs that persist across
 * application sessions. The device ID is used for device attestation and analytics purposes.
 *
 * Key improvements:
 * - Reduced synchronization scope using atomic operations
 * - Asynchronous initialization to avoid blocking
 * - Lazy loading with fallback mechanisms
 * - Better error handling and recovery
 *
 * The device ID is generated once per device installation and stored in persistent storage.
 * If storage is unavailable, a temporary device ID is generated that will be replaced with
 * a persistent one when storage becomes available.
 */
internal object DeviceIdGenerator {
  
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val deviceIdState = AtomicReference<String?>(null)
  private val initializationMutex = Mutex()
  private var initializationTask: Deferred<String>? = null

  /**
   * Asynchronously initializes the device ID generator and loads or creates a device ID.
   *
   * This method uses atomic operations and reduces synchronization scope for better performance.
   * Multiple concurrent calls will share the same initialization task.
   *
   * @return Deferred<String> that will complete with the device ID
   */
  suspend fun initializeAsync(): Deferred<String> {
    // Fast path: if already initialized, return cached value
    deviceIdState.get()?.let { 
      return scope.async { it }
    }

    // Check if initialization is already in progress
    initializationTask?.let { return it }

    return initializationMutex.withLock {
      // Double check after acquiring lock
      deviceIdState.get()?.let { 
        return@withLock scope.async { it }
      }

      initializationTask?.let { return@withLock it }

      // Start new initialization task
      val task = scope.async {
        try {
          initializeDeviceIdInternal()
        } catch (e: Exception) {
          ClerkLog.w("Device ID initialization failed, generating temporary ID: ${e.message}")
          generateTemporaryDeviceId()
        }
      }
      
      initializationTask = task
      task
    }
  }

  /**
   * Initializes the device ID generator and loads or creates a device ID.
   *
   * This method should be called during app initialization to ensure the device ID is
   * available when needed. It now uses improved atomic operations for better performance.
   */
  fun initialize() {
    // Fast path: if already initialized, return
    if (deviceIdState.get() != null) return

    synchronized(this) {
      // Double check after acquiring lock
      if (deviceIdState.get() != null) return

      try {
        val deviceId = initializeDeviceIdBlocking()
        deviceIdState.set(deviceId)
        ClerkLog.d("Device ID initialized: ${deviceId.take(8)}...")
      } catch (e: Exception) {
        // If storage fails, generate a temporary device ID
        ClerkLog.w("Storage not available, generating temporary device ID: ${e.message}")
        val tempId = UUID.randomUUID().toString()
        deviceIdState.set(tempId)
        
        // Try to persist asynchronously
        tryPersistDeviceIdAsync(tempId)
      }
    }
  }

  /**
   * Retrieves the current device ID.
   *
   * This method assumes that initialization has been called previously. If the device ID
   * is not available, it will attempt to initialize it automatically.
   *
   * @return The device ID string
   * @throws IllegalStateException if device ID initialization fails
   */
  fun getDeviceId(): String {
    return deviceIdState.get() ?: run {
      // Auto-initialize if not already done
      initialize()
      deviceIdState.get() ?: error("Device ID initialization failed")
    }
  }

  /**
   * Retrieves the device ID, generating one if necessary.
   *
   * This method provides lazy initialization and can be called safely from anywhere
   * without requiring prior initialization. Uses atomic operations for thread safety.
   *
   * @return The device ID string, either from cache, storage, or newly generated
   */
  fun getOrGenerateDeviceId(): String {
    return deviceIdState.get() ?: run {
      // Use compareAndSet for atomic lazy initialization
      val newDeviceId = UUID.randomUUID().toString()
      if (deviceIdState.compareAndSet(null, newDeviceId)) {
        ClerkLog.d("Generated temporary device ID (will persist when storage is ready)")
        // Try to persist asynchronously
        tryPersistDeviceIdAsync(newDeviceId)
        newDeviceId
      } else {
        // Another thread beat us to it, use their value
        deviceIdState.get() ?: newDeviceId
      }
    }
  }

  /**
   * Internal method to initialize device ID with proper storage access
   */
  private suspend fun initializeDeviceIdInternal(): String {
    if (!StorageHelper.isInitialized()) {
      ClerkLog.w("Storage not initialized for device ID generation")
      return generateTemporaryDeviceId()
    }

    return withContext(Dispatchers.IO) {
      val storedId = StorageHelper.loadValue(StorageKey.DEVICE_ID)
      
      val deviceId = if (storedId.isNullOrEmpty()) {
        val newId = UUID.randomUUID().toString()
        try {
          StorageHelper.saveValue(StorageKey.DEVICE_ID, newId)
          ClerkLog.d("Generated and saved new device ID")
          newId
        } catch (e: Exception) {
          ClerkLog.w("Failed to save device ID to storage: ${e.message}")
          // Continue with generated ID even if save fails
          newId
        }
      } else {
        ClerkLog.d("Loaded existing device ID from storage")
        storedId
      }
      
      // Update atomic reference
      deviceIdState.set(deviceId)
      deviceId
    }
  }

  /**
   * Blocking version of device ID initialization for backward compatibility
   */
  private fun initializeDeviceIdBlocking(): String {
    val storedId = try {
      StorageHelper.loadValue(StorageKey.DEVICE_ID)
    } catch (e: Exception) {
      ClerkLog.w("Failed to load device ID from storage: ${e.message}")
      null
    }
    
    return if (storedId.isNullOrEmpty()) {
      val newId = UUID.randomUUID().toString()
      try {
        StorageHelper.saveValue(StorageKey.DEVICE_ID, newId)
        ClerkLog.d("Generated and saved new device ID")
        newId
      } catch (e: Exception) {
        ClerkLog.w("Failed to save device ID to storage: ${e.message}")
        // Continue with generated ID even if save fails
        newId
      }
    } else {
      ClerkLog.d("Loaded existing device ID from storage")
      storedId
    }
  }

  /**
   * Generate a temporary device ID when storage is not available
   */
  private fun generateTemporaryDeviceId(): String {
    val tempId = UUID.randomUUID().toString()
    deviceIdState.set(tempId)
    ClerkLog.d("Generated temporary device ID")
    return tempId
  }

  /**
   * Attempts to persist the device ID to storage asynchronously.
   *
   * This method tries to save the device ID to persistent storage. If a different device ID
   * already exists in storage, the cached device ID will be updated to match the stored one.
   * This prevents device ID conflicts when multiple instances try to generate device IDs.
   *
   * @param deviceId The device ID to persist
   */
  private fun tryPersistDeviceIdAsync(deviceId: String) {
    scope.async {
      try {
        if (!StorageHelper.isInitialized()) {
          ClerkLog.d("Storage not ready, device ID will be persisted later")
          return@async
        }

        withContext(Dispatchers.IO) {
          // Check for existing ID to avoid conflicts
          val existingId = StorageHelper.loadValue(StorageKey.DEVICE_ID)
          if (existingId.isNullOrEmpty()) {
            StorageHelper.saveValue(StorageKey.DEVICE_ID, deviceId)
            ClerkLog.d("Persisted device ID to storage")
          } else if (existingId != deviceId) {
            // Use the stored ID instead of the generated one
            deviceIdState.set(existingId)
            ClerkLog.d("Updated cached device ID to match stored ID")
          }
        }
      } catch (e: Exception) {
        ClerkLog.w("Could not persist device ID: ${e.message}")
      }
    }
  }

  /**
   * Clears the cached device ID for testing purposes.
   *
   * This method is only available for testing and should not be used in production code.
   * It resets the internal state to allow for clean test scenarios.
   */
  @VisibleForTesting
  internal fun clearCache() {
    deviceIdState.set(null)
    initializationTask?.cancel()
    initializationTask = null
  }

  /**
   * Check if device ID is currently cached (for testing/debugging)
   */
  @VisibleForTesting
  internal fun isCached(): Boolean = deviceIdState.get() != null
}
