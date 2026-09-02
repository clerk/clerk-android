package com.clerk.api.billing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The current status of a billing statement.
 *
 * Matches clerk-js `BillingStatementStatus`.
 */
@Serializable
enum class BillingStatementStatus {
  @SerialName("open") OPEN,
  @SerialName("closed") CLOSED,
  @SerialName("unknown") UNKNOWN,
}

/**
 * Total costs, taxes, and other pricing details for a statement.
 *
 * @property subtotal The price of the items or Plan before taxes, credits, or discounts are
 *   applied.
 * @property grandTotal The total amount, including taxes and after credits/discounts are applied.
 * @property taxTotal The amount of tax included in the statement.
 */
@Serializable
data class BillingStatementTotals(
  val subtotal: BillingMoneyAmount,
  val grandTotal: BillingMoneyAmount,
  val taxTotal: BillingMoneyAmount,
)

/**
 * A group of payment items within a statement.
 *
 * @property id The unique identifier for the statement group, when present.
 * @property timestamp The date and time when this group of payment items was created or last
 *   updated, as Unix milliseconds.
 * @property items Payment resources that belong to this group.
 */
@Serializable
data class BillingStatementGroup(
  val id: String? = null,
  val timestamp: Long,
  val items: List<BillingPayment> = emptyList(),
)

/**
 * A billing statement for a user or Organization.
 *
 * @property id The unique identifier for the statement.
 * @property totals Financial totals for the statement, including subtotal, grand total, and tax
 *   total.
 * @property status The current status of the statement. Statements can be `open` (still
 *   accumulating charges) or `closed` (finalized).
 * @property timestamp The date and time when the statement was created or last updated, as Unix
 *   milliseconds.
 * @property groups Statement groups, where each group contains payment items organized by
 *   timestamp.
 */
@Serializable
data class BillingStatement(
  val id: String,
  val totals: BillingStatementTotals,
  val status: BillingStatementStatus = BillingStatementStatus.UNKNOWN,
  val timestamp: Long,
  val groups: List<BillingStatementGroup> = emptyList(),
)
