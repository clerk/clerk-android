package com.clerk.ui.auth.biometriccredential

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.ui.auth.isBiometricCredentialCancellation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** ViewModel driving the post-auth biometric credential enrollment prompt. */
internal class BiometricCredentialEnrollmentViewModel : ViewModel() {

  private val _state = MutableStateFlow<State>(State.Idle)
  val state: StateFlow<State> = _state.asStateFlow()

  /**
   * Enrolls the current device with a biometric credential.
   *
   * User-canceled biometric prompts reset the state silently instead of surfacing an error.
   */
  fun enroll(promptTitle: String, promptSubtitle: String?) {
    if (_state.value is State.Loading) return
    _state.value = State.Loading
    viewModelScope.launch(Dispatchers.IO) {
      Clerk.biometricCredentials
        .enroll(
          identifierHint = Clerk.user?.biometricCredentialIdentifierHint,
          promptTitle = promptTitle,
          promptSubtitle = promptSubtitle,
        )
        .onSuccess { withContext(Dispatchers.Main) { _state.value = State.Enrolled } }
        .onFailure { failure ->
          withContext(Dispatchers.Main) {
            _state.value =
              if (failure.isBiometricCredentialCancellation) {
                State.Idle
              } else {
                State.Error(failure.errorMessage)
              }
          }
        }
    }
  }

  fun resetState() {
    _state.value = State.Idle
  }

  internal sealed interface State {
    data object Idle : State

    data object Loading : State

    data object Enrolled : State

    data class Error(val message: String?) : State
  }
}
