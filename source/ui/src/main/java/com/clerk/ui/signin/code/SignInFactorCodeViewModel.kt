package com.clerk.ui.signin.code

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.auth.types.Strategy
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.signin.SignIn
import com.clerk.ui.auth.AuthenticationViewState
import com.clerk.ui.auth.VerificationUiState
import com.clerk.ui.auth.guardSignIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class SignInFactorCodeViewModel(
  private val attemptHandler: SignInAttemptHandler = SignInAttemptHandler(),
  private val prepareHandler: SignInPrepareHandler = SignInPrepareHandler(),
  private val workDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

  private val _verificationUiState = MutableStateFlow<VerificationUiState>(VerificationUiState.Idle)
  val verificationUiState = _verificationUiState.asStateFlow()

  private val _state: MutableStateFlow<AuthenticationViewState> =
    MutableStateFlow(AuthenticationViewState.Idle)
  val state = _state.asStateFlow()

  fun prepare(factor: Factor, isSecondFactor: Boolean, forcePrepare: Boolean = false) {
    guardSignIn(_state) { inProgressSignIn ->
      val shouldReroute =
        shouldRerouteForUnsupportedFactor(inProgressSignIn, factor, isSecondFactor)
      if (
        !forcePrepare &&
          !shouldReroute &&
          inProgressSignIn.hasActiveVerification(factor, isSecondFactor)
      ) {
        return@guardSignIn
      }

      _state.value = AuthenticationViewState.Loading
      viewModelScope.launch(workDispatcher) {
        if (shouldReroute) {
          _state.value = AuthenticationViewState.Success.SignIn(inProgressSignIn)
          return@launch
        }

        when (factor.strategyType) {
          Strategy.EmailCode -> {
            prepareHandler.prepareForEmailCode(inProgressSignIn, factor, isSecondFactor) {
              _state.value = AuthenticationViewState.Error(it)
            }
          }
          Strategy.PhoneCode ->
            prepareHandler.prepareForPhoneCode(
              inProgressSignIn = inProgressSignIn,
              factor = factor,
              isSecondFactor = isSecondFactor,
            ) {
              _state.value = AuthenticationViewState.Error(it)
            }

          Strategy.ResetPasswordPhoneCode ->
            prepareHandler.prepareForResetPasswordWithPhone(inProgressSignIn, factor) {
              _state.value = AuthenticationViewState.Error(it)
            }

          Strategy.ResetPasswordEmailCode ->
            prepareHandler.prepareForResetWithEmailCode(inProgressSignIn, factor) {
              _state.value = AuthenticationViewState.Error(it)
            }

          // TOTP has nothing to prepare; other strategies never reach the code view.
          else -> Unit
        }
      }
    }
  }

  private fun SignIn.hasActiveVerification(factor: Factor, isSecondFactor: Boolean): Boolean {
    val verification = if (isSecondFactor) secondFactorVerification else firstFactorVerification
    return verification?.status == Verification.Status.UNVERIFIED &&
      verification.strategyType == factor.strategyType &&
      verification.expireAt?.let { it > System.currentTimeMillis() } == true
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

      viewModelScope.launch(workDispatcher) {
        when (factor.strategyType) {
          Strategy.EmailCode ->
            attemptHandler.attemptEmailCode(
              inProgressSignIn = inProgressSignIn,
              code = code,
              isSecondFactor = isSecondFactor,
              onSuccessCallback = onSuccessCallback,
              onErrorCallback = onErrorCallback,
            )

          Strategy.PhoneCode ->
            attemptHandler.attemptFirstFactorPhoneCode(
              inProgressSignIn = inProgressSignIn,
              code = code,
              isSecondFactor = isSecondFactor,
              onSuccessCallback = onSuccessCallback,
              onErrorCallback = onErrorCallback,
            )

          Strategy.ResetPasswordEmailCode ->
            attemptHandler.attemptResetForEmailCode(
              inProgressSignIn = inProgressSignIn,
              code = code,
              onSuccessCallback = onSuccessCallback,
              onErrorCallback = onErrorCallback,
            )

          Strategy.ResetPasswordPhoneCode ->
            attemptHandler.attemptResetForPhoneCode(
              inProgressSignIn = inProgressSignIn,
              code = code,
              onSuccessCallback = onSuccessCallback,
              onErrorCallback = onErrorCallback,
            )

          Strategy.Totp ->
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
    return if (isSecondFactor) {
      signIn.supportedSecondFactors?.none { it.matches(factor) } == true
    } else {
      signIn.supportedFirstFactors.orEmpty().none { it.matches(factor) }
    }
  }

  private fun Factor.matches(other: Factor): Boolean {
    if (strategyType != other.strategyType) return false

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
