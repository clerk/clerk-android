package com.clerk.api.signin

import com.clerk.api.Clerk
import com.clerk.api.network.model.environment.PreferredSignInStrategy
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.network.model.factor.FactorComparators
import com.clerk.api.network.model.factor.isResetFactor

/**
 * Retrieves a list of alternative first factors for the current sign-in attempt, excluding the
 * specified factor and certain strategy types.
 *
 * This function filters the [SignIn.supportedFirstFactors] to provide alternative options for
 * first-factor authentication. It excludes the currently provided [factor], reset factors, OAuth,
 * Enterprise SSO, and SAML strategies. The remaining factors are then sorted based on a predefined
 * order, with unknown strategies placed at the end.
 *
 * @param factor The [Factor] to exclude from the list of alternatives.
 * @return A [List] of alternative [Factor] objects, sorted by preference. Returns an empty list if
 *   no suitable alternatives are found or if [SignIn.supportedFirstFactors] is null.
 */
fun SignIn.alternativeFirstFactors(factor: Factor? = null): List<Factor> {
  val firstFactors =
    supportedFirstFactors?.filter {
      it != factor &&
        !it.isResetFactor() &&
        !it.strategy.contains("oauth") &&
        it.strategy != "enterprise_sso" &&
        it.strategy != "saml"
    }
  return (firstFactors ?: emptyList()).sortedWith(FactorComparators.allStrategiesButtonsComparator)
}

/**
 * Returns a list of alternative second factors, sorted by a predefined order, excluding the
 * provided factor.
 *
 * This function filters the [SignIn.supportedSecondFactors] to remove the specified [factor] and
 * then sorts the remaining factors based on the `strategySortOrderBackupCodePref` list. Factors not
 * found in the sort order list are placed at the end.
 *
 * @param factor The factor to exclude from the returned list.
 * @return A list of alternative second factors, sorted according to the predefined order.
 */
fun SignIn.alternativeSecondFactors(factor: Factor): List<Factor> {
  return supportedSecondFactors
    ?.filter { it != factor }
    .orEmpty()
    .sortedWith(comparator = FactorComparators.backupCodePrefComparator)
}

/**
 * Determines the starting first factor for a sign-in attempt based on the preferred sign-in
 * strategy.
 *
 * This property inspects the `preferredSignInStrategy` from the Clerk environment's display
 * configuration.
 * - If the preferred strategy is `PASSWORD`, it returns the result of
 *   [factorWhenPasswordIsPreferred].
 * - Otherwise (implying an OTP-based preference), it returns the result of
 *   [factorWhenOtpIsPreferred].
 *
 * @return The [Factor] to be presented as the initial first factor, or `null` if no suitable factor
 *   is found.
 */
val SignIn.startingFirstFactor: Factor?
  get() =
    when (Clerk.environment.displayConfig?.preferredSignInStrategy) {
      PreferredSignInStrategy.PASSWORD -> this.factorWhenPasswordIsPreferred
      else -> this.factorWhenOtpIsPreferred
    }

val SignIn.startingSecondFactor: Factor?
  get() {
    supportedSecondFactors
      ?.firstOrNull { it.strategy == "totp" }
      ?.let {
        return it
      }
    supportedSecondFactors
      ?.firstOrNull { it.strategy == "phone_code" }
      ?.let {
        return it
      }
    return supportedSecondFactors?.firstOrNull()
  }

private val SignIn.factorWhenPasswordIsPreferred: Factor?
  get() {
    // email links are not supported on iOS (keeping the same exclusion here)
    val availableFirstFactors =
      supportedFirstFactors?.filter { it.strategy != "email_link" } ?: return null

    // Prefer passkey
    availableFirstFactors
      .firstOrNull { it.strategy == "passkey" }
      ?.let {
        return it
      }

    // Then password
    availableFirstFactors
      .firstOrNull { it.strategy == "password" }
      ?.let {
        return it
      }

    // Then: sort by password-pref comparator, but first try to match current identifier
    val sorted = availableFirstFactors.sortedWith(FactorComparators.passwordPrefComparator)
    return availableFirstFactors.firstOrNull { it.safeIdentifier == identifier }
      ?: sorted.firstOrNull()
  }

private val SignIn.factorWhenOtpIsPreferred: Factor?
  get() {
    // email links are not supported on iOS (keeping the same exclusion here)
    val availableFirstFactors =
      supportedFirstFactors?.filter { it.strategy != "email_link" } ?: return null

    // Prefer passkey
    availableFirstFactors
      .firstOrNull { it.strategy == "passkey" }
      ?.let {
        return it
      }

    // Then: sort by OTP-pref comparator; prefer matching identifier if present
    val sorted = availableFirstFactors.sortedWith(FactorComparators.otpPrefComparator)
    return sorted.firstOrNull { it.safeIdentifier == identifier } ?: sorted.firstOrNull()
  }
