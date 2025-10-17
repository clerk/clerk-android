package com.clerk.ui.signin.code

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.clerk.api.Clerk
import com.clerk.api.network.model.factor.Factor
import com.clerk.ui.R
import com.clerk.ui.core.common.StrategyKeys

internal object SignInFactorCodeHelper {
  internal fun getShowResendValue(verificationState: VerificationState): Boolean =
    when (verificationState) {
      VerificationState.Default,
      is VerificationState.Error -> true
      VerificationState.Verifying,
      VerificationState.Success -> false
    }

  internal fun showResend(factor: Factor, verificationState: VerificationState): Boolean {
    return when (factor.strategy) {
      StrategyKeys.TOTP -> false
      else -> getShowResendValue(verificationState)
    }
  }

  internal fun showUseAnotherMethod(factor: Factor): Boolean {
    return when (factor.strategy) {
      StrategyKeys.RESET_PASSWORD_EMAIL_CODE,
      StrategyKeys.RESET_PASSWORD_PHONE_CODE -> false
      else -> true
    }
  }

  @Composable
  internal fun titleForStrategy(factor: Factor): String {
    return when (factor.strategy) {
      StrategyKeys.EMAIL_CODE -> "Check your email"
      StrategyKeys.PHONE_CODE -> "Check your phone"
      StrategyKeys.RESET_PASSWORD_EMAIL_CODE,
      StrategyKeys.RESET_PASSWORD_PHONE_CODE -> "Reset password"
      StrategyKeys.TOTP -> "Two-step verification"
      else -> ""
    }
  }

  @Composable
  internal fun subtitleForStrategy(factor: Factor): String {
    return when (factor.strategy) {
      StrategyKeys.RESET_PASSWORD_EMAIL_CODE ->
        stringResource(R.string.first_enter_the_code_sent_to_your_email_address)
      StrategyKeys.RESET_PASSWORD_PHONE_CODE ->
        stringResource(R.string.first_enter_the_code_sent_to_your_phone)
      StrategyKeys.TOTP ->
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

  @Composable
  internal fun resendString(remainingSeconds: Int): String {
    return if (remainingSeconds > 0) {
      stringResource(R.string.resend_with_pararm, remainingSeconds)
    } else {
      stringResource(R.string.resend)
    }
  }
}
