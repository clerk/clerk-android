package com.clerk.ui.auth

import com.clerk.api.*
import kotlinx.serialization.Serializable

/**
 * Saved screen selection. The generated resource remains the owner of supported factors and
 * verification state.
 */
@Serializable
data class FactorSelection(
  val strategy: String,
  val emailAddressId: String? = null,
  val phoneNumberId: String? = null,
  val web3WalletId: String? = null,
  val enterpriseConnectionId: String? = null,
  val enterpriseConnectionName: String? = null,
  val safeIdentifier: String? = null,
  val primary: Boolean? = null,
  val default: Boolean? = null,
)

internal fun FactorSelection.isResetFactor() =
  strategy == "reset_password_email_code" || strategy == "reset_password_phone_code"

internal fun SignInFirstFactor.selection(): FactorSelection =
  when (this) {
    is SignInFirstFactor.Case1 ->
      FactorSelection(
        strategy,
        emailAddressId = value.emailAddressId,
        safeIdentifier = value.safeIdentifier,
        primary = value.primary,
      )
    is SignInFirstFactor.Case2 ->
      FactorSelection(
        strategy,
        emailAddressId = value.emailAddressId,
        safeIdentifier = value.safeIdentifier,
        primary = value.primary,
      )
    is SignInFirstFactor.Case3 ->
      FactorSelection(
        strategy,
        phoneNumberId = value.phoneNumberId,
        safeIdentifier = value.safeIdentifier,
        primary = value.primary,
        default = value.default,
      )
    is SignInFirstFactor.Case4 ->
      FactorSelection(strategy, web3WalletId = value.web3WalletId, primary = value.primary)
    is SignInFirstFactor.Case5 -> FactorSelection(strategy)
    is SignInFirstFactor.Case6 -> FactorSelection(strategy)
    is SignInFirstFactor.Case7 -> FactorSelection(strategy)
    is SignInFirstFactor.Case8 ->
      FactorSelection(
        strategy,
        enterpriseConnectionId = value.enterpriseConnectionId,
        enterpriseConnectionName = value.enterpriseConnectionName,
      )
    is SignInFirstFactor.Case9 ->
      FactorSelection(
        strategy,
        phoneNumberId = value.phoneNumberId,
        safeIdentifier = value.safeIdentifier,
        primary = value.primary,
      )
    is SignInFirstFactor.Case10 ->
      FactorSelection(
        strategy,
        emailAddressId = value.emailAddressId,
        safeIdentifier = value.safeIdentifier,
        primary = value.primary,
      )
  }

internal fun SignInSecondFactor.selection(): FactorSelection =
  when (this) {
    is SignInSecondFactor.Case1 ->
      FactorSelection(
        strategy,
        emailAddressId = value.emailAddressId,
        safeIdentifier = value.safeIdentifier,
        primary = value.primary,
      )
    is SignInSecondFactor.Case2 ->
      FactorSelection(
        strategy,
        emailAddressId = value.emailAddressId,
        safeIdentifier = value.safeIdentifier,
        primary = value.primary,
      )
    is SignInSecondFactor.Case3 ->
      FactorSelection(
        strategy,
        phoneNumberId = value.phoneNumberId,
        safeIdentifier = value.safeIdentifier,
        primary = value.primary,
        default = value.default,
      )
    is SignInSecondFactor.Case4 -> FactorSelection(strategy)
    is SignInSecondFactor.Case5 -> FactorSelection(strategy)
    is SignInSecondFactor.Case6 -> FactorSelection(strategy)
  }

internal val SignIn.firstFactorChoices
  get() = supportedFirstFactors.map { it.selection() }
internal val SignIn.secondFactorChoices
  get() = supportedSecondFactors.map { it.selection() }

private fun List<FactorSelection>.ordered(order: List<String>) = sortedBy {
  order.indexOf(it.strategy).takeIf { it >= 0 } ?: Int.MAX_VALUE
}

private val otpOrder = listOf("email_link", "email_code", "phone_code", "passkey", "password")
private val passwordOrder = listOf("passkey", "password", "email_link", "email_code", "phone_code")
private val secondFactorOrder = listOf("passkey", "totp", "phone_code", "backup_code")

internal fun SignIn.startingFirstFactor(
  clerk: Clerk,
  fallbackIdentifier: String? = null,
): FactorSelection? {
  val choices = firstFactorChoices
  choices
    .firstOrNull { it.strategy == "passkey" }
    ?.let {
      return it
    }
  val prefersPassword =
    clerk.environment.displayConfig.preferredSignInStrategy == PreferredSignInStrategy.Password
  if (prefersPassword)
    choices
      .firstOrNull { it.strategy == "password" }
      ?.let {
        return it
      }
  val identifier = identifier?.takeIf { it.isNotBlank() } ?: fallbackIdentifier
  if (identifier?.contains("@") == true) {
    val matching = choices.firstOrNull {
      it.safeIdentifier == identifier && it.strategy in listOf("email_code", "email_link")
    }
    val emailLink =
      if (matching == null) choices.filter { it.strategy == "email_link" }.singleOrNull()
      else
        choices.firstOrNull {
          it.strategy == "email_link" &&
            (it.emailAddressId == matching.emailAddressId ||
              it.safeIdentifier == matching.safeIdentifier)
        }
    emailLink?.let {
      return it
    }
  }
  val sorted = choices.ordered(if (prefersPassword) passwordOrder else otpOrder)
  return sorted.firstOrNull { identifier != null && it.safeIdentifier == identifier }
    ?: sorted.firstOrNull()
}

internal val SignIn.startingSecondFactor: FactorSelection?
  get() = secondFactorChoices.let { choices ->
    listOf("passkey", "totp", "phone_code").firstNotNullOfOrNull { strategy ->
      choices.firstOrNull { it.strategy == strategy }
    } ?: choices.firstOrNull()
  }

internal fun SignIn.alternativeFirstFactors(
  factor: FactorSelection? = null
): List<FactorSelection> =
  firstFactorChoices
    .filter {
      it != factor &&
        !it.isResetFactor() &&
        !it.strategy.contains("oauth") &&
        it.strategy !in listOf("enterprise_sso", "saml")
    }
    .ordered(otpOrder)

internal fun SignIn.alternativeSecondFactors(factor: FactorSelection): List<FactorSelection> =
  secondFactorChoices.filter { it != factor }.ordered(secondFactorOrder)

internal val SignIn.resetPasswordFactor: FactorSelection?
  get() =
    firstFactorChoices.firstOrNull { it.isResetFactor() && it.safeIdentifier == identifier }
      ?: firstFactorChoices.firstOrNull { it.isResetFactor() }
