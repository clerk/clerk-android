package com.clerk.api.network.model.billing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A subscription plan configured for a Clerk instance.
 *
 * Plans are part of Clerk Billing's unified catalog: a single plan can be purchasable via Stripe on
 * the web and/or via in-app purchase on mobile. When a plan has been mapped to store products in
 * the Clerk Dashboard, [storeProducts] contains the store-specific product identifiers the SDK
 * should request from Google Play Billing (or StoreKit on iOS).
 */
@Serializable
data class BillingPlan(
  /** The unique identifier of the plan. */
  val id: String,

  /** The display name of the plan. */
  val name: String,

  /** The URL-friendly identifier of the plan. */
  val slug: String,

  /** An optional description of the plan. */
  val description: String? = null,

  /** The monthly fee for the plan. `null` when the plan is annual-only. */
  val fee: BillingMoney? = null,

  /** The total annual fee for the plan. `null` when the plan is monthly-only. */
  @SerialName("annual_fee") val annualFee: BillingMoney? = null,

  /**
   * The effective monthly fee when the plan is billed annually. `null` when the plan is
   * monthly-only.
   */
  @SerialName("annual_monthly_fee") val annualMonthlyFee: BillingMoney? = null,

  /** Whether this is the instance's default plan. */
  @SerialName("is_default") val isDefault: Boolean = false,

  /** Whether the plan renews on a recurring basis. */
  @SerialName("is_recurring") val isRecurring: Boolean = false,

  /** Whether the plan is publicly visible (e.g. shown in pricing tables). */
  @SerialName("publicly_visible") val publiclyVisible: Boolean = false,

  /** Whether the plan charges a base fee. */
  @SerialName("has_base_fee") val hasBaseFee: Boolean = false,

  /** The payer type the plan is sold to (`user` or `org`). IAP plans are always `user` plans. */
  @SerialName("for_payer_type") val forPayerType: String? = null,

  /** An optional avatar image URL for the plan. */
  @SerialName("avatar_url") val avatarUrl: String? = null,

  /** Whether the plan offers a free trial. */
  @SerialName("free_trial_enabled") val freeTrialEnabled: Boolean = false,

  /** The number of free trial days, when [freeTrialEnabled] is `true`. */
  @SerialName("free_trial_days") val freeTrialDays: Int? = null,

  /** The features included in the plan. */
  val features: List<BillingFeature> = emptyList(),

  /**
   * The app-store products mapped to this plan in the Clerk Dashboard. Empty when the plan is not
   * purchasable via in-app purchase.
   */
  @SerialName("store_products") val storeProducts: List<BillingStoreProduct> = emptyList(),
)

/** A monetary amount as reported by the Clerk API. */
@Serializable
data class BillingMoney(
  /** The amount in the currency's minor unit (e.g. cents). */
  val amount: Long,

  /** The amount formatted for display (e.g. "9.99"). */
  @SerialName("amount_formatted") val amountFormatted: String,

  /** The ISO 4217 currency code (e.g. "USD"). */
  val currency: String,

  /** The currency symbol (e.g. "$"). */
  @SerialName("currency_symbol") val currencySymbol: String,
)

/** A feature included in a [BillingPlan]. */
@Serializable
data class BillingFeature(
  /** The unique identifier of the feature. */
  val id: String,

  /** The display name of the feature. */
  val name: String,

  /** The URL-friendly identifier of the feature. Also surfaced in the session token. */
  val slug: String,

  /** An optional description of the feature. */
  val description: String? = null,

  /** An optional avatar image URL for the feature. */
  @SerialName("avatar_url") val avatarUrl: String? = null,
)

/**
 * An app-store purchase identity mapped to a [BillingPlan].
 *
 * A plan can map any number of store products per store; the store's own product configuration
 * (purchase option and renewal term) governs billing. On Google Play a mapping identifies both the
 * subscription product and the exact base plan to buy via [purchaseOptionId].
 */
@Serializable
data class BillingStoreProduct(
  /** The store the product is sold through. */
  val store: BillingStore = BillingStore.UNKNOWN,

  /** The store-specific product identifier (e.g. `com.acme.pro`). */
  @SerialName("product_id") val productId: String,

  /**
   * The store-specific purchase option identifier — on Google Play, the ID of the base plan to buy
   * (e.g. `monthly`). `null` when the store does not model purchase options.
   */
  @SerialName("purchase_option_id") val purchaseOptionId: String? = null,
)

/** The app store a [BillingStoreProduct] is sold through. */
@Serializable
enum class BillingStore {
  @SerialName("apple") APPLE,
  @SerialName("google") GOOGLE,
  @SerialName("unknown") UNKNOWN,
}

/** The billing period of a plan price or store product. */
@Serializable
enum class BillingPlanPeriod {
  @SerialName("month") MONTH,
  @SerialName("annual") ANNUAL,
  @SerialName("unknown") UNKNOWN,
}
