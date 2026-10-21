package com.clerk.api.configuration

import androidx.annotation.VisibleForTesting
import com.clerk.api.log.ClerkLogger
import com.clerk.api.storage.StorageHelper
import com.clerk.api.storage.StorageKey
import java.util.UUID

internal object DeviceIdGenerator {
  @Volatile private var cachedDeviceId: String? = null

  fun initialize() {
    if (cachedDeviceId == null) {
      synchronized(this) {
        if (cachedDeviceId == null) {
          try {
            val storedId = StorageHelper.loadValue(StorageKey.DEVICE_ID)

            if (!storedId.isNullOrEmpty()) {
              cachedDeviceId = storedId
              ClerkLogger.d("Loaded existing device ID from storage")
            } else {
              val newId = UUID.randomUUID().toString()
              cachedDeviceId = newId
              StorageHelper.saveValue(StorageKey.DEVICE_ID, newId)
              ClerkLogger.d("Generated and saved new device ID")
            }
          } catch (e: Exception) {
            ClerkLogger.w("Storage not available, generating temporary device ID: ${e.message}")
            cachedDeviceId = UUID.randomUUID().toString()
          }
        }
      }
    }
  }

  /**
   * Retrieves the current device ID.
   *
   * This method assumes that [initialize] has been called previously. If the device ID is not
   * available, it will attempt to initialize it automatically.
   *
   * @return The device ID string
   * @throws IllegalStateException if device ID initialization fails
   */
  fun getDeviceId(): String {
    return cachedDeviceId
      ?: run {
        initialize()
        cachedDeviceId ?: error("Device ID initialization failed")
      }
  }

  fun getOrGenerateDeviceId(): String {
    cachedDeviceId?.let {
      return it
    }

    return synchronized(this) {
      cachedDeviceId?.let {
        return@synchronized it
      }

      val deviceId =
        try {
          val existingId = StorageHelper.loadValue(StorageKey.DEVICE_ID)
          if (!existingId.isNullOrEmpty()) {
            ClerkLogger.d("Loaded existing device ID from storage during lazy initialization")
            existingId
          } else {
            val newId = UUID.randomUUID().toString()
            ClerkLogger.d("Generated temporary device ID (will persist when storage is ready)")

            tryPersistDeviceIdAsync(newId)

            newId
          }
        } catch (e: Exception) {
          ClerkLogger.w("Storage not available during lazy initialization: ${e.message}")
          UUID.randomUUID().toString()
        }

      cachedDeviceId = deviceId
      deviceId
    }
  }

  private fun tryPersistDeviceIdAsync(deviceId: String) {
    try {
      StorageHelper.saveValue(StorageKey.DEVICE_ID, deviceId)
      ClerkLogger.d("Persisted device ID to storage")
    } catch (e: Exception) {
      ClerkLogger.w("Could not persist device ID: ${e.message}")
    }
  }

  @VisibleForTesting
  internal fun clearCache() {
    cachedDeviceId = null
  }
}
