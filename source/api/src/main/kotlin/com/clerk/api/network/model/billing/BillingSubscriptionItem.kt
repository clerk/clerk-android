package com.clerk.api.network.model.billing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A single plan subscription belonging to the current user.
 *
 * Subscription items may be managed by Clerk (purchased via Stripe on the web) or by an app store
 * (purchased via in-app purchase). Store-managed items ([managedBy] is [BillingManagedBy.APPLE] or
 * [BillingManagedBy.GOOGLE]) cannot be canceled or updated through Clerk — users manage them in the
 * store's subscription settings.
 */
@Serializable
data class BillingSubscriptionItem(
  /** The unique identifier of the subscription item. */
  val id: String,

  /** The current status of the subscription item. */
  val status: Status = Status.UNKNOWN,

  /** The identifier of the plan this item subscribes to. */
  @SerialName("plan_id") val planId: String? = null,

  /** The plan this item subscribes to, when included by the API. */
  val plan: BillingPlan? = null,

  /** The billing period of the subscription. */
  @SerialName("plan_period") val planPeriod: BillingPlanPeriod = BillingPlanPeriod.UNKNOWN,

  /** Which system owns the subscription lifecycle. Defaults to [BillingManagedBy.CLERK]. */
  @SerialName("managed_by") val managedBy: BillingManagedBy = BillingManagedBy.CLERK,

  /** The Unix timestamp (milliseconds) at which the current billing period started. */
  @SerialName("period_start") val periodStart: Long = 0L,

  /**
   * The Unix timestamp (milliseconds) at which the current billing period ends, or `null` when the
   * item has no period end.
   */
  @SerialName("period_end") val periodEnd: Long? = null,

  /**
   * The Unix timestamp (milliseconds) at which the subscription was canceled, or `null` when it has
   * not been canceled.
   */
  @SerialName("canceled_at") val canceledAt: Long? = null,

  /**
   * The Unix timestamp (milliseconds) at which the subscription became past due, or `null` when it
   * is not past due.
   */
  @SerialName("past_due_at") val pastDueAt: Long? = null,

  /**
   * The Unix timestamp (milliseconds) at which the subscription ended, or `null` when it has not
   * ended.
   */
  @SerialName("ended_at") val endedAt: Long? = null,

  /** Whether the subscription is currently in a free trial. */
  @SerialName("is_free_trial") val isFreeTrial: Boolean = false,

  /** The identifier of the payer that owns the subscription. */
  @SerialName("payer_id") val payerId: String? = null,

  /** The Unix timestamp (milliseconds) at which the item was created. */
  @SerialName("created_at") val createdAt: Long? = null,

  /** The Unix timestamp (milliseconds) at which the item was last updated. */
  @SerialName("updated_at") val updatedAt: Long? = null,
) {
  /** The lifecycle status of a subscription item. */
  @Serializable
  enum class Status {
    @SerialName("abandoned") ABANDONED,
    @SerialName("active") ACTIVE,
    @SerialName("canceled") CANCELED,
    @SerialName("ended") ENDED,
    @SerialName("expired") EXPIRED,
    @SerialName("incomplete") INCOMPLETE,
    @SerialName("past_due") PAST_DUE,
    @SerialName("upcoming") UPCOMING,
    @SerialName("unknown") UNKNOWN,
  }
}

/** The system that owns a subscription item's lifecycle. */
@Serializable
enum class BillingManagedBy {
  /** The subscription is managed by Clerk's own billing engine (e.g. Stripe-backed). */
  @SerialName("clerk") CLERK,

  /** The subscription is managed by the Apple App Store. */
  @SerialName("apple") APPLE,

  /** The subscription is managed by Google Play. */
  @SerialName("google") GOOGLE,
}
