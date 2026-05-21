package com.clerk.e2e

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.verifyCode
import com.clerk.api.signup.SignUp
import com.clerk.api.signup.attemptVerification
import com.clerk.api.signup.sendEmailCode
import com.clerk.api.signup.sendPhoneCode
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.sso.OAuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val US_PHONE_NUMBER_DIGIT_COUNT = 10
private const val US_PHONE_NUMBER_WITH_COUNTRY_CODE_DIGIT_COUNT = 11

@Suppress("TooManyFunctions")
class E2EViewModel : ViewModel() {
  private companion object {
    const val TEST_PASSWORD = "Trailblaze424242!"
  }

  private val _customOtpState = MutableStateFlow<CustomOtpState>(CustomOtpState.Idle)
  val customOtpState = _customOtpState.asStateFlow()
  private val _oauthState = MutableStateFlow<OAuthState>(OAuthState.Idle)
  val oauthState = _oauthState.asStateFlow()
  private var customOtpMode: CustomOtpMode? = null

  fun resetCustomOtpState() {
    customOtpMode = null
    _customOtpState.value = CustomOtpState.Idle
  }

  fun resetOAuthState() {
    _oauthState.value = OAuthState.Idle
  }

  fun submitCustomOtpPhone(phoneNumber: String) {
    _customOtpState.value = CustomOtpState.Loading
    viewModelScope.launch {
      val normalizedPhoneNumber = phoneNumber.normalizedUsTestPhoneNumber()
      when (val result = Clerk.auth.signInWithOtp { phone = normalizedPhoneNumber }) {
        is ClerkResult.Success -> {
          customOtpMode = CustomOtpMode.SignIn
          _customOtpState.value = CustomOtpState.AwaitingCode
        }
        is ClerkResult.Failure -> {
          if (result.hasErrorCode("form_identifier_not_found")) {
            createCustomOtpTestUser(normalizedPhoneNumber)
          } else {
            _customOtpState.value = CustomOtpState.Error(result.errorMessage)
          }
        }
      }
    }
  }

  fun verifyCustomOtpCode(code: String) {
    _customOtpState.value = CustomOtpState.Loading
    viewModelScope.launch {
      when (customOtpMode) {
        CustomOtpMode.SignIn -> verifyCustomSignInCode(code)
        CustomOtpMode.SignUp -> verifyCustomSignUpCode(code)
        null -> _customOtpState.value = CustomOtpState.Error("No OTP flow is in progress.")
      }
    }
  }

  private suspend fun createCustomOtpTestUser(phoneNumber: String) {
    val testIdentifier = phoneNumber.testIdentifier()
    when (
      val signUpResult =
        Clerk.auth.signUp {
          phone = phoneNumber
          email = "android-e2e-$testIdentifier+clerk_test@example.com"
          username = "android_e2e_$testIdentifier"
          password = TEST_PASSWORD
        }
    ) {
      is ClerkResult.Success -> {
        when (val prepareResult = signUpResult.value.sendPhoneCode()) {
          is ClerkResult.Success -> {
            customOtpMode = CustomOtpMode.SignUp
            _customOtpState.value = CustomOtpState.AwaitingCode
          }
          is ClerkResult.Failure -> {
            _customOtpState.value = CustomOtpState.Error(prepareResult.errorMessage)
          }
        }
      }
      is ClerkResult.Failure ->
        _customOtpState.value = CustomOtpState.Error(signUpResult.errorMessage)
    }
  }

  private suspend fun verifyCustomSignInCode(code: String) {
    val signIn = Clerk.auth.currentSignIn
    if (signIn == null) {
      _customOtpState.value = CustomOtpState.Error("No sign-in is in progress.")
      return
    }

    when (val result = signIn.verifyCode(code)) {
      is ClerkResult.Success -> {
        val verifiedSignIn = result.value
        if (verifiedSignIn.status != SignIn.Status.COMPLETE) {
          _customOtpState.value =
            CustomOtpState.Error("Sign-in requires another step: ${verifiedSignIn.status}.")
          return
        }

        activateSession(verifiedSignIn.createdSessionId)
      }
      is ClerkResult.Failure -> _customOtpState.value = CustomOtpState.Error(result.errorMessage)
    }
  }

