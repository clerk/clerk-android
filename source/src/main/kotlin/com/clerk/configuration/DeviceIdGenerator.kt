package com.clerk.configuration

import androidx.annotation.VisibleForTesting
import com.clerk.log.ClerkLog
import com.clerk.storage.StorageHelper
import com.clerk.storage.StorageKey
import java.util.UUID

/**
 * Internal utility for generating and managing unique device identifiers.
 *
 * This object provides thread-safe generation and caching of device IDs that persist across
 * application sessions. The device ID is used for device attestation and analytics purposes.
 *
 * The device ID is generated once per device installation and stored in persistent storage.
 * If storage is unavailable, a temporary device ID is generated that will be replaced with
 * a persistent one when storage becomes available.
 */
internal object DeviceIdGenerator {
  @Volatile private var cachedDeviceId: String? = null

  /**
   * Initializes the device ID generator and loads or creates a device ID.
   *
   * This method should be called during app initialization to ensure the device ID is
   * available when needed. It uses double-checked locking to ensure thread safety and
   * prevent multiple device ID generation.
   *
   * If a device ID exists in storage, it will be loaded. Otherwise, a new UUID will be
   * generated and saved to storage. If storage operations fail, the device ID will still
   * be cached in memory for the current session.
   */
  // Call this during app initialization
  fun initialize() {
    if (cachedDeviceId == null) {
      synchronized(this) {
        if (cachedDeviceId == null) {
          try {
            val storedId = StorageHelper.loadValue(StorageKey.DEVICE_ID)
            cachedDeviceId =
              if (storedId.isNullOrEmpty()) {
                UUID.randomUUID().toString().also { newId ->
                  try {
                    StorageHelper.saveValue(StorageKey.DEVICE_ID, newId)
                    ClerkLog.d("Generated and saved new device ID")
                  } catch (e: Exception) {
                    ClerkLog.w("Failed to save device ID to storage: ${e.message}")
                    // Continue with generated ID even if save fails
                  }
                }
              } else {
                ClerkLog.d("Loaded existing device ID from storage")
                storedId
              }
          } catch (e: Exception) {
            // If storage fails, generate a temporary device ID
            ClerkLog.w("Storage not available, generating temporary device ID: ${e.message}")
            cachedDeviceId = UUID.randomUUID().toString()
          }
        }
      }
    }
  }

  /**
   * Retrieves the current device ID.
   *
   * This method assumes that [initialize] has been called previously. If the device ID
   * is not available, it will attempt to initialize it automatically.
   *
   * @return The device ID string
   * @throws IllegalStateException if device ID initialization fails
   */
  fun getDeviceId(): String {
    return cachedDeviceId
      ?: run {
        // If not initialized yet, try to initialize now
        initialize()
        cachedDeviceId ?: error("Device ID initialization failed")
      }
  }

  /**
   * Retrieves the device ID, generating one if necessary.
   *
   * This method provides lazy initialization and can be called safely from anywhere
   * without requiring prior initialization. If no device ID exists, it will generate
   * a temporary one and attempt to persist it asynchronously when storage becomes available.
   *
   * @return The device ID string, either from cache, storage, or newly generated
   */
  // Lazy initialization version that can be called safely from anywhere
  fun getOrGenerateDeviceId(): String {
    return cachedDeviceId
      ?: run {
        synchronized(this) {
          cachedDeviceId
            ?: run {
              val deviceId = UUID.randomUUID().toString()
              cachedDeviceId = deviceId
              ClerkLog.d("Generated temporary device ID (will persist when storage is ready)")

              // Try to persist asynchronously if storage becomes available
              tryPersistDeviceIdAsync(deviceId)

              deviceId
            }
        }
      }
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
    try {
      // Try to save the device ID if storage is available
      val existingId = StorageHelper.loadValue(StorageKey.DEVICE_ID)
      if (existingId.isNullOrEmpty()) {
        StorageHelper.saveValue(StorageKey.DEVICE_ID, deviceId)
        ClerkLog.d("Persisted device ID to storage")
      } else if (existingId != deviceId) {
        // Use the stored ID instead of the generated one
        cachedDeviceId = existingId
        ClerkLog.d("Updated cached device ID to match stored ID")
      }
    } catch (e: Exception) {
      ClerkLog.w("Could not persist device ID: ${e.message}")
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
    cachedDeviceId = null
  }
}
