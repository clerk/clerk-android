package com.clerk.customflows.addphone

import com.clerk.api.AttemptPhoneNumberVerificationParams
import com.clerk.api.Clerk
import com.clerk.api.CreatePhoneNumberParams
import com.clerk.api.PhoneNumber
import com.clerk.api.VerificationStatus
import com.clerk.customflows.CustomFlowFeedback
import com.clerk.customflows.CustomFlowViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AddPhoneViewModel(clerk: Clerk, feedback: CustomFlowFeedback) :
  CustomFlowViewModel(clerk, feedback) {
  private val _uiState = MutableStateFlow<UiState>(UiState.NeedsVerification)
  val uiState = _uiState.asStateFlow()

  init {
    observeSession { signedIn ->
      _uiState.value = if (signedIn) UiState.NeedsVerification else UiState.SignedOut
    }
  }

  fun createPhoneNumber(phoneNumber: String) = runOperation {
    val user = requireNotNull(clerk.user) { "Sign in before adding a contact method." }
    val resource = user.createPhoneNumber(CreatePhoneNumberParams(phoneNumber = phoneNumber))
    resource.prepareVerification()
    _uiState.value = UiState.Verifying(resource)
  }

  fun verifyCode(code: String, newPhoneNumber: PhoneNumber) = runOperation {
    val resource = newPhoneNumber.attemptVerification(AttemptPhoneNumberVerificationParams(code))
    if (resource.verification.status == VerificationStatus.Verified)
      _uiState.value = UiState.Verified
    else feedback.error.value = "This contact method still needs verification."
  }

  sealed interface UiState {
    data object Loading : UiState

    data object NeedsVerification : UiState

    data class Verifying(val phoneNumber: PhoneNumber) : UiState

    data object Verified : UiState

    data object SignedOut : UiState
  }
}
