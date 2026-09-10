package com.clerk.ui.core.composition

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.clerk.api.Clerk
import com.clerk.api.SessionStatus

/**
 * Tracks only presentation ownership. Authentication and session tasks are reported by the core.
 */
internal class AuthPresentationState(private val clerk: Clerk) {
  private var registrations by mutableIntStateOf(0)
  private var completedSessionId by mutableStateOf<String?>(null)
  val isComplete: Boolean
    get() = readySessionId()?.let { registrations == 0 || completedSessionId == it } == true

  private fun readySessionId(): String? =
    clerk.session
      ?.takeIf { it.status == SessionStatus.Active && it.currentTask == null && clerk.user != null }
      ?.id

  fun register(): AutoCloseable {
    if (readySessionId() != null) return AutoCloseable {}
    if (registrations == 0) completedSessionId = null
    registrations++
    var closed = false
    return AutoCloseable {
      if (!closed) {
        closed = true
        registrations--
      }
    }
  }

  fun complete() {
    completedSessionId = readySessionId() ?: return
  }

  fun pending() {
    completedSessionId = null
  }
}
