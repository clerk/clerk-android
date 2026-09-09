package com.clerk.ui.auth.biometriccredential

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.*
import com.clerk.ui.auth.isSSOCancellation
import com.clerk.ui.core.common.displayMessage
import com.clerk.ui.core.common.runUiOperation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class BiometricCredentialEnrollmentViewModel(private val clerk: Clerk) : ViewModel() {
  private val _state = MutableStateFlow<State>(State.Idle)
  val state: StateFlow<State> = _state.asStateFlow()

  fun enroll(promptTitle: String, promptSubtitle: String?) {
    if (_state.value is State.Loading) return
    val user = clerk.user ?: return
    val sessionId = clerk.session?.id
    _state.value = State.Loading
    viewModelScope.launch {
      runUiOperation {
          clerk.biometricCredentials.enroll(
            BiometricCredentialEnrollmentParams(
              identifierHint = user.biometricCredentialIdentifierHint,
              reason = promptTitle,
              promptSubtitle = promptSubtitle,
            )
          )
        }
        .onSuccess {
          if (clerk.user?.id == user.id && clerk.session?.id == sessionId)
            _state.value = State.Enrolled
        }
        .onFailure {
          _state.value = if (it.isSSOCancellation) State.Idle else State.Error(it.displayMessage)
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
