package com.clerk.customflows.addphone

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.flatMap
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.phonenumber.PhoneNumber
import com.clerk.api.phonenumber.attemptVerification
import com.clerk.api.phonenumber.prepareVerification
import com.clerk.api.user.createPhoneNumber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

class AddPhoneViewModel : ViewModel() {
  private val _uiState = MutableStateFlow<UiState>(UiState.NeedsVerification)
  val uiState = _uiState.asStateFlow()

  init {
    combine(Clerk.isInitialized, Clerk.userFlow) { isInitialized, user ->
        _uiState.value =
          when {
            !isInitialized -> UiState.Loading
            user == null -> UiState.SignedOut
            else -> UiState.NeedsVerification
          }
      }
      .launchIn(viewModelScope)
  }

  fun createPhoneNumber(phoneNumber: String) {
    val user = requireNotNull(Clerk.userFlow.value)

    // Add an unverified phone number to the user,
    // then send the user an SMS with the verification code
    viewModelScope.launch {
      user
        .createPhoneNumber(phoneNumber)
        .flatMap { it.prepareVerification() }
        .onSuccess {
          // Update the state to show that the phone number has been created
          // and that the user needs to verify the phone number
          _uiState.value = UiState.Verifying(it)
        }
        .onFailure {
          Log.e(
            "AddPhoneViewModel",
            "Failed to create phone number and prepare verification: ${it.errorMessage}",
          )
        }
    }
  }

  fun verifyCode(code: String, newPhoneNumber: PhoneNumber) {
    viewModelScope.launch {
      newPhoneNumber
        .attemptVerification(code)
        .onSuccess {
          // Update the state to show that the phone number has been verified
          _uiState.value = UiState.Verified
        }
        .onFailure {
          Log.e("AddPhoneViewModel", "Failed to verify phone number: ${it.errorMessage}")
        }
    }
  }

  sealed interface UiState {
    data object Loading : UiState

    data object NeedsVerification : UiState

    data class Verifying(val phoneNumber: PhoneNumber) : UiState

    data object Verified : UiState

    data object SignedOut : UiState
  }
}
