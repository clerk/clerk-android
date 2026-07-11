package com.clerk.api.billing

import com.clerk.api.network.model.billing.BillingStoreProduct
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import kotlinx.serialization.json.jsonPrimitive

/**
 * Errors that can occur during a Clerk Billing in-app purchase flow.
 *
 * Billing operations return `ClerkResult<T, BillingError>`; pattern match on the failure's error to
 * render the appropriate UI:
 * ```kotlin
 * when (val result = Clerk.billing.purchase(activity, plan)) {
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
   * The plan has no Google Play product mapped that matches the request. When [productId] or
   * [purchaseOptionId] were passed to [Billing.purchase], no mapping matched them exactly;
   * otherwise the plan has no Google Play mapping at all. Map the store product (and its base plan)
   * to the plan in the Clerk Dashboard.
   *
   * @property planId The identifier of the plan.
   * @property productId The requested store product identifier, when one was passed.
   * @property purchaseOptionId The requested purchase option (base plan) identifier, when one was
   *   passed.
   */
  data class ProductNotMapped(
    val planId: String,
    val productId: String? = null,
    val purchaseOptionId: String? = null,
  ) : BillingError

  /**
   * The request matches more than one Google Play product mapped to the plan, so the SDK cannot
   * choose one automatically. Pass `productId` (and `purchaseOptionId` when one product maps
   * multiple base plans) to [Billing.purchase] to select among [candidates].
   *
   * @property planId The identifier of the plan.
   * @property candidates The Google Play mappings that matched the request.
   */
  data class AmbiguousStoreProduct(
    val planId: String,
    val candidates: List<BillingStoreProduct>,
  ) : BillingError

  /**
   * The mapped Google Play product could not be loaded from the Play Store. Verify the product
   * exists in the Play Console and is active.
   *
   * @property productId The store product identifier that could not be found.
   */
  data class ProductNotFound(val productId: String) : BillingError

  /**
   * The user already holds an active paid subscription managed by a different payment processor
   * (e.g. a Stripe subscription purchased on the web, or an Apple App Store subscription). Suppress
   * the purchase and surface the existing subscription instead — users must cancel it where it was
   * purchased.
   *
   * An active subscription managed by Google Play is not an error: purchasing another plan through
   * [Billing.purchase] performs an in-app plan change that supersedes it.
   *
   * @property processor The processor that manages the existing subscription (e.g. `stripe` or
   *   `apple`), when reported by the API.
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
 * Preflight rejection codes the backend states definitively: proceeding would charge the user for a
 * purchase that registration is certain to reject.
 */
private val DEFINITIVE_PREFLIGHT_REJECTION_CODES =
  setOf(
    "product_not_mapped",
    "store_connection_not_configured",
    "form_param_missing",
    "form_param_value_invalid",
  )

/** The lower bound of the HTTP server-error status range. */
private const val HTTP_INTERNAL_SERVER_ERROR = 500

/**
 * Maps a failed `POST /me/billing/store_purchases` result to a [BillingError].
 *
 * An `already_subscribed` error code becomes [BillingError.AlreadySubscribedVia] (reading the
 * managing processor from the response meta when present); everything else becomes
 * [BillingError.ServerRejected].
 */
internal fun ClerkResult.Failure<ClerkErrorResponse>.toBillingError(): BillingError =
  alreadySubscribedViaOrNull() ?: BillingError.ServerRejected(error = error, code = code)

/**
 * Maps a failed `POST /me/billing/store_purchases/preflight` result to the [BillingError] that
 * should block the purchase, or `null` when the purchase should proceed anyway.
 *
 * Only verdicts the backend states definitively block the flow: `already_subscribed` (the payer's
 * active subscription is managed by a different processor) becomes
 * [BillingError.AlreadySubscribedVia], and 4xx rejections that registration is certain to repeat
 * (unmapped product, unconfigured store connection, invalid parameters) become
 * [BillingError.ServerRejected]. Everything else — network failures, 5xx responses, FAPI
 * deployments that predate the preflight endpoint — returns `null`: registration after the store
 * transaction remains the authoritative guard, the preflight is an optimization.
 */
internal fun ClerkResult.Failure<ClerkErrorResponse>.toPreflightBlockOrNull(): BillingError? {
  val status = code
  if (status != null && status >= HTTP_INTERNAL_SERVER_ERROR) return null
  val definitive = error?.errors.orEmpty().any { it.code in DEFINITIVE_PREFLIGHT_REJECTION_CODES }
  return alreadySubscribedViaOrNull()
    ?: BillingError.ServerRejected(error = error, code = status).takeIf { definitive }
}

/**
 * Returns [BillingError.AlreadySubscribedVia] when the failure carries an `already_subscribed`
 * error code (reading the managing processor from the response meta when present), or `null`
 * otherwise.
 */
private fun ClerkResult.Failure<ClerkErrorResponse>.alreadySubscribedViaOrNull():
  BillingError.AlreadySubscribedVia? {
  val alreadySubscribed = error?.errors.orEmpty().any { it.code == ERROR_CODE_ALREADY_SUBSCRIBED }
  if (!alreadySubscribed) return null
  val processor =
    runCatching { error?.meta?.get(META_KEY_ALREADY_SUBSCRIBED_VIA)?.jsonPrimitive?.content }
      .getOrNull()
  return BillingError.AlreadySubscribedVia(processor)
}
