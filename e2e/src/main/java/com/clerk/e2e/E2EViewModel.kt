package com.clerk.e2e

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.verifyCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class E2EViewModel : ViewModel() {
  private val _customOtpState = MutableStateFlow<CustomOtpState>(CustomOtpState.Idle)
  val customOtpState = _customOtpState.asStateFlow()

  fun resetCustomOtpState() {
    _customOtpState.value = CustomOtpState.Idle
  }

  fun submitCustomOtpPhone(phoneNumber: String) {
    _customOtpState.value = CustomOtpState.Loading
    viewModelScope.launch {
      Clerk.auth
        .signInWithOtp { phone = phoneNumber.normalizedUsTestPhoneNumber() }
        .onSuccess { _customOtpState.value = CustomOtpState.AwaitingCode }
        .onFailure { _customOtpState.value = CustomOtpState.Error(it.errorMessage) }
    }
  }

  fun verifyCustomOtpCode(code: String) {
    val signIn = Clerk.auth.currentSignIn
    if (signIn == null) {
      _customOtpState.value = CustomOtpState.Error("No sign-in is in progress.")
      return
    }

    _customOtpState.value = CustomOtpState.Loading
    viewModelScope.launch {
      signIn
        .verifyCode(code)
        .onSuccess { verifiedSignIn ->
          if (verifiedSignIn.status != SignIn.Status.COMPLETE) {
            _customOtpState.value =
              CustomOtpState.Error("Sign-in requires another step: ${verifiedSignIn.status}.")
            return@onSuccess
          }

          val createdSessionId = verifiedSignIn.createdSessionId
          if (createdSessionId == null) {
            _customOtpState.value = CustomOtpState.SignedIn
            return@onSuccess
          }

          Clerk.auth
            .setActive(createdSessionId)
            .onSuccess { _customOtpState.value = CustomOtpState.SignedIn }
            .onFailure { _customOtpState.value = CustomOtpState.Error(it.errorMessage) }
        }
        .onFailure { _customOtpState.value = CustomOtpState.Error(it.errorMessage) }
    }
  }

  fun signOut() {
    viewModelScope.launch {
      Clerk.auth
        .signOut()
        .onSuccess { resetCustomOtpState() }
        .onFailure { _customOtpState.value = CustomOtpState.Error(it.errorMessage) }
    }
  }
}

sealed interface CustomOtpState {
  data object Idle : CustomOtpState

  data object Loading : CustomOtpState

  data object AwaitingCode : CustomOtpState

  data object SignedIn : CustomOtpState

  data class Error(val message: String?) : CustomOtpState
}

private fun String.normalizedUsTestPhoneNumber(): String {
  val trimmed = trim()
  val digits = trimmed.filter(Char::isDigit)
  return when {
    trimmed.startsWith("+") -> "+$digits"
    digits.length == 10 -> "+1$digits"
    digits.length == 11 && digits.startsWith("1") -> "+$digits"
    else -> trimmed
  }
}
