package com.clerk.customflows.emailpassword.mfa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.auth.types.MfaType
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.verifyMfaCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MFASignInViewModel : ViewModel() {
  private val _uiState = MutableStateFlow<UiState>(UiState.Unverified)
  val uiState = _uiState.asStateFlow()

  fun submit(email: String, password: String) {
    viewModelScope.launch {
      Clerk.auth
        .signInWithPassword {
          identifier = email
          this.password = password
        }
        .onSuccess {
          if (it.status == SignIn.Status.NEEDS_SECOND_FACTOR) {
            _uiState.value = UiState.NeedsSecondFactor
          }
        }
        .onFailure {}
    }
  }

  fun verify(code: String) {
    val inProgressSignIn = Clerk.auth.currentSignIn ?: return
    viewModelScope.launch {
      inProgressSignIn
        .verifyMfaCode(code, MfaType.TOTP)
        .onSuccess {
          if (it.status == SignIn.Status.COMPLETE) {
            _uiState.value = UiState.Verified
          }
        }
        .onFailure {}
    }
  }

  sealed interface UiState {
    data object Unverified : UiState

    data object Verified : UiState

    data object NeedsSecondFactor : UiState
  }
}
