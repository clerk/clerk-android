package com.clerk.ui.reverification

import com.clerk.api.auth.types.Strategy
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.SessionVerification

internal enum class ReverificationStage {
  FIRST_FACTOR,
  SECOND_FACTOR;

  val level: SessionVerification.Level
    get() =
      when (this) {
        FIRST_FACTOR -> SessionVerification.Level.FIRST_FACTOR
        SECOND_FACTOR -> SessionVerification.Level.SECOND_FACTOR
      }
}

internal enum class PasswordResetReason {
  PWNED,
  COMPROMISED;

  companion object {
    fun of(failure: ClerkResult.Failure<ClerkErrorResponse>): PasswordResetReason? =
      when (failure.error?.errors?.firstOrNull()?.code) {
        "form_password_pwned" -> PWNED
        "form_password_compromised" -> COMPROMISED
        else -> null
      }
  }
}

internal sealed interface ReverificationDestination {
  data class FactorEntry(val factor: Factor, val stage: ReverificationStage) :
    ReverificationDestination

  data class AlternativeMethods(val currentFactor: Factor, val stage: ReverificationStage) :
    ReverificationDestination

  data class NewPassword(val resetFactor: Factor, val reason: PasswordResetReason) :
    ReverificationDestination

  data object GetHelp : ReverificationDestination
}

internal object ReverificationFactors {
  private val firstFactorOrder =
    listOf(
      Strategy.Passkey,
      Strategy.TrustedDevice,
      Strategy.EmailCode,
      Strategy.PhoneCode,
      Strategy.Password,
    )

  private val passwordPreferredFirstFactorOrder =
    listOf(
      Strategy.Passkey,
      Strategy.TrustedDevice,
      Strategy.Password,
      Strategy.EmailCode,
      Strategy.PhoneCode,
    )

  private val secondFactorOrder =
    listOf(
      Strategy.Passkey,
      Strategy.TrustedDevice,
      Strategy.Totp,
      Strategy.PhoneCode,
      Strategy.BackupCode,
    )

  private fun order(stage: ReverificationStage): List<Strategy> =
    when (stage) {
      ReverificationStage.FIRST_FACTOR -> firstFactorOrder
      ReverificationStage.SECOND_FACTOR -> secondFactorOrder
    }

  fun usable(
    factors: List<Factor>,
    stage: ReverificationStage,
    hasLocalBiometricCredential: Boolean,
  ): List<Factor> {
    val order = order(stage)
    return factors
      .filter { factor ->
        when (factor.strategyType) {
          !in order -> false
          Strategy.TrustedDevice -> hasLocalBiometricCredential
          Strategy.EmailCode,
          Strategy.PhoneCode -> factor.safeIdentifier != null
          else -> true
        }
      }
      .sortedWith(
        compareBy<Factor> { order.indexOf(it.strategyType) }.thenBy { it.primary != true }
      )
  }

  fun startingFactor(
    usable: List<Factor>,
    stage: ReverificationStage,
    prefersPassword: Boolean,
  ): Factor? {
    val order =
      if (stage == ReverificationStage.FIRST_FACTOR && prefersPassword) {
        passwordPreferredFirstFactorOrder
      } else {
        order(stage)
      }
    return usable.minByOrNull { order.indexOf(it.strategyType) }
  }

  fun resetPasswordFactor(firstFactors: List<Factor>): Factor? =
    firstFactors.firstOrNull { it.strategyType == Strategy.ResetPasswordEmailCode }
      ?: firstFactors.firstOrNull { it.strategyType == Strategy.ResetPasswordPhoneCode }
}

internal val Factor.isResetPasswordFactor: Boolean
  get() =
    strategyType == Strategy.ResetPasswordEmailCode ||
      strategyType == Strategy.ResetPasswordPhoneCode
