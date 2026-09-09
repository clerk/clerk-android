package com.clerk.e2e

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.CoreException
import com.clerk.api.SessionStatus
import com.clerk.api.SignInCreateParams
import com.clerk.api.SignInPhoneCodeVerifyParams
import com.clerk.api.SignInStatus
import com.clerk.api.SignUpCreateParams
import com.clerk.api.SignUpEmailCodeVerifyParams
import com.clerk.api.SignUpIdentificationField
import com.clerk.api.SignUpPhoneCodeVerifyParams
import com.clerk.api.SignUpStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val US_PHONE_NUMBER_DIGIT_COUNT = 10
private const val US_PHONE_NUMBER_WITH_COUNTRY_CODE_DIGIT_COUNT = 11

class E2EViewModel(private val clerk: Clerk) : ViewModel() {
  private companion object {
    const val TEST_PASSWORD = "Trailblaze424242!"
  }

  private val _customOtpState = MutableStateFlow<CustomOtpState>(CustomOtpState.Idle)
  val customOtpState = _customOtpState.asStateFlow()
  private var customOtpMode: CustomOtpMode? = null

  fun resetCustomOtpState() {
    customOtpMode = null
    _customOtpState.value = CustomOtpState.Idle
  }

  private fun operation(block: suspend () -> Unit) {
    _customOtpState.value = CustomOtpState.Loading
    viewModelScope.launch {
      try {
        block()
      } catch (error: CancellationException) {
        throw error
      } catch (error: Exception) {
        _customOtpState.value = CustomOtpState.Error(error.localizedMessage, customOtpMode != null)
      }
    }
  }

  fun submitCustomOtpPhone(phoneNumber: String) = operation {
    customOtpMode = null
    val normalized = phoneNumber.normalizedUsTestPhoneNumber()
    try {
      clerk.signIn.create(SignInCreateParams(identifier = normalized))
      clerk.signIn.phoneCode.sendCode()
      customOtpMode = CustomOtpMode.SignIn
    } catch (error: CoreException) {
      if (error.errors.none { it.code == "form_identifier_not_found" }) throw error
      val identifier = normalized.testIdentifier()
      clerk.signUp.create(
        SignUpCreateParams(
          phoneNumber = normalized,
          emailAddress = "android-e2e-$identifier+clerk_test@example.com",
          username = "android_e2e_$identifier",
          password = TEST_PASSWORD,
        )
      )
      clerk.signUp.verifications.sendPhoneCode()
      customOtpMode = CustomOtpMode.SignUp
    }
    _customOtpState.value = CustomOtpState.AwaitingCode
  }

  fun verifyCustomOtpCode(code: String) = operation {
    when (customOtpMode) {
      CustomOtpMode.SignIn -> {
        val attempt = clerk.signIn
        attempt.phoneCode.verifyCode(SignInPhoneCodeVerifyParams(code))
        if (attempt.status == SignInStatus.Complete) attempt.finalize()
      }
      CustomOtpMode.SignUp -> {
        val attempt = clerk.signUp
        attempt.verifications.verifyPhoneCode(SignUpPhoneCodeVerifyParams(code))
        // E2E-only identifiers use Clerk test verification codes for both channels.
        if (
          attempt.status != SignUpStatus.Complete &&
            SignUpIdentificationField.EmailAddress in attempt.unverifiedFields
        ) {
          attempt.verifications.sendEmailCode()
          attempt.verifications.verifyEmailCode(SignUpEmailCodeVerifyParams(code))
        }
        if (attempt.status == SignUpStatus.Complete) attempt.finalize()
      }
      null -> error("No OTP flow is in progress.")
    }
    _customOtpState.value =
      if (
        clerk.session?.status == SessionStatus.Active &&
          clerk.session?.currentTask == null &&
          clerk.user != null
      )
        CustomOtpState.SignedIn
      else CustomOtpState.RequiresCompletion
  }

  fun signOut() = operation {
    clerk.signOut()
    resetCustomOtpState()
  }
}

sealed interface CustomOtpState {
  data object Idle : CustomOtpState

  data object Loading : CustomOtpState

  data object AwaitingCode : CustomOtpState

  data object SignedIn : CustomOtpState

  data object RequiresCompletion : CustomOtpState

  data class Error(val message: String?, val awaitingCode: Boolean = false) : CustomOtpState
}

private enum class CustomOtpMode {
  SignIn,
  SignUp,
}

private fun String.normalizedUsTestPhoneNumber(): String {
  val trimmed = trim()
  val digits = trimmed.filter(Char::isDigit)
  return when {
    trimmed.startsWith("+") -> "+$digits"
    digits.length == US_PHONE_NUMBER_DIGIT_COUNT -> "+1$digits"
    digits.length == US_PHONE_NUMBER_WITH_COUNTRY_CODE_DIGIT_COUNT && digits.startsWith("1") ->
      "+$digits"
    else -> trimmed
  }
}

private fun String.testIdentifier(): String = filter(Char::isDigit)
