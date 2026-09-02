package com.clerk.api.billing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A monetary value with currency information.
 *
 * @property amount The raw amount, usually in the smallest unit of the currency (like cents for
 *   USD). For example, `1000` for $10.00.
 * @property amountFormatted The amount as a formatted string. For example, `10.00` for $10.00.
 * @property currency The ISO currency code for this amount. For example, `USD`.
 * @property currencySymbol The symbol for the currency. For example, `$`.
 */
@Serializable
data class BillingMoneyAmount(
  val amount: Int,
  val amountFormatted: String,
  val currency: String,
  val currencySymbol: String,
)

/**
 * Whether a catalog discount subtracts a percentage or a fixed amount.
 *
 * Matches clerk-js `BillingAppliedDiscount.effect`.
 */
@Serializable
enum class BillingDiscountEffect {
  @SerialName("percentage") PERCENTAGE,
  @SerialName("fixed_amount") FIXED_AMOUNT,
  @SerialName("unknown") UNKNOWN,
}

/**
 * How a discount was applied to a subscription item.
 *
 * Matches clerk-js `BillingDiscountRedemption.source`.
 */
@Serializable
enum class BillingDiscountSource {
  @SerialName("promotion") PROMOTION,
  @SerialName("manual") MANUAL,
  @SerialName("promo_code") PROMO_CODE,
  @SerialName("unknown") UNKNOWN,
}

/**
 * The current status of a discount redemption.
 *
 * Matches clerk-js `BillingDiscountRedemption.status`.
 */
@Serializable
enum class BillingDiscountRedemptionStatus {
  @SerialName("active") ACTIVE,
  @SerialName("exhausted") EXHAUSTED,
  @SerialName("removed") REMOVED,
  @SerialName("unknown") UNKNOWN,
}

/**
 * The cost breakdown for a single pricing tier in totals.
 *
 * @property quantity The quantity billed within this tier. `null` means unlimited.
 * @property feePerBlock The fee charged per block for this tier.
 * @property total The total billed amount for this tier.
 */
@Serializable
data class BillingPerUnitTotalTier(
  val quantity: Int? = null,
  val feePerBlock: BillingMoneyAmount,
  val total: BillingMoneyAmount,
)

/**
 * Per-unit cost breakdown in totals (for example, seats).
 *
 * @property name The unit name, for example `seats`.
 * @property blockSize The number of units represented by one billable block.
 * @property tiers The tier breakdown for this unit total.
 */
@Serializable
data class BillingPerUnitTotal(
  val name: String,
  val blockSize: Int,
  val tiers: List<BillingPerUnitTotalTier> = emptyList(),
)

/**
 * Details about a proration credit, including the remaining portion of the billing cycle.
 *
 * @property amount The monetary value of the proration credit.
 * @property cycleDaysRemaining The number of days remaining in the current billing cycle.
 * @property cycleDaysTotal The total number of days in the billing cycle.
 * @property cycleRemainingPercent The percentage of the billing cycle that remains.
 */
@Serializable
data class BillingProrationCreditDetail(
  val amount: BillingMoneyAmount,
  val cycleDaysRemaining: Int,
  val cycleDaysTotal: Int,
  val cycleRemainingPercent: Double,
)

/**
 * The payer's available credit and the amount applied to a transaction.
 *
 * @property remainingBalance The payer's credit balance remaining after the transaction.
 * @property appliedAmount The amount of payer credit applied to the transaction.
 */
@Serializable
data class BillingPayerCredit(
  val remainingBalance: BillingMoneyAmount,
  val appliedAmount: BillingMoneyAmount,
)

/**
 * Credits applied to a checkout, payment, or subscription item.
 *
 * @property proration The credit for the unused portion of the current billing cycle. `null` when
 *   no proration credit applies.
 * @property payer The payer credit applied to the transaction. `null` when no payer credit applies.
 * @property total The total monetary value of all credits applied to the transaction.
 */
@Serializable
data class BillingCredits(
  val proration: BillingProrationCreditDetail? = null,
  val payer: BillingPayerCredit? = null,
  val total: BillingMoneyAmount,
)

/**
 * A prorated discount applied when adding a seat mid-cycle.
 *
 * @property amount The amount of the proration discount.
 * @property cycleDaysPassed The number of days that have already passed in the billing cycle.
 * @property cycleDaysTotal The total number of days in the billing cycle.
 * @property cyclePassedPercent The percentage of the billing cycle that has passed.
 */
@Serializable
data class BillingProrationDiscount(
  val amount: BillingMoneyAmount,
  val cycleDaysPassed: Int,
  val cycleDaysTotal: Int,
  val cyclePassedPercent: Double,
)

/**
 * A catalog discount applied to a checkout or payment.
 *
 * @property amount The monetary value of the discount applied to the transaction.
 * @property discountId The unique identifier of the discount.
 * @property name The display name of the discount.
 * @property effect Whether the discount subtracts a percentage or a fixed amount.
 * @property percentOff The percentage deducted when [effect] is [BillingDiscountEffect.PERCENTAGE].
 * @property amountOff The configured fixed amount off when [effect] is
 *   [BillingDiscountEffect.FIXED_AMOUNT].
 * @property promoCode The promotion code used to apply the discount.
 * @property cyclesRemaining The number of billing cycles for which the discount remains active.
 *   `null` means the discount does not expire after a fixed number of cycles.
 * @property durationInCycles The originally configured duration in billing cycles. `null` means the
 *   discount does not expire after a fixed number of cycles.
 */
