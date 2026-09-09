package com.clerk.quickstart.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.CoreException
import com.clerk.api.SignUp
import com.clerk.api.SignUpCreateParams
import com.clerk.api.SignUpEmailCodeVerifyParams
import com.clerk.api.SignUpIdentificationField
import com.clerk.api.SignUpStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SignUpViewModel(private val clerk: Clerk) : ViewModel() {
  private val _uiState = MutableStateFlow<SignUpUiState>(SignUpUiState.SignedOut)
  val uiState = _uiState.asStateFlow()
  private val _error = MutableStateFlow<String?>(null)
  val error = _error.asStateFlow()
  private var attempt: SignUp? = null

  fun signUp(email: String, password: String) {
    viewModelScope.launch {
      try {
        _error.value = null
        clerk.signUp.create(SignUpCreateParams(emailAddress = email, password = password))
        val signUp = clerk.signUp
        attempt = signUp
        if (signUp.status == SignUpStatus.Complete) {
          signUp.finalize()
          _uiState.value = SignUpUiState.Success
        } else if (SignUpIdentificationField.EmailAddress in signUp.unverifiedFields) {
          signUp.verifications.sendEmailCode()
          _uiState.value = SignUpUiState.NeedsVerification
        } else {
          showRemainingRequirements()
        }
      } catch (error: CoreException) {
        _error.value = error.errors.firstOrNull()?.longMessage ?: error.message
      }
    }
  }

  fun verify(code: String) {
    val signUp = attempt ?: return
    viewModelScope.launch {
      try {
        _error.value = null
        signUp.verifications.verifyEmailCode(SignUpEmailCodeVerifyParams(code))
        if (signUp.status == SignUpStatus.Complete) {
          signUp.finalize()
          _uiState.value = SignUpUiState.Success
        } else {
          showRemainingRequirements()
        }
      } catch (error: CoreException) {
        _error.value = error.errors.firstOrNull()?.longMessage ?: error.message
      }
    }
  }

  private fun showRemainingRequirements() {
    _error.value =
      "Additional account details are required. Use the prebuilt-ui sample for this flow."
  }
}

sealed interface SignUpUiState {
  data object SignedOut : SignUpUiState

  data object Success : SignUpUiState

  data object NeedsVerification : SignUpUiState
}
