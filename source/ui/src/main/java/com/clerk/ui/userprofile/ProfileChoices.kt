package com.clerk.ui.userprofile

import com.clerk.api.*
import kotlinx.serialization.json.JsonPrimitive

/** Choices shown by the profile forms; all mutations and validation remain in the core. */
internal val User.availablePhoneNumbersForMfa: List<PhoneNumber>
  get() = phoneNumbers.filter { phone ->
    phone.verification.status == VerificationStatus.Verified &&
      !phone.reservedForSecondFactor &&
      (!username.isNullOrBlank() ||
        emailAddresses.any { it.verification.status == VerificationStatus.Verified } ||
        phoneNumbers.any {
          it.id != phone.id &&
            !it.reservedForSecondFactor &&
            it.verification.status == VerificationStatus.Verified
        })
  }

internal val Clerk.unconnectedProviders: List<OAuthProvider>
  get() {
    val connected = user?.verifiedExternalAccounts.orEmpty().map { it.provider.rawValue }.toSet()
    return environment.userSettings.social.values
      .filter { it.enabled }
      .map {
        OAuthProvider.fromJson(
          JsonPrimitive(it.strategy.rawValue.removePrefix("oauth_")),
          context.requireRuntime(),
        )
      }
      .filter { it.rawValue !in connected }
  }
