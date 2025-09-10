package com.clerk.ui.signin.code

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.prepareFirstFactor
import com.clerk.api.signin.prepareSecondFactor
import com.clerk.ui.core.common.StrategyKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SignInFactorCodeViewModel : ViewModel() {

  private val _state: MutableStateFlow<State> = MutableStateFlow(State.Idle)
  val state = _state.asStateFlow()

  fun prepare(factor: Factor, isSecondFactor: Boolean) {
    val inProgressSignIn = Clerk.signIn ?: error("No sign in in progress")
    viewModelScope.launch {
      when (factor.strategy) {
        StrategyKeys.EMAIL_CODE -> prepareForEmailCode(inProgressSignIn, factor)
        StrategyKeys.PHONE_CODE ->
          prepareForPhoneCode(
            inProgressSignIn = inProgressSignIn,
            factor = factor,
            isSecondFactor = isSecondFactor,
          )

        StrategyKeys.RESET_PASSWORD_PHONE_CODE ->
          prepareForResetPasswordWithPhone(inProgressSignIn, factor)

        StrategyKeys.RESET_PASSWORD_EMAIL_CODE ->
          prepareForResetWithEmailCode(inProgressSignIn, factor)
      }
    }
  }

  private suspend fun prepareForResetWithEmailCode(inProgressSignIn: SignIn, factor: Factor) {
    inProgressSignIn
      .prepareFirstFactor(
        SignIn.PrepareFirstFactorParams.ResetPasswordEmailCode(
          emailAddressId = factor.emailAddressId!!
        )
      )
      .onSuccess {}
      .onFailure {}
  }

  private suspend fun prepareForResetPasswordWithPhone(inProgressSignIn: SignIn, factor: Factor) {
    inProgressSignIn
      .prepareFirstFactor(
        SignIn.PrepareFirstFactorParams.ResetPasswordPhoneCode(
          phoneNumberId = factor.phoneNumberId!!
        )
      )
      .onSuccess {}
      .onFailure {}
  }

  private suspend fun prepareForPhoneCode(
    inProgressSignIn: SignIn,
    factor: Factor,
    isSecondFactor: Boolean,
  ) {
    if (isSecondFactor) {
      inProgressSignIn.prepareSecondFactor(factor.phoneNumberId).onSuccess {}.onFailure {}
    } else {
      inProgressSignIn
        .prepareFirstFactor(
          SignIn.PrepareFirstFactorParams.PhoneCode(phoneNumberId = factor.phoneNumberId!!)
        )
        .onSuccess {}
        .onFailure {}
    }
  }

  private suspend fun prepareForEmailCode(inProgressSignIn: SignIn, factor: Factor) {
    inProgressSignIn.prepareFirstFactor(
      SignIn.PrepareFirstFactorParams.EmailCode(emailAddressId = factor.emailAddressId!!)
    )
  }

  sealed interface State {
    object Idle : State

    object Verifying : State

    object Error : State

    object Success : State
  }
}
