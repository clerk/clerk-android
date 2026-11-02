package com.clerk.api.network.middleware

import com.clerk.api.Clerk
import java.util.concurrent.atomic.AtomicReference

internal class ManualClientSyncRequest {
  private val observedResponse = AtomicReference<DeviceTokenHeaders?>()

  internal fun recordResponse(requestDeviceToken: String?, responseDeviceToken: String?) {
    observedResponse.set(
      DeviceTokenHeaders(
        requestDeviceToken = requestDeviceToken,
        responseDeviceToken = responseDeviceToken,
      )
    )
  }

  internal fun runIfResponseCurrent(sideEffect: () -> Unit): Boolean {
    val response = observedResponse.get()
    val isCurrent =
      response != null &&
        Clerk.isClientResponseCurrent(
          requestDeviceToken = response.requestDeviceToken,
          responseDeviceToken = response.responseDeviceToken,
        )
    if (isCurrent) {
      sideEffect()
    }
    return isCurrent
  }

  private data class DeviceTokenHeaders(
    val requestDeviceToken: String?,
    val responseDeviceToken: String?,
  )
}

internal fun interface ResponseGuard {
  fun runIfAllowed(sideEffect: () -> Unit)

  companion object {
    val always = ResponseGuard { it() }
  }
}

internal object SensitiveRequest
