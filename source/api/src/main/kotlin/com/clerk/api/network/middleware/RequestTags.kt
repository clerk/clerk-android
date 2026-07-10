package com.clerk.api.network.middleware

/** Marks a request whose response client will be validated and applied by the caller. */
internal object ManualClientSyncRequest

/**
 * Applies response side effects only while the caller still owns the request that produced them.
 */
internal fun interface ResponseGuard {
  fun runIfAllowed(sideEffect: () -> Unit)

  companion object {
    val always = ResponseGuard { it() }
  }
}

/** Marks a request whose body must not be written to debug logs. */
internal object SensitiveRequest
