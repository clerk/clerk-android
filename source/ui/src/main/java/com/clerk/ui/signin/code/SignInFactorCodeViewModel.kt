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
    guardSignIn(_state) { inProgressSignIn ->
      _state.value = AuthenticationViewState.Loading

      guardSignIn(_state) { inProgressSignIn ->
        viewModelScope.launch(Dispatchers.IO) {
          when (factor.strategy) {
            StrategyKeys.EMAIL_CODE -> prepareHandler.prepareForEmailCode(inProgressSignIn, factor)
            StrategyKeys.PHONE_CODE ->
              prepareHandler.prepareForPhoneCode(
                inProgressSignIn = inProgressSignIn,
                factor = factor,
                isSecondFactor = isSecondFactor,
              )

            StrategyKeys.RESET_PASSWORD_PHONE_CODE ->
              prepareHandler.prepareForResetPasswordWithPhone(inProgressSignIn, factor)

            StrategyKeys.RESET_PASSWORD_EMAIL_CODE ->
              prepareHandler.prepareForResetWithEmailCode(inProgressSignIn, factor)
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
            attemptHandler.attemptFirstFactorEmailCode(
              inProgressSignIn = inProgressSignIn,
              code = code,
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
}
