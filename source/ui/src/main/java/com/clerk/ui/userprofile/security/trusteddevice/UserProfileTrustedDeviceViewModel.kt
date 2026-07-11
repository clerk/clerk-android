package com.clerk.ui.userprofile.security.trusteddevice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.errorMessage
import com.clerk.ui.auth.isTrustedDeviceCancellation
import com.clerk.ui.auth.trusteddevice.trustedDeviceIdentifierHint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** ViewModel backing the biometric sign-in toggle in the user-profile security screen. */
internal class UserProfileTrustedDeviceViewModel : ViewModel() {

  private val _state = MutableStateFlow(State())
  val state: StateFlow<State> = _state.asStateFlow()

  /** Refreshes availability locally first, then reconciles with the server. */
  fun refreshAvailability() {
    _state.update {
      it.copy(isEnabled = Clerk.trustedDevices.currentUserLocalAvailability().isAvailable)
    }
    viewModelScope.launch(Dispatchers.IO) {
      val availability = Clerk.trustedDevices.currentUserAvailability()
      withContext(Dispatchers.Main) {
        _state.update { it.copy(isEnabled = availability.isAvailable) }
      }
    }
  }

  /**
   * Enables or disables biometric sign-in for the current user on this device.
   *
   * Enabling enrolls this device as a trusted device (showing the system biometric prompt);
   * disabling revokes the local trusted-device credential. User-canceled biometric prompts revert
   * the toggle silently.
   */
  fun setTrustedDeviceSignInEnabled(
    enabled: Boolean,
    promptTitle: String,
    promptSubtitle: String?,
  ) {
    val current = _state.value
    if (current.isLoading || current.isEnabled == enabled) return
    _state.value = current.copy(isEnabled = enabled, isLoading = true)

    viewModelScope.launch(Dispatchers.IO) {
      val failure =
        if (enabled) {
          Clerk.trustedDevices
            .enroll(
              identifierHint = Clerk.user?.trustedDeviceIdentifierHint,
              promptTitle = promptTitle,
              promptSubtitle = promptSubtitle,
            )
            .asFailureOrNull()
        } else {
          Clerk.trustedDevices.revokeCurrentDeviceCredential().asFailureOrNull()
        }

      val availability = Clerk.trustedDevices.currentUserAvailability()
      withContext(Dispatchers.Main) {
        _state.value =
          State(
            isEnabled = availability.isAvailable,
            isLoading = false,
            error = failure?.takeUnless { it.isTrustedDeviceCancellation }?.let { it.errorMessage },
          )
      }
    }
  }

  fun clearError() {
    _state.update { it.copy(error = null) }
  }

  internal data class State(
    val isEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
  )
}

private fun <T : Any> ClerkResult<T, com.clerk.api.network.model.error.ClerkErrorResponse>
  .asFailureOrNull(): ClerkResult.Failure<com.clerk.api.network.model.error.ClerkErrorResponse>? {
  return this as? ClerkResult.Failure
}
