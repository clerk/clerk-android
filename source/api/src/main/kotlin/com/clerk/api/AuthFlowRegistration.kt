package com.clerk.api

import androidx.annotation.RestrictTo
import java.util.concurrent.atomic.AtomicBoolean

/** Keeps an SDK-owned authentication flow registered until the owning UI is disposed. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AuthFlowRegistration internal constructor(private val unregister: () -> Unit) :
  AutoCloseable {
  private val isClosed = AtomicBoolean(false)

  override fun close() {
    if (isClosed.compareAndSet(false, true)) {
      unregister()
    }
  }
}
