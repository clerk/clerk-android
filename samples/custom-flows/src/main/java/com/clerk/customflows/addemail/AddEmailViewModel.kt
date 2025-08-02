package com.clerk.customflows.addemail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.emailaddress.EmailAddress
import com.clerk.api.emailaddress.attemptVerification
import com.clerk.api.emailaddress.prepareVerification
import com.clerk.api.network.serialization.flatMap
import com.clerk.api.network.serialization.longErrorMessageOrNull
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.user.createEmailAddress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

class AddEmailViewModel : ViewModel() {
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

  fun createEmailAddress(emailAddress: String) {
    val user = requireNotNull(Clerk.userFlow.value)

    // Add an unverified email address to the user,
    // then send the user an email with the verification code
    viewModelScope.launch {
      user
        .createEmailAddress(emailAddress)
        .flatMap { it.prepareVerification(EmailAddress.PrepareVerificationParams.EmailCode()) }
        .onSuccess {
          // Update the state to show that the email address has been created
          // and that the user needs to verify the email address
          _uiState.value = UiState.Verifying(it)
        }
        .onFailure {
          Log.e(
            "AddEmailViewModel",
            "Failed to create email address and prepare verification: ${it.longErrorMessageOrNull}",
          )
        }
    }
  }

  fun verifyCode(code: String, newEmailAddress: EmailAddress) {
    viewModelScope.launch {
      newEmailAddress
        .attemptVerification(code)
        .onSuccess {
          // Update the state to show that the email addresshas been verified
          _uiState.value = UiState.Verified
        }
        .onFailure {
          Log.e("AddEmailViewModel", "Failed to verify email address: ${it.longErrorMessageOrNull}")
        }
    }
  }

  sealed interface UiState {
    data object Loading : UiState

    data object NeedsVerification : UiState

    data class Verifying(val emailAddress: EmailAddress) : UiState

    data object Verified : UiState

    data object SignedOut : UiState
  }
}
