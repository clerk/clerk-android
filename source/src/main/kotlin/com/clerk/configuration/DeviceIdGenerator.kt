package com.clerk.configuration

import androidx.annotation.VisibleForTesting
import com.clerk.storage.StorageHelper
import com.clerk.storage.StorageKey
import java.util.UUID

/** Generates a unique device ID if one doesn't already exist. */
internal object DeviceIdGenerator {

  private var _deviceId: String? = null
  private var deviceIdInitialized = false

  val deviceId: String?
    get() {
      if (!deviceIdInitialized) {
        _deviceId = StorageHelper.loadValue(StorageKey.DEVICE_ID)
        deviceIdInitialized = true
      }
      return _deviceId
    }

  // Volatile to ensure visibility across threads
  @Volatile private var cachedDeviceId: String? = null

  @Suppress("ReturnCount")
  /**
   * Generates a unique device ID if one doesn't already exist and returns it. This method is
   * thread-safe and ensures only one device ID is ever generated.
   *
   * @return The unique device ID.
   */
  fun getOrGenerateDeviceId(): String {
    // First check without synchronization for performance
    cachedDeviceId?.let {
      return it
    }

    synchronized(this) {
      // Double-check inside the lock
      cachedDeviceId?.let {
        return it
      }

      // Load from storage
      val storedDeviceId = StorageHelper.loadValue(StorageKey.DEVICE_ID)
      if (!storedDeviceId.isNullOrEmpty()) {
        cachedDeviceId = storedDeviceId
        return storedDeviceId
      }

      // Generate new device ID only if none exists
      val generatedDeviceId = UUID.randomUUID().toString()
      StorageHelper.saveValue(StorageKey.DEVICE_ID, generatedDeviceId)
      cachedDeviceId = generatedDeviceId
      return generatedDeviceId
    }
  }

  @VisibleForTesting
  internal fun clearCache() {
    cachedDeviceId = null
    _deviceId = null
    deviceIdInitialized = false
  }
}
