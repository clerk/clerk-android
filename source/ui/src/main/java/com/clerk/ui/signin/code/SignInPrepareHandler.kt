package com.clerk.ui.signin.code

import com.clerk.api.log.ClerkLog
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.prepareFirstFactor
import com.clerk.api.signin.prepareSecondFactor
import com.clerk.ui.core.common.StrategyKeys

internal class SignInPrepareHandler {

  internal suspend fun prepareForResetWithEmailCode(
    inProgressSignIn: SignIn,
    factor: Factor,
    onError: (String) -> Unit,
  ) {
    val emailAddressId = factor.emailAddressId
    if (emailAddressId == null) {
      ClerkLog.e("Error preparing for reset password with email code: emailAddressId is null")
      return
    }

    inProgressSignIn
      .prepareFirstFactor(
        SignIn.PrepareFirstFactorParams.ResetPasswordEmailCode(emailAddressId = emailAddressId)
      )
      .onFailure {
        ClerkLog.e("Error preparing for reset password with email code: $it")
        onError(it.errorMessage)
      }
  }

  internal suspend fun prepareForResetPasswordWithPhone(
    inProgressSignIn: SignIn,
    factor: Factor,
    onError: (String) -> Unit,
  ) {
    val phoneNumberId = factor.phoneNumberId
    if (phoneNumberId == null) {
      ClerkLog.e("Error preparing for reset password with phone code: phoneNumberId is null")
      return
    }

    inProgressSignIn
      .prepareFirstFactor(
        SignIn.PrepareFirstFactorParams.ResetPasswordPhoneCode(phoneNumberId = phoneNumberId)
      )
      .onFailure {
        onError(it.errorMessage)
        ClerkLog.e("Error preparing for reset password with phone code: $it")
      }
  }

  internal suspend fun prepareForPhoneCode(
    inProgressSignIn: SignIn,
    factor: Factor,
    isSecondFactor: Boolean,
    onError: (String) -> Unit,
  ) {
    val phoneNumberId = factor.phoneNumberId
    if (phoneNumberId == null) {
      ClerkLog.e("Error preparing for phone code: phoneNumberId is null")
      return
    }

    if (isSecondFactor) {
      prepareSecondFactorPhoneCode(inProgressSignIn, phoneNumberId, onError)
    } else {
      prepareFirstFactorPhoneCode(inProgressSignIn, phoneNumberId, onError)
    }
  }

  internal suspend fun prepareForEmailCode(
    inProgressSignIn: SignIn,
    factor: Factor,
    isSecondFactor: Boolean,
    onError: (String) -> Unit,
  ) {
    val emailAddressId = factor.emailAddressId
    if (emailAddressId == null) {
      ClerkLog.e("Error preparing for email code: emailAddressId is null")
      return
    }

    if (isSecondFactor) {
      prepareSecondFactorEmailCode(inProgressSignIn, emailAddressId, onError)
    } else {
      prepareFirstFactorEmailCode(inProgressSignIn, emailAddressId, onError)
    }
  }

  private suspend fun prepareSecondFactorPhoneCode(
    inProgressSignIn: SignIn,
    phoneNumberId: String,
    onError: (String) -> Unit,
  ) {
    val isSupported =
      isFactorStrategySupported(
        factors = inProgressSignIn.supportedSecondFactors,
        strategy = SignIn.PrepareSecondFactorParams.PHONE_CODE,
      )
    if (!isSupported) {
      ClerkLog.e("Error preparing for phone code: strategy no longer supported for second factor")
      onError("Selected sign-in method is no longer available.")
    } else {
      inProgressSignIn
        .prepareSecondFactor(phoneNumberId)
        .onSuccess { ClerkLog.v("Successfully prepared second factor for phone code") }
        .onFailure { ClerkLog.e("Error preparing second factor for phone code: $it") }
    }
  }

  private suspend fun prepareFirstFactorPhoneCode(
    inProgressSignIn: SignIn,
    phoneNumberId: String,
    onError: (String) -> Unit,
  ) {
    val isSupported =
      isFactorStrategySupported(
        factors = inProgressSignIn.supportedFirstFactors,
        strategy = StrategyKeys.PHONE_CODE,
      )
    if (!isSupported) {
      ClerkLog.e("Error preparing for phone code: strategy no longer supported for first factor")
      onError("Selected sign-in method is no longer available.")
    } else {
      inProgressSignIn
        .prepareFirstFactor(
          SignIn.PrepareFirstFactorParams.PhoneCode(phoneNumberId = phoneNumberId)
        )
        .onFailure {
          onError(it.errorMessage)
          ClerkLog.e("Error preparing for phone code: $it")
        }
    }
  }

  private suspend fun prepareSecondFactorEmailCode(
    inProgressSignIn: SignIn,
    emailAddressId: String,
    onError: (String) -> Unit,
  ) {
    val isSupported =
      isFactorStrategySupported(
        factors = inProgressSignIn.supportedSecondFactors,
        strategy = SignIn.PrepareSecondFactorParams.EMAIL_CODE,
      )
    if (!isSupported) {
      ClerkLog.e("Error preparing for email code: strategy no longer supported for second factor")
      onError("Selected sign-in method is no longer available.")
    } else {
      inProgressSignIn
        .prepareSecondFactor(emailAddressId = emailAddressId)
        .onSuccess { ClerkLog.v("Successfully prepared second factor for email code") }
        .onFailure { ClerkLog.e("Error preparing second factor for email code: $it") }
    }
  }

  private suspend fun prepareFirstFactorEmailCode(
    inProgressSignIn: SignIn,
    emailAddressId: String,
    onError: (String) -> Unit,
  ) {
    val isSupported =
      isFactorStrategySupported(
        factors = inProgressSignIn.supportedFirstFactors,
        strategy = StrategyKeys.EMAIL_CODE,
      )
    if (!isSupported) {
      ClerkLog.e("Error preparing for email code: strategy no longer supported for first factor")
      onError("Selected sign-in method is no longer available.")
    } else {
      inProgressSignIn
        .prepareFirstFactor(
          SignIn.PrepareFirstFactorParams.EmailCode(emailAddressId = emailAddressId)
        )
        .onSuccess { ClerkLog.v("Successfully prepared for email code: $it") }
        .onFailure {
          onError(it.errorMessage)
          ClerkLog.e("Error preparing for email code: ${it.errorMessage}")
        }
    }
  }

  private fun isFactorStrategySupported(factors: List<Factor>?, strategy: String): Boolean {
    return factors.isNullOrEmpty() || factors.any { it.strategy == strategy }
  }
}
