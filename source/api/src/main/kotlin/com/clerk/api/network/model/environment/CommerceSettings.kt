package com.clerk.api.network.model.environment

import kotlinx.serialization.Serializable

/** The Billing settings from the Clerk Dashboard. */
@Serializable
public data class CommerceSettings(val billing: Billing = Billing()) {
  @Serializable
  public data class Billing(
    val stripePublishableKey: String? = null,
    val organization: Payer = Payer(),
    val user: Payer = Payer(),
  ) {
    @Serializable
    public data class Payer(val enabled: Boolean = false, val hasPaidPlans: Boolean = false)
  }
}
