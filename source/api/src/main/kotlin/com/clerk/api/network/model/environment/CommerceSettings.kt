package com.clerk.api.network.model.environment

import kotlinx.serialization.Serializable

/**
 * Billing configuration from `/v1/environment`.
 *
 * Mirrors clerk-js `CommerceSettingsResource`. Apps read this through
 * [com.clerk.api.Clerk.commerceSettings].
 */
@Serializable
data class CommerceSettings(val billing: Billing = Billing()) {
  @Serializable
  data class Billing(
    val stripePublishableKey: String? = null,
    val organization: Payer = Payer(),
    val user: Payer = Payer(),
  ) {
    @Serializable data class Payer(val enabled: Boolean = false, val hasPaidPlans: Boolean = false)
  }
}
