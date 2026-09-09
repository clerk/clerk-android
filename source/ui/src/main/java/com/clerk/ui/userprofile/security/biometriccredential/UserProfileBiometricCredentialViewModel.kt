package com.clerk.ui.userprofile.security.biometriccredential

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.*
import com.clerk.ui.auth.biometriccredential.biometricCredentialIdentifierHint
import com.clerk.ui.core.common.displayMessage
import com.clerk.ui.core.common.runUiOperation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Presentation state for the current user's device credential toggle. */
internal class UserProfileBiometricCredentialViewModel(private val clerk: Clerk) : ViewModel() {
  private val _state = MutableStateFlow(State())
  val state: StateFlow<State> = _state.asStateFlow()
  private var requestGeneration = 0
  private var displayedUserId: String? = null

  fun refreshAvailability() {
    val generation = ++requestGeneration
    val userId = clerk.user?.id
    if (displayedUserId != userId) _state.value = State()
    displayedUserId = userId
    viewModelScope.launch {
      runUiOperation {
          val selection = BiometricCredentialSelectionParams(currentUser = true)
          val local = clerk.biometricCredentials.localAvailability(selection)
          if (isCurrent(generation, userId))
            _state.update { it.copy(isEnabled = local.isAvailable) }
          clerk.biometricCredentials.availability(selection)
        }
        .onSuccess { available ->
          if (isCurrent(generation, userId))
            _state.update { it.copy(isEnabled = available.isAvailable) }
        }
        .onFailure { error ->
          if (isCurrent(generation, userId)) _state.update { it.copy(error = error.displayMessage) }
        }
    }
  }

  fun setBiometricSignInEnabled(enabled: Boolean, promptTitle: String, promptSubtitle: String?) {
    val previous = _state.value
    if (previous.isLoading || previous.isEnabled == enabled) return
    val generation = ++requestGeneration
    val userId = clerk.user?.id
    _state.value = previous.copy(isEnabled = enabled, isLoading = true, error = null)
    viewModelScope.launch {
      val operation = runUiOperation {
        if (enabled) {
          clerk.biometricCredentials.enroll(
            BiometricCredentialEnrollmentParams(
              identifierHint = clerk.user?.biometricCredentialIdentifierHint,
              reason = promptTitle,
              promptSubtitle = promptSubtitle,
            )
          )
        } else clerk.biometricCredentials.revokeCurrentDeviceCredential()
      }
      val availability = runUiOperation {
        clerk.biometricCredentials.availability(
          BiometricCredentialSelectionParams(currentUser = true)
        )
      }
      if (isCurrent(generation, userId)) {
        val failure = operation.exceptionOrNull() ?: availability.exceptionOrNull()
        val wasCancelled =
          (failure as? CoreException)?.let {
            it.code == "user_cancelled" || it.kind == CoreFailureKind.Cancelled
          } == true
        _state.value =
          State(
            isEnabled = availability.getOrNull()?.isAvailable ?: previous.isEnabled,
            error = failure?.takeUnless { wasCancelled }?.displayMessage,
          )
      }
    }
  }

  private fun isCurrent(generation: Int, userId: String?): Boolean =
    generation == requestGeneration && clerk.user?.id == userId

  fun clearError() {
    _state.update { it.copy(error = null) }
  }

  internal data class State(
    val isEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
  )
}
