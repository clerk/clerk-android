package com.clerk.ui.signin.code

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.signin.SignIn
import com.clerk.ui.auth.AuthenticationViewState
import com.clerk.ui.auth.VerificationUiState
import com.clerk.ui.auth.guardSignIn
import com.clerk.ui.core.common.StrategyKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class SignInFactorCodeViewModel(
  private val attemptHandler: SignInAttemptHandler = SignInAttemptHandler(),
  private val prepareHandler: SignInPrepareHandler = SignInPrepareHandler(),
) : ViewModel() {

  private val _verificationUiState = MutableStateFlow<VerificationUiState>(VerificationUiState.Idle)
  val verificationUiState = _verificationUiState.asStateFlow()

  private val _state: MutableStateFlow<AuthenticationViewState> =
    MutableStateFlow(AuthenticationViewState.Idle)
  val state = _state.asStateFlow()

  fun prepare(factor: Factor, isSecondFactor: Boolean) {
    _state.value = AuthenticationViewState.Loading

    guardSignIn(_state) { inProgressSignIn ->
      viewModelScope.launch(Dispatchers.IO) {
        if (shouldRerouteForUnsupportedFactor(inProgressSignIn, factor, isSecondFactor)) {
          _state.value = AuthenticationViewState.Success.SignIn(inProgressSignIn)
          return@launch
        }

        when (factor.strategy) {
          StrategyKeys.EMAIL_CODE -> {
            prepareHandler.prepareForEmailCode(inProgressSignIn, factor, isSecondFactor) {
              _state.value = AuthenticationViewState.Error(it)
            }
          }
          StrategyKeys.PHONE_CODE ->
            prepareHandler.prepareForPhoneCode(
              inProgressSignIn = inProgressSignIn,
              factor = factor,
              isSecondFactor = isSecondFactor,
            ) {
              _state.value = AuthenticationViewState.Error(it)
            }

          StrategyKeys.RESET_PASSWORD_PHONE_CODE ->
            prepareHandler.prepareForResetPasswordWithPhone(inProgressSignIn, factor) {
              _state.value = AuthenticationViewState.Error(it)
            }

          StrategyKeys.RESET_PASSWORD_EMAIL_CODE ->
            prepareHandler.prepareForResetWithEmailCode(inProgressSignIn, factor) {
              _state.value = AuthenticationViewState.Error(it)
            }
        }
      }
    }
  }

  fun attempt(factor: Factor, isSecondFactor: Boolean, code: String) {
    _verificationUiState.value = VerificationUiState.Verifying
    guardSignIn(_state) { inProgressSignIn ->
      _state.value = AuthenticationViewState.Loading
      val onSuccessCallback = { signIn: SignIn ->
        _verificationUiState.value = VerificationUiState.Verified
        _state.value = AuthenticationViewState.Success.SignIn(signIn)
      }
      val onErrorCallback = { message: String? ->
        _verificationUiState.value = VerificationUiState.Error(message)
        _state.value = AuthenticationViewState.Error(message)
      }

      viewModelScope.launch(Dispatchers.IO) {
        when (factor.strategy) {
          StrategyKeys.EMAIL_CODE ->
            attemptHandler.attemptEmailCode(
              inProgressSignIn = inProgressSignIn,
              code = code,
              isSecondFactor = isSecondFactor,
              onSuccessCallback = onSuccessCallback,
              onErrorCallback = onErrorCallback,
            )

          StrategyKeys.PHONE_CODE ->
            attemptHandler.attemptFirstFactorPhoneCode(
              inProgressSignIn = inProgressSignIn,
              code = code,
              isSecondFactor = isSecondFactor,
              onSuccessCallback = onSuccessCallback,
              onErrorCallback = onErrorCallback,
            )

          StrategyKeys.RESET_PASSWORD_EMAIL_CODE ->
            attemptHandler.attemptResetForEmailCode(
              inProgressSignIn = inProgressSignIn,
              code = code,
              onSuccessCallback = onSuccessCallback,
              onErrorCallback = onErrorCallback,
            )

          StrategyKeys.RESET_PASSWORD_PHONE_CODE ->
            attemptHandler.attemptResetForPhoneCode(
              inProgressSignIn = inProgressSignIn,
              code = code,
              onSuccessCallback = onSuccessCallback,
              onErrorCallback = onErrorCallback,
            )

          StrategyKeys.TOTP ->
            attemptHandler.attemptForTotp(
              inProgressSignIn = inProgressSignIn,
              code = code,
              onSuccessCallback = onSuccessCallback,
              onErrorCallback = onErrorCallback,
            )

          else -> error("Unsupported strategy: ${factor.strategy}")
        }
      }
    }
  }

  fun resetState() {
    _state.value = AuthenticationViewState.Idle
  }

  fun resetVerificationState() {
    _verificationUiState.value = VerificationUiState.Idle
  }

  private fun shouldRerouteForUnsupportedFactor(
    signIn: SignIn,
    factor: Factor,
    isSecondFactor: Boolean,
  ): Boolean {
    val supportedFirstFactors = signIn.supportedFirstFactors.orEmpty()
    val prefersEmailLinkOverEmailCode =
      factor.strategy == StrategyKeys.EMAIL_CODE &&
        signIn.firstFactorVerification?.strategy != StrategyKeys.EMAIL_CODE &&
        signIn.shouldPreferEmailLink(
          supportedFirstFactors = supportedFirstFactors,
          fallbackFactor = factor,
        )

    return if (isSecondFactor) {
      signIn.supportedSecondFactors?.none { it.matches(factor) } == true
    } else {
      supportedFirstFactors.none { it.matches(factor) } || prefersEmailLinkOverEmailCode
    }
  }

  private fun SignIn.shouldPreferEmailLink(
    supportedFirstFactors: List<Factor>,
    fallbackFactor: Factor,
  ): Boolean {
    val hasEmailLink = supportedFirstFactors.any { it.strategy == StrategyKeys.EMAIL_LINK }
    if (!hasEmailLink) return false

    val isEmailIdentifier =
      fallbackFactor.emailAddressId != null ||
        fallbackFactor.safeIdentifier?.contains("@") == true ||
        identifier?.contains("@") == true ||
        supportedFirstFactors.any {
          (it.strategy == StrategyKeys.EMAIL_LINK || it.strategy == StrategyKeys.EMAIL_CODE) &&
            it.safeIdentifier?.contains("@") == true
        }
    return isEmailIdentifier
  }

  private fun Factor.matches(other: Factor): Boolean {
    if (strategy != other.strategy) return false

    return when {
      emailAddressId != null || other.emailAddressId != null ->
        emailAddressId == other.emailAddressId
      phoneNumberId != null || other.phoneNumberId != null -> phoneNumberId == other.phoneNumberId
      web3WalletId != null || other.web3WalletId != null -> web3WalletId == other.web3WalletId
      safeIdentifier != null || other.safeIdentifier != null ->
        safeIdentifier == other.safeIdentifier
      else -> true
    }
  }
}
