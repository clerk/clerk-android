package com.clerk.api.billing

import kotlinx.serialization.Serializable

/**
 * The credit balance for a payer.
 *
 * @property balance The balance of the credit, or `null` when no balance is available.
 */
@Serializable public data class BillingCreditBalance(val balance: BillingMoneyAmount? = null)
