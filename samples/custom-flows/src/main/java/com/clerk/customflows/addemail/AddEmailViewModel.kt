package com.clerk.customflows.addemail

import com.clerk.api.AttemptEmailAddressVerificationParams
import com.clerk.api.Clerk
import com.clerk.api.CreateEmailAddressParams
import com.clerk.api.EmailAddress
import com.clerk.api.EmailAddressPrepareVerificationParamsCase1
import com.clerk.api.PrepareEmailAddressVerificationParams
import com.clerk.api.VerificationStatus
import com.clerk.customflows.CustomFlowFeedback
import com.clerk.customflows.CustomFlowViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AddEmailViewModel(clerk: Clerk, feedback: CustomFlowFeedback) :
  CustomFlowViewModel(clerk, feedback) {
  private val _uiState = MutableStateFlow<UiState>(UiState.NeedsVerification)
  val uiState = _uiState.asStateFlow()

  init {
    observeSession { signedIn ->
      _uiState.value = if (signedIn) UiState.NeedsVerification else UiState.SignedOut
    }
  }

  fun createEmailAddress(emailAddress: String) = runOperation {
    val user = requireNotNull(clerk.user) { "Sign in before adding a contact method." }
    val resource = user.createEmailAddress(CreateEmailAddressParams(email = emailAddress))
    resource.prepareVerification(
      PrepareEmailAddressVerificationParams.Case1(EmailAddressPrepareVerificationParamsCase1())
    )
    _uiState.value = UiState.Verifying(resource)
  }

  fun verifyCode(code: String, newEmailAddress: EmailAddress) = runOperation {
    val resource = newEmailAddress.attemptVerification(AttemptEmailAddressVerificationParams(code))
    if (resource.verification.status == VerificationStatus.Verified)
      _uiState.value = UiState.Verified
    else feedback.error.value = "This contact method still needs verification."
  }

  sealed interface UiState {
    data object Loading : UiState

    data object NeedsVerification : UiState

    data class Verifying(val emailAddress: EmailAddress) : UiState

    data object Verified : UiState

    data object SignedOut : UiState
  }
}
