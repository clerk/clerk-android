package com.clerk.api.billing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The type of charge a payment represents.
 *
 * Matches clerk-js `BillingPaymentChargeType`.
 */
@Serializable
enum class BillingPaymentChargeType {
  @SerialName("checkout") CHECKOUT,
  @SerialName("recurring") RECURRING,
  @SerialName("price_transition") PRICE_TRANSITION,
  @SerialName("unknown") UNKNOWN,
}

/**
 * The current status of a payment.
 *
 * Matches clerk-js `BillingPaymentStatus`.
 */
@Serializable
enum class BillingPaymentStatus {
  @SerialName("pending") PENDING,
  @SerialName("paid") PAID,
  @SerialName("failed") FAILED,
  @SerialName("unknown") UNKNOWN,
}

/**
 * Per-payment cost breakdown, including any base fee and per-unit (for example, seats) subtotals.
 *
 * @property subtotal The price of the items before taxes, credits, or discounts are applied.
 * @property grandTotal The total amount for the payment, including taxes and after
 *   credits/discounts are applied.
 * @property taxTotal The amount of tax included in the payment.
 * @property baseFee The flat base fee charged on top of any per-unit fees.
 * @property perUnitTotals Per-unit cost breakdown for this payment (for example, seats).
 * @property discounts Discounts applied to this payment such as mid-cycle prorated seat discounts.
 *   `null` when no discounts apply.
 */
@Serializable
data class BillingPaymentTotals(
  val subtotal: BillingMoneyAmount,
  val grandTotal: BillingMoneyAmount,
  val taxTotal: BillingMoneyAmount,
  val baseFee: BillingMoneyAmount? = null,
  val perUnitTotals: List<BillingPerUnitTotal>? = null,
  val discounts: BillingDiscounts? = null,
)

/**
 * A payment attempt for a user or Organization.
 *
 * @property id The unique identifier for the payment.
 * @property amount The amount of the payment.
 * @property paidAt The date and time when the payment was successfully completed, as Unix
 *   milliseconds.
 * @property failedAt The date and time when the payment failed, as Unix milliseconds.
 * @property updatedAt The date and time when the payment was last updated, as Unix milliseconds.
 * @property paymentMethod The payment method being used for the payment.
 * @property subscriptionItem The subscription item being paid for.
 * @property chargeType The type of charge this payment represents.
 * @property status The current status of the payment.
 * @property totals Per-payment breakdown with optional base fee and per-unit subtotals. Absent on
 *   older responses.
 */
@Serializable
data class BillingPayment(
  val id: String,
  val amount: BillingMoneyAmount,
  val paidAt: Long? = null,
  val failedAt: Long? = null,
  val updatedAt: Long,
  val paymentMethod: BillingPaymentMethod? = null,
  val subscriptionItem: BillingSubscriptionItem,
  val chargeType: BillingPaymentChargeType = BillingPaymentChargeType.UNKNOWN,
  val status: BillingPaymentStatus = BillingPaymentStatus.UNKNOWN,
  val totals: BillingPaymentTotals? = null,
)
