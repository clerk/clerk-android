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
    get() =
      clerk.session?.let {
        it.status == SessionStatus.Active &&
          it.currentTask == null &&
          (registrations == 0 || completedSessionId == it.id)
      } == true

  fun register(): AutoCloseable {
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
    completedSessionId = clerk.session?.id
  }

  fun pending() {
    completedSessionId = null
  }
}
