package com.clerk.api.billing

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * The status of a Billing Subscription or subscription item.
 *
 * Matches clerk-js `BillingSubscriptionStatus`. The top-level subscription is `active` or
 * `past_due`.
 */
@Serializable
enum class BillingSubscriptionStatus {
  @SerialName("active") ACTIVE,
  @SerialName("ended") ENDED,
  @SerialName("upcoming") UPCOMING,
  @SerialName("past_due") PAST_DUE,
  @SerialName("unknown") UNKNOWN,
}

/**
 * The billing period for a Plan.
 *
 * Matches clerk-js `BillingSubscriptionPlanPeriod`.
 */
@Serializable
enum class BillingSubscriptionPlanPeriod {
  @SerialName("month") MONTH,
  @SerialName("annual") ANNUAL,
  @SerialName("unknown") UNKNOWN,
}

/**
 * Seat entitlements attached to a subscription item.
 *
 * @property quantity The seat limit active while the parent subscription item was active. `null`
 *   means unlimited.
 * @property tiers The tier-level breakdown of seats for this subscription item.
 */
@Serializable
data class BillingSubscriptionItemSeats(
  val quantity: Int? = null,
  val tiers: List<BillingPerUnitTotalTier>? = null,
)

/**
 * Upcoming payment details for a subscription.
 *
 * @property amount The amount of the next payment.
 * @property date The date when the next payment is due, as Unix milliseconds.
 * @property perUnitTotals Per-unit cost breakdown for the next payment (for example, seats).
 * @property totals Full cost breakdown for the next payment.
 */
@Serializable
data class BillingSubscriptionNextPayment(
  val amount: BillingMoneyAmount,
  val date: Long,
  val perUnitTotals: List<BillingPerUnitTotal>? = null,
  val totals: BillingTotals? = null,
)

/**
 * Upcoming payment details for a subscription item.
 *
 * @property amount The amount of the next payment.
 * @property date The date when the next payment is due, as Unix milliseconds.
 * @property perUnitTotals Per-unit cost breakdown for the next payment (for example, seats).
 * @property totals Full cost breakdown for the next payment.
 */
@Serializable
data class BillingSubscriptionItemNextPayment(
  val amount: BillingMoneyAmount,
  val date: Long,
  val perUnitTotals: List<BillingPerUnitTotal>? = null,
  val totals: BillingTotals? = null,
)

/**
 * Credit from a previous purchase applied to a subscription item.
 *
 * @property amount The amount of credit being applied.
 */
@Serializable data class BillingSubscriptionItemCredit(val amount: BillingMoneyAmount)

/**
 * An item in a Billing Subscription.
 *
 * The clerk-js `cancel` write method is not ported.
 *
 * @property id The unique identifier for the subscription item.
 * @property plan The Plan associated with the subscription item.
 * @property planPeriod The billing period for the subscription item.
 * @property priceId The ID of the price that this subscription item is associated with.
 * @property status The status of the subscription item.
 * @property createdAt The date and time when the subscription item was created, as Unix
 *   milliseconds. `null` if FAPI omits it.
 * @property pastDueAt The date and time when the subscription item became past due, as Unix
 *   milliseconds. `null` if the subscription item is not past due.
 * @property periodStart The date and time when the current billing period starts, as Unix
 *   milliseconds. `null` if FAPI omits it.
 * @property periodEnd The date and time when the current billing period ends, as Unix milliseconds.
 *   `null` if not set.
 * @property canceledAt The date and time when the subscription item was canceled, as Unix
 *   milliseconds. `null` if the subscription item is not canceled.
 * @property amount The amount charged for the subscription item.
 * @property nextPayment Information about the next payment for this subscription item.
 * @property credit The credit from a previous purchase that is being applied to the subscription
 *   item.
 * @property credits The credits applied to this subscription item.
 * @property appliedDiscount The active discount applied to this subscription item.
 * @property seats Seat entitlement details for this subscription item. Only set for organization
 *   subscription items with seat-based billing.
 * @property isFreeTrial Whether the subscription item is for a free trial.
 */
@Serializable
data class BillingSubscriptionItem(
  val id: String,
  val plan: BillingPlan,
  val planPeriod: BillingSubscriptionPlanPeriod = BillingSubscriptionPlanPeriod.UNKNOWN,
  val priceId: String? = null,
  val status: BillingSubscriptionStatus = BillingSubscriptionStatus.UNKNOWN,
  @Serializable(with = ZeroMillisAsNullSerializer::class) val createdAt: Long? = null,
  val pastDueAt: Long? = null,
  @Serializable(with = ZeroMillisAsNullSerializer::class) val periodStart: Long? = null,
  val periodEnd: Long? = null,
  val canceledAt: Long? = null,
  val amount: BillingMoneyAmount? = null,
  val nextPayment: BillingSubscriptionItemNextPayment? = null,
  val credit: BillingSubscriptionItemCredit? = null,
  val credits: BillingCredits? = null,
  val appliedDiscount: BillingDiscountRedemption? = null,
  val seats: BillingSubscriptionItemSeats? = null,
  val isFreeTrial: Boolean = false,
)

/**
 * A subscription to a plan for the current user or an Organization.
 *
 * @property id The unique identifier for the subscription.
 * @property activeAt The date when the subscription became active, as Unix milliseconds. `null`
 *   when FAPI has not activated the subscription yet.
 * @property createdAt The date when the subscription was created, as Unix milliseconds.
 * @property nextPayment Information about the next payment, including the amount and the date it is
 *   due. `null` if there is no upcoming payment.
 * @property pastDueAt The date when the subscription became past due, as Unix milliseconds. `null`
 *   if the subscription is not past due.
 * @property status The current status of the subscription. Due to the free plan subscription item,
 *   the top-level subscription is `active` or `past_due`.
 * @property subscriptionItems The list of subscription items included in this subscription.
 * @property updatedAt The date when the subscription was last updated, as Unix milliseconds. `null`
 *   if it has not been updated.
 * @property eligibleForFreeTrial Whether the payer is eligible for a free trial.
 */
@Serializable
data class BillingSubscription(
  val id: String,
  val activeAt: Long? = null,
  val createdAt: Long,
  val nextPayment: BillingSubscriptionNextPayment? = null,
  val pastDueAt: Long? = null,
  val status: BillingSubscriptionStatus = BillingSubscriptionStatus.UNKNOWN,
  val subscriptionItems: List<BillingSubscriptionItem> = emptyList(),
  val updatedAt: Long? = null,
  val eligibleForFreeTrial: Boolean = false,
)

internal object ZeroMillisAsNullSerializer : KSerializer<Long?> {
  private val delegate = Long.serializer().nullable

  override val descriptor = delegate.descriptor

  override fun deserialize(decoder: Decoder): Long? {
    return decoder.decodeSerializableValue(delegate)?.takeUnless { it == 0L }
  }

  override fun serialize(encoder: Encoder, value: Long?) {
    encoder.encodeSerializableValue(delegate, value)
  }
}
