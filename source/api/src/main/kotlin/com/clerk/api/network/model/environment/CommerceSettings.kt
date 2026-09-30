package com.clerk.api.network.model.environment

import kotlinx.serialization.Serializable

/** The Billing settings from the Clerk Dashboard. */
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
