package com.clerk.configuration

import androidx.annotation.VisibleForTesting
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
          val storedId = StorageHelper.loadValue(StorageKey.DEVICE_ID)
          cachedDeviceId =
            if (storedId.isNullOrEmpty()) {
              UUID.randomUUID().toString().also {
                StorageHelper.saveValue(StorageKey.DEVICE_ID, it)
              }
            } else {
              storedId
            }
        }
      }
    }
  }

  fun getDeviceId(): String {
    return cachedDeviceId ?: error("Device ID not initialized")
  }

  @VisibleForTesting
  internal fun clearCache() {
    cachedDeviceId = null
  }
}
