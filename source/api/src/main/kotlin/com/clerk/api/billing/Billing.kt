package com.clerk.api.billing

import android.app.Activity
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingFlowParams.SubscriptionUpdateParams.ReplacementMode
import com.android.billingclient.api.ProductDetails
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.billing.BillingPlan
import com.clerk.api.network.model.billing.BillingSubscriptionItem
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult

/**
 * Main Billing class providing Clerk Billing in-app purchase entry points.
 *
 * Access via `Clerk.billing`.
 *
 * Clerk Billing unifies web (Stripe) and in-app (Google Play) purchases under a single plan
 * catalog: plans are configured in the Clerk Dashboard and mapped to Google Play products, and
 * entitlements resolve identically regardless of purchase channel.
 *
 * The typical flow is:
 * 1. [fetchPlans] to load the plan catalog, including mapped store products.
 * 2. [loadProducts] to load Google Play pricing for display.
 * 3. [purchase] to launch the Google Play purchase flow for a plan.
 *
 * The SDK reports completed purchases to Clerk for server-side verification and acknowledgment,
 * then refreshes the session token so feature entitlements (`fea` claims) are live immediately.
 * Renewals and other out-of-band transactions delivered while the app is running are registered
 * automatically; [restorePurchases] covers reinstalls and new devices.
 *
 * ### Example usage:
 * ```kotlin
 * val plans = Clerk.billing.fetchPlans().successOrNull().orEmpty()
 * val plan = plans.first { it.slug == "pro" }
 *
 * when (val result = Clerk.billing.purchase(activity, plan)) {
 *     is ClerkResult.Success -> // Subscription active, entitlements refreshed
 *     is ClerkResult.Failure -> // Inspect result.error (a BillingError)
 * }
 * ```
 */
class Billing internal constructor() {

  private val service = PlayBillingService()

  /**
   * Fetches the plans purchasable by users on this instance.
   *
   * Each plan carries the Google Play products mapped to it in the Clerk Dashboard via
   * [BillingPlan.storeProducts].
   *
   * @return A [ClerkResult] containing the plans on success, or a [ClerkErrorResponse] on failure.
   */
  suspend fun fetchPlans(): ClerkResult<List<BillingPlan>, ClerkErrorResponse> =
    when (val result = ClerkApi.commerce.plans(payerType = PAYER_TYPE_USER)) {
      is ClerkResult.Success -> ClerkResult.success(result.value.data)
      is ClerkResult.Failure -> result
    }

  /**
   * Fetches the current user's subscription items.
   *
   * Check [BillingSubscriptionItem.managedBy] to render the correct manage UI: store-managed
   * subscriptions are canceled/updated in the store's subscription settings, not through Clerk.
   *
   * @return A [ClerkResult] containing the subscription items on success, or a [ClerkErrorResponse]
   *   on failure.
   */
  suspend fun fetchSubscriptionItems():
    ClerkResult<List<BillingSubscriptionItem>, ClerkErrorResponse> =
    when (val result = ClerkApi.commerce.subscriptionItems()) {
      is ClerkResult.Success -> ClerkResult.success(result.value.data)
      is ClerkResult.Failure -> result
    }

  /**
   * Loads Google Play [ProductDetails] for the store products mapped to the given [plans].
   *
   * Use the returned product details to display localized store pricing.
   *
   * @param plans The plans to load products for, as returned by [fetchPlans].
   * @return A [ClerkResult] containing the loaded product details on success, or a [BillingError]
   *   on failure.
   */
  suspend fun loadProducts(
    plans: List<BillingPlan>
  ): ClerkResult<List<ProductDetails>, BillingError> = service.loadProducts(plans)

  /**
   * Purchases the given [plan] through Google Play, as a fresh purchase or as a plan change.
   *
   * A plan can map any number of Google Play products, each mapping naming the exact base plan
   * (purchase option) to buy — see [BillingPlan.storeProducts]. With exactly one mapping no
   * selectors are needed; with several, pass [productId] (and [purchaseOptionId] when one product
   * maps multiple base plans) to choose. When no mapping matches, the call fails with
   * [BillingError.ProductNotMapped]; when several match, it fails with
   * [BillingError.AmbiguousStoreProduct] carrying the candidates to choose from.
   *
   * Launches the Google Play purchase flow for the resolved product and base plan, stamped with a
   * deterministic obfuscated account identifier (the SHA-256 hex digest of the Clerk user ID,
   * truncated to 64 characters) so Clerk can bind the transaction to the user. The completed
   * purchase is posted to Clerk, which verifies and acknowledges it server-side and activates the
   * subscription; the session token is then refreshed so new feature entitlements are live
   * immediately.
   *
   * ### Plan changes
   *
   * When the user already holds an active subscription managed by Google Play, purchasing another
   * plan (or another base plan of the same product) is an in-app plan change: the purchase flow is
   * launched as a Google Play subscription update that supersedes the current purchase, and Clerk
   * swaps the subscription item when the new purchase is registered. How Google Play charges the
   * switch is controlled by [replacementMode].
   *
   * [BillingError.AlreadySubscribedVia] is returned only when the existing active subscription is
   * managed by a *different* processor (e.g. a Stripe subscription purchased on the web, or an
   * Apple App Store subscription); such purchases are blocked before the payment sheet opens, and
   * users must manage that subscription where it was purchased.
   *
   * @param activity The foreground activity used to launch the Google Play purchase dialog.
   * @param plan The plan to purchase, as returned by [fetchPlans].
   * @param productId Selects a product when the plan has more than one Google Play mapping.
   * @param purchaseOptionId Selects the exact Google Play base plan when one product maps multiple
   *   base plans.
   * @param replacementMode How Google Play charges a plan change, as a
   *   [BillingFlowParams.SubscriptionUpdateParams.ReplacementMode] constant; ignored for fresh
   *   purchases. Defaults to [ReplacementMode.CHARGE_PRORATED_PRICE], Google's recommended mode for
   *   upgrades (the price difference is charged immediately, prorated for the remaining period).
   *   For downgrades prefer [ReplacementMode.DEFERRED] (the change takes effect at the next
   *   renewal) or [ReplacementMode.WITHOUT_PRORATION] (the new price is charged at the next
   *   renewal, the plan changes immediately).
   * @return A [ClerkResult] containing the activated [BillingSubscriptionItem] on success, or a
   *   [BillingError] on failure.
   */
  suspend fun purchase(
    activity: Activity,
    plan: BillingPlan,
    productId: String? = null,
    purchaseOptionId: String? = null,
    replacementMode: Int = ReplacementMode.CHARGE_PRORATED_PRICE,
  ): ClerkResult<BillingSubscriptionItem, BillingError> =
    service.purchase(activity, plan, productId, purchaseOptionId, replacementMode)

  /**
   * Restores the user's Google Play subscriptions.
   *
   * Queries the user's current subscriptions from Google Play and registers each active purchase
   * with Clerk. Registration is idempotent server-side, so purchases Clerk already knows about
   * simply return their current subscription item.
   *
   * @return A [ClerkResult] containing the restored subscription items (possibly empty) on success,
   *   or a [BillingError] when nothing could be restored.
   */
  suspend fun restorePurchases(): ClerkResult<List<BillingSubscriptionItem>, BillingError> =
    service.restorePurchases()

  private companion object {
    /** IAP is restricted to user payers; org payers remain web-only. */
    const val PAYER_TYPE_USER = "user"
  }
}