  private suspend fun verifyCustomSignUpCode(code: String) {
    val signUp = Clerk.auth.currentSignUp
    if (signUp == null) {
      _customOtpState.value = CustomOtpState.Error("No sign-up is in progress.")
      return
    }

    when (
      val result = signUp.attemptVerification(SignUp.AttemptVerificationParams.PhoneCode(code))
    ) {
      is ClerkResult.Success -> completeVerifiedSignUp(result.value, code)
      is ClerkResult.Failure -> _customOtpState.value = CustomOtpState.Error(result.errorMessage)
    }
  }

  private suspend fun completeVerifiedSignUp(signUp: SignUp, code: String) {
    if (signUp.status == SignUp.Status.COMPLETE) {
      activateSession(signUp.createdSessionId)
      return
    }

    if (signUp.unverifiedFields.contains("email_address")) {
      verifySignUpEmail(signUp, code)
      return
    }

    _customOtpState.value = CustomOtpState.Error("Sign-up requires another step: ${signUp.status}.")
  }

  private suspend fun verifySignUpEmail(signUp: SignUp, code: String) {
    when (val prepareResult = signUp.sendEmailCode()) {
      is ClerkResult.Success -> {
        when (
          val result =
            prepareResult.value.attemptVerification(
              SignUp.AttemptVerificationParams.EmailCode(code)
            )
        ) {
          is ClerkResult.Success -> completeVerifiedSignUp(result.value, code)
          is ClerkResult.Failure ->
            _customOtpState.value = CustomOtpState.Error(result.errorMessage)
        }
      }
      is ClerkResult.Failure ->
        _customOtpState.value = CustomOtpState.Error(prepareResult.errorMessage)
    }
  }

  private suspend fun activateSession(createdSessionId: String?) {
    if (createdSessionId == null) {
      _customOtpState.value = CustomOtpState.SignedIn
      return
    }

    when (val result = Clerk.auth.setActive(createdSessionId)) {
      is ClerkResult.Success -> _customOtpState.value = CustomOtpState.SignedIn
      is ClerkResult.Failure -> _customOtpState.value = CustomOtpState.Error(result.errorMessage)
    }
  }

  fun signInWithGoogleOAuth() {
    _oauthState.value = OAuthState.Loading
    viewModelScope.launch {
      when (val result = Clerk.auth.signInWithOAuth(OAuthProvider.GOOGLE)) {
        is ClerkResult.Success -> completeOAuthResult(result.value)
        is ClerkResult.Failure -> _oauthState.value = OAuthState.Error(result.errorMessage)
      }
    }
  }

  private suspend fun completeOAuthResult(result: OAuthResult) {
    val signIn = result.signIn
    val signUp = result.signUp
    val errorMessage =
      when {
        signIn != null && signIn.status != SignIn.Status.COMPLETE ->
          "OAuth sign-in requires another step: ${signIn.status}."
        signUp != null && signUp.status != SignUp.Status.COMPLETE ->
          "OAuth sign-up requires another step: ${signUp.status}."
        signIn == null && signUp == null -> "OAuth completed without a sign-in or sign-up result."
        else -> null
      }
    if (errorMessage != null) {
      _oauthState.value = OAuthState.Error(errorMessage)
    } else {
      activateOAuthSession(signIn?.createdSessionId ?: signUp?.createdSessionId)
    }
  }

  private suspend fun activateOAuthSession(createdSessionId: String?) {
    if (createdSessionId == null) {
      _oauthState.value = OAuthState.SignedIn
      return
    }

    when (val result = Clerk.auth.setActive(createdSessionId)) {
      is ClerkResult.Success -> _oauthState.value = OAuthState.SignedIn
      is ClerkResult.Failure -> _oauthState.value = OAuthState.Error(result.errorMessage)
    }
  }

  fun signOut() {
    viewModelScope.launch {
      when (val result = Clerk.auth.signOut()) {
        is ClerkResult.Success -> {
          resetCustomOtpState()
          resetOAuthState()
        }
        is ClerkResult.Failure -> _customOtpState.value = CustomOtpState.Error(result.errorMessage)
      }
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

sealed interface OAuthState {
  data object Idle : OAuthState

  data object Loading : OAuthState

  data object SignedIn : OAuthState

  data class Error(val message: String?) : OAuthState
}

private enum class CustomOtpMode {
  SignIn,
  SignUp,
}

private fun ClerkResult.Failure<ClerkErrorResponse>.hasErrorCode(code: String): Boolean {
  return error?.errors?.any { it.code == code } == true
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
