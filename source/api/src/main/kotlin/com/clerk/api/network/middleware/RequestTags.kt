package com.clerk.api.network.middleware

import com.clerk.api.Clerk
import java.util.concurrent.atomic.AtomicReference

/**
 * Marks a request whose response client is validated and applied by the caller instead of by
 * [com.clerk.api.network.middleware.incoming.ClientSyncingMiddleware]. It records the response's
 * device-token headers so the caller can reject a stale shared-session response before applying
 * that client.
 */
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

/**
 * Applies response side effects only while the caller still owns the request that produced them.
 *
 * Consumed by client syncing; device-token saving intentionally ignores it because the token in a
 * response reflects the server's current credential for this client even after the local flow was
 * cancelled.
 */
internal fun interface ResponseGuard {
  fun runIfAllowed(sideEffect: () -> Unit)

  companion object {
    val always = ResponseGuard { it() }
  }
}

/** Marks a request whose body must not be written to debug logs. */
internal object SensitiveRequest
