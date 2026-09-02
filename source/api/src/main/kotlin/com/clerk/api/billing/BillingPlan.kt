package com.clerk.api.billing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Specifies the subscriber type a Plan is designed for.
 *
 * Each Plan is exclusively created for either individual users or Organizations.
 */
@Serializable
enum class BillingPayerResourceType {
  @SerialName("org") ORG,
  @SerialName("user") USER,
}

/**
 * A single pricing tier for a unit type on a plan.
 *
 * @property id The unique identifier of the unit price tier.
 * @property startsAtBlock The first block number this tier applies to.
 * @property endsAfterBlock The final block this tier applies to. `null` means unlimited.
 * @property feePerBlock The fee charged for each block in this tier.
 */
@Serializable
data class BillingPlanUnitPriceTier(
  val id: String,
  val startsAtBlock: Int,
  val endsAfterBlock: Int? = null,
  val feePerBlock: BillingMoneyAmount,
)

/**
 * Unit pricing for a specific unit type (for example, seats) on a plan.
 *
 * @property name The unit name, for example `seats`.
 * @property blockSize Number of units represented by one billable block.
 * @property tiers Tiers that define how each block range is priced.
 */
@Serializable
data class BillingPlanUnitPrice(
  val name: String,
  val blockSize: Int,
  val tiers: List<BillingPlanUnitPriceTier> = emptyList(),
)

/**
 * A specific possible price for a Billing Plan.
 *
 * @property id The unique identifier of the price.
 * @property fee The monthly price, or `null` if the price is not monthly.
 * @property annualMonthlyFee The effective monthly price when billed annually, or `null` if the
 *   price is not annual.
 * @property isDefault Whether this price is the default price for the associated plan.
 * @property unitPrices The individual unit prices applicable to this price.
 */
@Serializable
data class BillingPlanPrice(
  val id: String,
  val fee: BillingMoneyAmount? = null,
  val annualMonthlyFee: BillingMoneyAmount? = null,
  val isDefault: Boolean,
  val unitPrices: List<BillingPlanUnitPrice>? = null,
)

/**
 * A Subscription Plan with its details.
 *
 * @property id The unique identifier for the Plan.
 * @property name The name of the Plan.
 * @property fee The monthly price of the Plan.
 * @property annualFee The annual price of the Plan, or `null` if the Plan is not annual.
 * @property annualMonthlyFee The effective monthly price when billed annually, or `null` if the
 *   Plan is not annual.
 * @property description A short description of what the Plan offers, or `null` if none is provided.
 * @property isDefault Whether the Plan is the default Plan.
 * @property isRecurring Whether the Plan is recurring.
 * @property hasBaseFee Whether the Plan has a base fee.
 * @property forPayerType Specifies the subscriber type this Plan is designed for.
 * @property publiclyVisible Whether the Plan is visible to the public.
 * @property slug The URL-friendly identifier of the Plan.
 * @property avatarUrl The URL of the Plan's avatar image, or `null` if not set.
 * @property features The Features the Plan offers.
 * @property unitPrices Per-unit pricing tiers for this Plan (for example, seats).
 * @property availablePrices Prices that can be used to check out for this plan, including
 *   non-default prices.
 * @property freeTrialDays The number of days of the free trial for the Plan. `null` if the Plan
 *   does not have a free trial.
 * @property freeTrialEnabled Whether the Plan has a free trial.
 */
@Serializable
data class BillingPlan(
  val id: String,
  val name: String,
  val fee: BillingMoneyAmount? = null,
  val annualFee: BillingMoneyAmount? = null,
  val annualMonthlyFee: BillingMoneyAmount? = null,
  val description: String? = null,
  val isDefault: Boolean,
  val isRecurring: Boolean,
  val hasBaseFee: Boolean,
  val forPayerType: BillingPayerResourceType,
  val publiclyVisible: Boolean,
  val slug: String,
  val avatarUrl: String? = null,
  val features: List<Feature> = emptyList(),
  val unitPrices: List<BillingPlanUnitPrice>? = null,
  val availablePrices: List<BillingPlanPrice>? = null,
  val freeTrialDays: Int? = null,
  val freeTrialEnabled: Boolean = false,
)
