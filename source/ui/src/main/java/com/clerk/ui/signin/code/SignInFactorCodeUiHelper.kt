package com.clerk.ui.signin.code

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.clerk.api.Clerk
import com.clerk.api.auth.types.Strategy
import com.clerk.api.network.model.factor.Factor
import com.clerk.ui.R

internal object SignInFactorCodeUiHelper {
  internal fun getShowResendValue(verificationState: VerificationState): Boolean =
    when (verificationState) {
      VerificationState.Default,
      is VerificationState.Error -> true
      VerificationState.Verifying,
      VerificationState.Success -> false
    }

  internal fun showResend(factor: Factor, verificationState: VerificationState): Boolean {
    return when (factor.strategyType) {
      Strategy.Totp -> false
      else -> getShowResendValue(verificationState)
    }
  }

  internal fun showUseAnotherMethod(factor: Factor): Boolean {
    return when (factor.strategyType) {
      Strategy.ResetPasswordEmailCode,
      Strategy.ResetPasswordPhoneCode -> false
      else -> true
    }
  }

  @Composable
  internal fun titleForStrategy(factor: Factor): String {
    return when (factor.strategyType) {
      Strategy.EmailCode -> stringResource(R.string.check_your_email)
      Strategy.PhoneCode -> stringResource(R.string.check_your_phone)
      Strategy.ResetPasswordEmailCode,
      Strategy.ResetPasswordPhoneCode -> stringResource(R.string.reset_password)
      Strategy.Totp -> stringResource(R.string.two_step_verification)
      else -> ""
    }
  }

  @Composable
  internal fun subtitleForStrategy(factor: Factor): String {
    return when (factor.strategyType) {
      Strategy.ResetPasswordEmailCode ->
        stringResource(R.string.first_enter_the_code_sent_to_your_email_address)
      Strategy.ResetPasswordPhoneCode ->
        stringResource(R.string.first_enter_the_code_sent_to_your_phone)
      Strategy.Totp ->
        stringResource(
          R.string
            .to_continue_please_enter_the_verification_code_generated_by_your_authenticator_app
        )
      else -> {
        Clerk.applicationName?.let { stringResource(R.string.to_continue_to, it) }
          ?: stringResource(R.string.to_continue)
      }
    }
  }
}
