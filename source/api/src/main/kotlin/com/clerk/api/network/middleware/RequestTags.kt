package com.clerk.api.network.middleware

/**
 * Marks a request whose response client is validated and applied by the caller instead of by
 * [com.clerk.api.network.middleware.incoming.ClientSyncingMiddleware].
 */
internal object ManualClientSyncRequest

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
