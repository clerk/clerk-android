package com.clerk.ui.userprofile.security.biometriccredential

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.errorMessage
import com.clerk.ui.auth.biometriccredential.biometricCredentialIdentifierHint
import com.clerk.ui.auth.isBiometricCredentialCancellation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** ViewModel backing the biometric sign-in toggle in the user-profile security screen. */
internal class UserProfileBiometricCredentialViewModel(
  private val workDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

  private val _state = MutableStateFlow(State())
  val state: StateFlow<State> = _state.asStateFlow()
  private var availabilityRequestGeneration = 0

  /** Refreshes availability locally first, then reconciles with the server. */
  fun refreshAvailability() {
    val requestGeneration = ++availabilityRequestGeneration
    _state.update {
      it.copy(isEnabled = Clerk.biometricCredentials.currentUserLocalAvailability().isAvailable)
    }
    viewModelScope.launch(workDispatcher) {
      val availability = Clerk.biometricCredentials.currentUserAvailability()
      withContext(Dispatchers.Main) {
        if (requestGeneration == availabilityRequestGeneration) {
          _state.update { it.copy(isEnabled = availability.isAvailable) }
        }
      }
    }
  }

  /**
   * Enables or disables biometric sign-in for the current user on this device.
   *
   * Enabling enrolls this device as a biometric credential (showing the system biometric prompt);
   * disabling revokes the local biometric credential. User-canceled biometric prompts revert the
   * toggle silently.
   */
  fun setBiometricSignInEnabled(
    enabled: Boolean,
    promptTitle: String,
    promptSubtitle: String?,
  ) {
    val current = _state.value
    if (current.isLoading || current.isEnabled == enabled) return
    availabilityRequestGeneration += 1
    _state.value = current.copy(isEnabled = enabled, isLoading = true)

    viewModelScope.launch(workDispatcher) {
      val failure =
        if (enabled) {
          Clerk.biometricCredentials
            .enroll(
              identifierHint = Clerk.user?.biometricCredentialIdentifierHint,
              promptTitle = promptTitle,
              promptSubtitle = promptSubtitle,
            )
            .asFailureOrNull()
        } else {
          Clerk.biometricCredentials.revokeCurrentDeviceCredential().asFailureOrNull()
        }

      val availability = Clerk.biometricCredentials.currentUserAvailability()
      withContext(Dispatchers.Main) {
        _state.value =
          State(
            isEnabled = availability.isAvailable,
            isLoading = false,
            error =
              failure?.takeUnless { it.isBiometricCredentialCancellation }?.let { it.errorMessage },
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
