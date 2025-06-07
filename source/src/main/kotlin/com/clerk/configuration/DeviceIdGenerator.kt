package com.clerk.configuration

import com.clerk.storage.StorageHelper
import com.clerk.storage.StorageKey
import java.util.UUID

/** Generates a unique device ID if one doesn't already exist. */
internal object DeviceIdGenerator {

  val deviceId by lazy { StorageHelper.loadValue(StorageKey.DEVICE_ID) }

  /**
   * Generates a unique device ID if one doesn't already exist and returns it.
   *
   * @return The unique device ID.
   */
  fun getOrGenerateDeviceId(): String {
    var currentDeviceId = StorageHelper.loadValue(StorageKey.DEVICE_ID)
    if (currentDeviceId.isNullOrEmpty()) {
      synchronized(this) {
        // Double-check inside the lock to handle concurrent calls
        currentDeviceId = StorageHelper.loadValue(StorageKey.DEVICE_ID)
        if (currentDeviceId.isNullOrEmpty()) {
          val generatedDeviceId = UUID.randomUUID().toString()
          StorageHelper.saveValue(StorageKey.DEVICE_ID, generatedDeviceId)
          currentDeviceId = generatedDeviceId
        }
      }
    }
    return currentDeviceId!!
  }
}