@Serializable
data class BillingAppliedDiscount(
  val amount: BillingMoneyAmount,
  val discountId: String,
  val name: String,
  val effect: BillingDiscountEffect = BillingDiscountEffect.UNKNOWN,
  val percentOff: Int? = null,
  val amountOff: BillingMoneyAmount? = null,
  val promoCode: String? = null,
  val cyclesRemaining: Int? = null,
  val durationInCycles: Int? = null,
)

/**
 * A discount redemption applied to a subscription item.
 *
 * @property id The unique identifier of the discount redemption.
 * @property subscriptionItemId The unique identifier of the subscription item receiving the
 *   discount.
 * @property discountId The unique identifier of the redeemed discount.
 * @property name The display name of the discount.
 * @property source How the discount was applied to the subscription item.
 * @property promoCode The promotion code used to redeem the discount.
 * @property effect Whether the discount subtracts a percentage or a fixed amount.
 * @property percentOff The percentage deducted when [effect] is [BillingDiscountEffect.PERCENTAGE].
 * @property amountOff The configured fixed amount off when [effect] is
 *   [BillingDiscountEffect.FIXED_AMOUNT].
 * @property amount The monetary value of the discount applied to the subscription item.
 * @property cyclesRemaining The number of billing cycles for which the discount remains active.
 *   `null` means the discount does not expire after a fixed number of cycles.
 * @property cyclesApplied The number of billing cycles to which the discount has already been
 *   applied.
 * @property status The current status of the discount redemption.
 * @property redeemedAt The date and time when the discount was redeemed, as Unix milliseconds.
 * @property redeemedBy The identifier of the user who redeemed the discount. `null` if no user was
 *   recorded.
 */
@Serializable
data class BillingDiscountRedemption(
  val id: String,
  val subscriptionItemId: String,
  val discountId: String,
  val name: String,
  val source: BillingDiscountSource = BillingDiscountSource.UNKNOWN,
  val promoCode: String? = null,
  val effect: BillingDiscountEffect? = null,
  val percentOff: Int? = null,
  val amountOff: BillingMoneyAmount? = null,
  val amount: BillingMoneyAmount? = null,
  val cyclesRemaining: Int? = null,
  val cyclesApplied: Int,
  val status: BillingDiscountRedemptionStatus? = null,
  val redeemedAt: Long,
  val redeemedBy: String? = null,
)

/**
 * Discounts applied to a checkout or payment, such as prorated discounts for mid-cycle seat
 * additions.
 *
 * @property proration The prorated discount for the part of the billing period that has already
 *   passed when adding a seat mid-cycle. `null` when no proration discount applies.
 * @property discount The catalog discount applied to the transaction. Omitted when no catalog
 *   discount applies.
 * @property total The total of all discounts applied.
 */
@Serializable
data class BillingDiscounts(
  val proration: BillingProrationDiscount? = null,
  val discount: BillingAppliedDiscount? = null,
  val total: BillingMoneyAmount,
)

/**
 * Per-period renewal totals after a checkout, including all seats and the base plan fee.
 *
 * @property subtotal The subtotal for the billing period.
 * @property baseFee The base fee for the billing period.
 * @property taxTotal The tax total for the billing period.
 * @property grandTotal The grand total for the billing period.
 * @property perUnitTotals Per-unit cost breakdown for the renewal period.
 */
@Serializable
data class BillingPeriodTotals(
  val subtotal: BillingMoneyAmount,
  val baseFee: BillingMoneyAmount,
  val taxTotal: BillingMoneyAmount,
  val grandTotal: BillingMoneyAmount,
  val perUnitTotals: List<BillingPerUnitTotal>? = null,
)

/**
 * A granular breakdown of the total amount that will be charged, either during checkout or at
 * renewal.
 *
 * @property subtotal Subtotal before adjustments.
 * @property baseFee Base fee component before per-unit charges and adjustments.
 * @property taxTotal Total tax amount.
 * @property grandTotal Grand total amount.
 * @property totalDueAfterFreeTrial Total amount due after a free trial ends.
 * @property credit Legacy credit amount.
 * @property credits Unified credits breakdown.
 * @property discounts Information about the discounts applied to the payment.
 * @property pastDue Past due amount.
 * @property totalDueNow Total amount due now.
 * @property perUnitTotals Per-unit total breakdown (for example, seats).
 * @property totalsDuePerPeriod Per-period renewal totals.
 * @property totalDuePerPeriod The expected total payment for each future billing period.
 */
@Serializable
data class BillingTotals(
  val subtotal: BillingMoneyAmount,
  val baseFee: BillingMoneyAmount? = null,
  val taxTotal: BillingMoneyAmount,
  val grandTotal: BillingMoneyAmount,
  val totalDueAfterFreeTrial: BillingMoneyAmount? = null,
  val credit: BillingMoneyAmount? = null,
  val credits: BillingCredits? = null,
  val discounts: BillingDiscounts? = null,
  val pastDue: BillingMoneyAmount? = null,
  val totalDueNow: BillingMoneyAmount? = null,
  val perUnitTotals: List<BillingPerUnitTotal>? = null,
  val totalsDuePerPeriod: BillingPeriodTotals? = null,
  val totalDuePerPeriod: BillingMoneyAmount? = null,
)
