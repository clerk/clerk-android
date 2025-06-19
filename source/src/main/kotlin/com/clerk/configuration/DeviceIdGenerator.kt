package com.clerk.configuration

import androidx.annotation.VisibleForTesting
import com.clerk.log.ClerkLog
import com.clerk.storage.StorageHelper
import com.clerk.storage.StorageKey
import java.util.UUID

internal object DeviceIdGenerator {
  @Volatile private var cachedDeviceId: String? = null

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

  fun getDeviceId(): String {
    return cachedDeviceId
      ?: run {
        // If not initialized yet, try to initialize now
        initialize()
        cachedDeviceId ?: error("Device ID initialization failed")
      }
  }

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

  @VisibleForTesting
  internal fun clearCache() {
    cachedDeviceId = null
  }
}
