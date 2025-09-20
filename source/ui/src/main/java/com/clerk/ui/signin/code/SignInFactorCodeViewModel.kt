package com.clerk.ui.signin.code

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.factor.Factor
import com.clerk.ui.core.common.StrategyKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class SignInFactorCodeViewModel(
  private val attemptHandler: SignInAttemptHandler = SignInAttemptHandler(),
  private val prepareHandler: SignInPrepareHandler = SignInPrepareHandler(),
) : ViewModel() {

  private val _state: MutableStateFlow<State> = MutableStateFlow(State.Idle)
  val state = _state.asStateFlow()

  fun prepare(factor: Factor, isSecondFactor: Boolean) {
    _state.value = State.Verifying
    val inProgressSignIn = Clerk.signIn ?: error("No sign in in progress")
    viewModelScope.launch {
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

  fun attempt(factor: Factor, isSecondFactor: Boolean, code: String) {
    _state.value = State.Verifying
    val inProgressSignIn = Clerk.signIn ?: error("No sign in in progress")
    viewModelScope.launch {
      val onSuccessCallback = { _state.value = State.Success }
      val onErrorCallback = { _: ClerkErrorResponse? -> _state.value = State.Error }

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

  sealed interface State {
    object Idle : State

    object Verifying : State

    object Error :
      State // State.Error does not take parameters, so ClerkError list is ignored for now

    object Success : State
  }
}
