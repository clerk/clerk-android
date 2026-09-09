package com.clerk.quickstart.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.CoreException
import com.clerk.api.SignInPasswordParams
import com.clerk.api.SignInPasswordParamsCase2
import com.clerk.api.SignInStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SignInViewModel(private val clerk: Clerk) : ViewModel() {
  private val _error = MutableStateFlow<String?>(null)
  val error = _error.asStateFlow()

  fun signIn(email: String, password: String) {
    viewModelScope.launch {
      try {
        _error.value = null
        clerk.signIn.password(
          SignInPasswordParams.Case2(SignInPasswordParamsCase2(password, email))
        )
        val signIn = clerk.signIn
        if (signIn.status == SignInStatus.Complete) {
          signIn.finalize()
        } else {
          _error.value =
            "Additional verification is required. Use the prebuilt-ui sample for this flow."
        }
      } catch (error: CoreException) {
        _error.value = error.errors.firstOrNull()?.longMessage ?: error.message
      }
    }
  }
}
