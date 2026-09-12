package com.clerk.api.billing

import kotlinx.serialization.Serializable

/**
 * A credit ledger entry for the current payer or a given Organization.
 *
 * @property id The ID of the credit ledger entry.
 * @property amount The amount of the credit ledger entry.
 * @property sourceType The type of the source of the credit ledger entry.
 * @property sourceId The ID of the source of the credit ledger entry.
 * @property createdAt The date when the credit ledger entry was created, as Unix milliseconds.
 */
@Serializable
data class BillingCreditLedger(
  val id: String,
  val amount: BillingMoneyAmount,
  val sourceType: String,
  val sourceId: String,
  val createdAt: Long,
)
