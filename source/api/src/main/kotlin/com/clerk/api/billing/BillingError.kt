package com.clerk.api.billing

import com.clerk.api.network.model.billing.BillingPlanPeriod
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import kotlinx.serialization.json.jsonPrimitive

/**
 * Errors that can occur during a Clerk Billing in-app purchase flow.
 *
 * Billing operations return `ClerkResult<T, BillingError>`; pattern match on the failure's error to
 * render the appropriate UI:
 * ```kotlin
 * when (val result = Clerk.billing.purchase(activity, plan, period)) {
 *     is ClerkResult.Success -> showSubscription(result.value)
 *     is ClerkResult.Failure ->
 *         when (val error = result.error) {
 *             is BillingError.UserCanceled -> Unit // no-op
 *             is BillingError.AlreadySubscribedVia -> showManageExistingSubscription(error)
 *             else -> showError(error)
 *         }
 * }
 * ```
 */
sealed interface BillingError {
  /** The user dismissed the Google Play purchase dialog. */
  data object UserCanceled : BillingError

  /**
   * The purchase requires further action before it completes (e.g. ask-to-buy approval or a pending
   * payment method). Once Google Play finalizes the purchase, the SDK's purchase listener registers
   * it with Clerk automatically.
   */
  data object PurchasePending : BillingError

  /**
   * Google Play reports the user already owns this subscription. Call [Billing.restorePurchases] to
   * register the existing purchase with Clerk.
   */
  data object ItemAlreadyOwned : BillingError

  /** Another purchase launched through the SDK is still in flight. */
  data object PurchaseInProgress : BillingError

  /** There is no signed-in user to bind the purchase to. */
  data object NotSignedIn : BillingError

  /**
   * Google Play Billing is unavailable on this device, or a connection to the Play Store could not
   * be established.
   *
   * @property responseCode The Play Billing response code, when one was reported.
   * @property debugMessage A developer-facing message describing the failure.
   */
  data class BillingUnavailable(val responseCode: Int? = null, val debugMessage: String? = null) :
    BillingError

  /**
   * Google Play returned an unexpected error.
   *
   * @property responseCode The Play Billing response code.
   * @property debugMessage A developer-facing message describing the failure.
   */
  data class PlayStoreError(val responseCode: Int, val debugMessage: String? = null) : BillingError

  /**
   * The plan has no Google Play product mapped for the requested billing period. Map a store
   * product to the plan in the Clerk Dashboard.
   *
   * @property planId The identifier of the plan.
   * @property period The billing period that has no mapped product.
   */
  data class ProductNotMapped(val planId: String, val period: BillingPlanPeriod) : BillingError

  /**
   * The mapped Google Play product could not be loaded from the Play Store. Verify the product
   * exists in the Play Console and is active.
   *
   * @property productId The store product identifier that could not be found.
   */
  data class ProductNotFound(val productId: String) : BillingError

  /**
   * The user already holds an active subscription for this plan through another payment processor
   * (e.g. a Stripe subscription purchased on the web). Suppress the purchase and surface the
   * existing subscription instead — users must cancel it where it was purchased.
   *
   * @property processor The processor that manages the existing subscription (e.g. `stripe`), when
   *   reported by the API.
   */
  data class AlreadySubscribedVia(val processor: String? = null) : BillingError

  /**
   * Clerk rejected the purchase registration (e.g. the purchase token failed verification or the
   * product is not mapped to a plan).
   *
   * @property error The Clerk API error response, when one was returned.
   * @property code The HTTP status code, when the failure was an HTTP failure.
   */
  data class ServerRejected(val error: ClerkErrorResponse? = null, val code: Int? = null) :
    BillingError
}

/** The error code Clerk returns when the payer already subscribes via another processor. */
internal const val ERROR_CODE_ALREADY_SUBSCRIBED = "already_subscribed"

/** The meta key carrying the processor that manages the conflicting subscription. */
internal const val META_KEY_ALREADY_SUBSCRIBED_VIA = "already_subscribed_via"

/**
 * Maps a failed `POST /me/commerce/store_purchases` result to a [BillingError].
 *
 * An `already_subscribed` error code becomes [BillingError.AlreadySubscribedVia] (reading the
 * managing processor from the response meta when present); everything else becomes
 * [BillingError.ServerRejected].
 */
internal fun ClerkResult.Failure<ClerkErrorResponse>.toBillingError(): BillingError {
  val alreadySubscribed =
    error?.errors.orEmpty().any { it.code == ERROR_CODE_ALREADY_SUBSCRIBED }
  if (alreadySubscribed) {
    val processor =
      runCatching { error?.meta?.get(META_KEY_ALREADY_SUBSCRIBED_VIA)?.jsonPrimitive?.content }
        .getOrNull()
    return BillingError.AlreadySubscribedVia(processor)
  }
  return BillingError.ServerRejected(error = error, code = code)
}
