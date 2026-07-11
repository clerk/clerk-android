package com.clerk.api.billing

import android.app.Activity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingFlowParams.SubscriptionUpdateParams.ReplacementMode
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.clerk.api.Clerk
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.model.billing.BillingPlan
import com.clerk.api.network.model.billing.BillingStore
import com.clerk.api.network.model.billing.BillingStoreProduct
import com.clerk.api.network.model.billing.BillingSubscriptionItem
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.onFailure
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Internal service that talks to Google Play Billing.
 *
 * Responsibilities:
 * - Lazily connects a [BillingClient] and transparently reconnects after a disconnect.
 * - Loads [ProductDetails] for the store products mapped to Clerk plans.
 * - Launches the Google Play purchase flow, stamping the Clerk user binding via
 *   [ObfuscatedAccountId], and registers completed purchases with Clerk. Purchases are
 *   intentionally never acknowledged client-side — Clerk acknowledges server-side after verifying
 *   the purchase.
 * - Registers out-of-band purchases (renewals completed while backgrounded, ask-to-buy approvals,
 *   promo-code redemptions) delivered to the [PurchasesUpdatedListener] that is attached when the
 *   billing client is created.
 */
internal class PlayBillingService(
  private val registrar: StorePurchaseRegistrar = StorePurchaseRegistrar(),
  private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {

  /** Result of a connection attempt. */
  private sealed interface Connection {
    data class Ready(val client: BillingClient) : Connection

    data class Unavailable(val error: BillingError) : Connection
  }

  /** A purchases update delivered by Google Play. */
  private data class PurchasesUpdate(val result: BillingResult, val purchases: List<Purchase>)

  private val connectionMutex = Mutex()

  /** The lazily created billing client. Guarded by [connectionMutex] for creation. */
  @Volatile private var billingClient: BillingClient? = null

  /** The waiter for an in-flight purchase launched through [purchase], if any. */
  private val pendingPurchase = AtomicReference<CompletableDeferred<PurchasesUpdate>?>(null)

  /**
   * The ongoing purchases listener. Attached once when the billing client is created; updates that
   * don't belong to an in-flight [purchase] call are treated as out-of-band purchases and
   * registered with Clerk (the endpoint is idempotent server-side).
   */
  private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
    val waiter = pendingPurchase.getAndSet(null)
    when {
      waiter != null -> waiter.complete(PurchasesUpdate(result, purchases.orEmpty()))
      result.responseCode == BillingClient.BillingResponseCode.OK ->
        scope.launch { registerOutOfBandPurchases(purchases.orEmpty()) }
      else ->
        ClerkLog.w(
          "Ignoring out-of-band purchases update with code ${result.responseCode}: " +
            result.debugMessage
        )
    }
  }

  /** Loads the Google Play [ProductDetails] for the store products mapped to the given [plans]. */
  suspend fun loadProducts(
    plans: List<BillingPlan>
  ): ClerkResult<List<ProductDetails>, BillingError> {
    val productIds =
      plans
        .flatMap { plan -> plan.storeProducts.filter { it.store == BillingStore.GOOGLE } }
        .map { it.productId }
        .distinct()
    if (productIds.isEmpty()) return ClerkResult.success(emptyList())

    return when (val connection = connect()) {
      is Connection.Unavailable -> ClerkResult.apiFailure(connection.error)
      is Connection.Ready -> queryProducts(connection.client, productIds)
    }
  }

  /**
   * Launches the Google Play purchase flow for the store product mapped to [plan] (selected by the
   * optional [productId] and [purchaseOptionId]), then registers the resulting purchase with Clerk.
   *
   * When the user already holds an active Google Play subscription, the flow is launched as a
   * subscription update (plan change) that supersedes it, charged per [replacementMode]; the
   * purchase is blocked before the payment sheet opens when Clerk's preflight reports the active
   * subscription is managed by a different processor.
   */
  @Suppress("ReturnCount")
  suspend fun purchase(
    activity: Activity,
    plan: BillingPlan,
    productId: String? = null,
    purchaseOptionId: String? = null,
    replacementMode: Int = ReplacementMode.CHARGE_PRORATED_PRICE,
  ): ClerkResult<BillingSubscriptionItem, BillingError> {
    val userId = Clerk.user?.id ?: return ClerkResult.apiFailure(BillingError.NotSignedIn)
    val storeProduct =
      when (val resolution = resolveStoreProduct(plan, productId, purchaseOptionId)) {
        is ClerkResult.Failure -> return resolution
        is ClerkResult.Success -> resolution.value
      }
    val basePlanId =
      storeProduct.purchaseOptionId
        ?: return ClerkResult.apiFailure(
          BillingError.ProductNotMapped(plan.id, storeProduct.productId)
        )

    registrar.preflight(storeProduct.productId, basePlanId)?.let {
      return ClerkResult.apiFailure(it)
    }

    return when (val connection = connect()) {
      is Connection.Unavailable -> ClerkResult.apiFailure(connection.error)
      is Connection.Ready ->
        launchPurchase(
          client = connection.client,
          activity = activity,
          productId = storeProduct.productId,
          basePlanId = basePlanId,
          userId = userId,
          replacementMode = replacementMode,
        )
    }
  }

  /**
   * Queries the user's current Google Play subscriptions and registers each purchased one with
   * Clerk. Server-side idempotency makes re-registering already known purchases safe.
   */
  @Suppress("ReturnCount")
  suspend fun restorePurchases(): ClerkResult<List<BillingSubscriptionItem>, BillingError> {
    val client =
      when (val connection = connect()) {
        is Connection.Unavailable -> return ClerkResult.apiFailure(connection.error)
        is Connection.Ready -> connection.client
      }

    val params =
      QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
    val result = client.queryPurchasesAsync(params)
    if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
      return ClerkResult.apiFailure(result.billingResult.toBillingError())
    }

    val purchased =
      result.purchasesList.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
    val items = mutableListOf<BillingSubscriptionItem>()
    var firstError: BillingError? = null
    for (purchase in purchased) {
      when (val registration = registrar.register(purchase.purchaseToken)) {
        is ClerkResult.Success -> items.add(registration.value)
        is ClerkResult.Failure ->
          if (firstError == null) {
            firstError = registration.error ?: BillingError.ServerRejected()
          }
      }
    }
    val error = firstError
    return if (items.isEmpty() && error != null) {
      ClerkResult.apiFailure(error)
    } else {
      ClerkResult.success(items)
    }
  }

  @Suppress("ReturnCount", "LongParameterList")
  private suspend fun launchPurchase(
    client: BillingClient,
    activity: Activity,
    productId: String,
    basePlanId: String,
    userId: String,
    replacementMode: Int,
  ): ClerkResult<BillingSubscriptionItem, BillingError> {
    val productDetails =
      when (val products = queryProducts(client, listOf(productId))) {
        is ClerkResult.Failure -> return ClerkResult.apiFailure(products.error)
        is ClerkResult.Success ->
          products.value.firstOrNull { it.productId == productId }
            ?: return ClerkResult.apiFailure(BillingError.ProductNotFound(productId))
      }
    val offer =
      resolveSubscriptionOffer(productDetails, basePlanId)
        ?: return ClerkResult.apiFailure(BillingError.ProductNotFound(productId))
    val replacement =
      resolveSubscriptionReplacement(client.currentSubscriptionPurchases(), replacementMode)

    val waiter = CompletableDeferred<PurchasesUpdate>()
    if (!pendingPurchase.compareAndSet(null, waiter)) {
      return ClerkResult.apiFailure(BillingError.PurchaseInProgress)
    }

    val flowParams =
      BillingFlowParams.newBuilder()
        .setProductDetailsParamsList(
          listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
              .setProductDetails(productDetails)
              .setOfferToken(offer.offerToken)
              .build()
          )
        )
        .setObfuscatedAccountId(ObfuscatedAccountId.fromUserId(userId))
        .apply {
          if (replacement != null) {
            setSubscriptionUpdateParams(
              BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                .setOldPurchaseToken(replacement.oldPurchaseToken)
                .setSubscriptionReplacementMode(replacement.replacementMode)
                .build()
            )
          }
        }
        .build()

    val launchResult =
      withContext(Dispatchers.Main) { client.launchBillingFlow(activity, flowParams) }
    if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
      pendingPurchase.compareAndSet(waiter, null)
      return ClerkResult.apiFailure(launchResult.toBillingError())
    }

    return handlePurchasesUpdate(waiter.await(), productId)
  }

  private suspend fun handlePurchasesUpdate(
    update: PurchasesUpdate,
    productId: String,
  ): ClerkResult<BillingSubscriptionItem, BillingError> {
    return when (update.result.responseCode) {
      BillingClient.BillingResponseCode.USER_CANCELED ->
        ClerkResult.apiFailure(BillingError.UserCanceled)
      BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED ->
        ClerkResult.apiFailure(BillingError.ItemAlreadyOwned)
      BillingClient.BillingResponseCode.OK -> {
        val purchase =
          update.purchases.firstOrNull { productId in it.products }
            ?: update.purchases.firstOrNull()
        when {
          purchase == null ->
            ClerkResult.apiFailure(
              BillingError.PlayStoreError(
                responseCode = update.result.responseCode,
                debugMessage = "Purchase flow completed without returning a purchase.",
              )
            )
          purchase.purchaseState == Purchase.PurchaseState.PENDING ->
            ClerkResult.apiFailure(BillingError.PurchasePending)
          else -> registrar.register(purchase.purchaseToken)
        }
      }
      else -> ClerkResult.apiFailure(update.result.toBillingError())
    }
  }

  /** Registers purchases delivered outside an in-flight [purchase] call. */
  private suspend fun registerOutOfBandPurchases(purchases: List<Purchase>) {
    purchases
      .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
      .forEach { purchase ->
        registrar.register(purchase.purchaseToken).onFailure { failure ->
          ClerkLog.w("Failed to register out-of-band purchase: ${failure.error}")
        }
      }
  }

  private suspend fun queryProducts(
    client: BillingClient,
    productIds: List<String>,
  ): ClerkResult<List<ProductDetails>, BillingError> {
    val params =
      QueryProductDetailsParams.newBuilder()
        .setProductList(
          productIds.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
              .setProductId(productId)
              .setProductType(BillingClient.ProductType.SUBS)
              .build()
          }
        )
        .build()
    val result = client.queryProductDetails(params)
    return if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
      ClerkResult.success(result.productDetailsList.orEmpty())
    } else {
      ClerkResult.apiFailure(result.billingResult.toBillingError())
    }
  }

  /**
   * Returns a connected [BillingClient], lazily creating it and retrying transient connection
   * failures with a linear backoff. A client that was disconnected by the Play Store is reused and
   * reconnected on the next call.
   */
  private suspend fun connect(): Connection = connectionMutex.withLock {
    billingClient
      ?.takeIf { it.isReady }
      ?.let {
        return Connection.Ready(it)
      }

    val context =
      Clerk.applicationContext?.get()
        ?: return Connection.Unavailable(
          BillingError.BillingUnavailable(debugMessage = "Clerk is not initialized.")
        )

    val client =
      billingClient
        ?: BillingClient.newBuilder(context)
          .setListener(purchasesUpdatedListener)
          .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
          )
          .build()
          .also { billingClient = it }

    var lastResult: BillingResult? = null
    repeat(MAX_CONNECTION_ATTEMPTS) { attempt ->
      val result = client.startConnectionAndAwait()
      when (result.responseCode) {
        BillingClient.BillingResponseCode.OK -> return Connection.Ready(client)
        BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
        BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED,
        BillingClient.BillingResponseCode.DEVELOPER_ERROR ->
          return Connection.Unavailable(
            BillingError.BillingUnavailable(result.responseCode, result.debugMessage)
          )
        else -> lastResult = result
      }
      delay(CONNECTION_RETRY_DELAY_MS * (attempt + 1))
    }
    Connection.Unavailable(
      BillingError.BillingUnavailable(lastResult?.responseCode, lastResult?.debugMessage)
    )
  }

  private suspend fun BillingClient.startConnectionAndAwait(): BillingResult =
    suspendCancellableCoroutine { continuation ->
      startConnection(
        object : BillingClientStateListener {
          override fun onBillingSetupFinished(billingResult: BillingResult) {
            if (continuation.isActive) continuation.resume(billingResult)
          }

          override fun onBillingServiceDisconnected() {
            // Intentionally empty: the client keeps its listener and the next billing
            // call reconnects via connect().
          }
        }
      )
    }

  private fun BillingResult.toBillingError(): BillingError =
    when (responseCode) {
      BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
      BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
      BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
      BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED ->
        BillingError.BillingUnavailable(responseCode, debugMessage)
      else -> BillingError.PlayStoreError(responseCode, debugMessage)
    }

  private companion object {
    const val MAX_CONNECTION_ATTEMPTS = 3
    const val CONNECTION_RETRY_DELAY_MS = 500L
  }
}

/**
 * Resolves the Google Play store product to purchase for [plan].
 *
 * A plan can map any number of Google Play purchase identities (product + base plan): with exactly
 * one mapping no selector is needed; with several, [productId] (and [purchaseOptionId] when one
 * product maps multiple base plans) identify the mapping to buy. Selectors, when provided, must
 * match a mapping exactly.
 */
internal fun resolveStoreProduct(
  plan: BillingPlan,
  productId: String? = null,
  purchaseOptionId: String? = null,
): ClerkResult<BillingStoreProduct, BillingError> {
  val candidates = plan.storeProducts.filter { it.store == BillingStore.GOOGLE }
  val matches =
    if (productId == null && purchaseOptionId == null) {
      candidates
    } else {
      candidates.filter {
        (productId == null || it.productId == productId) &&
          (purchaseOptionId == null || it.purchaseOptionId == purchaseOptionId)
      }
    }
  return when {
    matches.isEmpty() ->
      ClerkResult.apiFailure(BillingError.ProductNotMapped(plan.id, productId, purchaseOptionId))
    matches.size > 1 -> ClerkResult.apiFailure(BillingError.AmbiguousStoreProduct(plan.id, matches))
    else -> ClerkResult.success(matches.single())
  }
}

/**
 * Queries the store account's current Google Play subscription purchases, used to detect whether a
 * purchase is a plan change. Fails open to an empty list on a query error — Google Play still
 * rejects a genuinely conflicting fresh purchase with `ITEM_ALREADY_OWNED`, so no charge can slip
 * through.
 */
private suspend fun BillingClient.currentSubscriptionPurchases(): List<Purchase> {
  val params =
    QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
  val result = queryPurchasesAsync(params)
  if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
    ClerkLog.w(
      "Failed to query current subscriptions before purchase; launching as a fresh " +
        "purchase: ${result.billingResult.debugMessage}"
    )
    return emptyList()
  }
  return result.purchasesList
}

/**
 * The Google Play subscription update to apply when a purchase is a plan change: the token of the
 * purchase being superseded, plus how Google Play charges the switch.
 */
internal data class SubscriptionReplacement(val oldPurchaseToken: String, val replacementMode: Int)

/**
 * Resolves the subscription update to attach to a purchase flow.
 *
 * When the store account already holds an active Google Play subscription purchase, buying a plan
 * is a plan change, not a fresh purchase: the billing flow must carry the current purchase's token
 * so Google Play supersedes it (the new purchase reports it as `linkedPurchaseToken`, which Clerk
 * uses to swap the subscription item at registration). There is at most one relevant purchase — a
 * store account owns a single subscription per product — so the first purchase in the `PURCHASED`
 * state is the one being replaced. Returns `null` when no active purchase exists (a fresh
 * purchase), including when the only purchases are still pending.
 */
internal fun resolveSubscriptionReplacement(
  currentPurchases: List<Purchase>,
  replacementMode: Int,
): SubscriptionReplacement? =
  currentPurchases
    .firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED }
    ?.let { SubscriptionReplacement(it.purchaseToken, replacementMode) }

/**
 * Resolves the subscription offer to purchase for the given Google Play base plan.
 *
 * Google Play returns only the offers the user is eligible for. Among the offers belonging to
 * [basePlanId], applies automatic selection (matching the Clerk Expo SDK): the offer with the
 * longest free trial, then the cheapest introductory price, then the base offer (no offer ID), then
 * the first eligible offer. Returns `null` when the product has no offer for the base plan.
 */
internal fun resolveSubscriptionOffer(
  productDetails: ProductDetails,
  basePlanId: String,
): ProductDetails.SubscriptionOfferDetails? {
  val eligible =
    productDetails.subscriptionOfferDetails.orEmpty().filter { it.basePlanId == basePlanId }
  val trial = eligible.filter { it.freeTrialDays > 0 }.maxByOrNull { it.freeTrialDays }
  val introductory =
    eligible
      .mapNotNull { offer -> offer.introductoryPriceMicros?.let { price -> offer to price } }
      .minByOrNull { it.second }
      ?.first
  return trial
    ?: introductory
    ?: eligible.firstOrNull { it.offerId == null }
    ?: eligible.firstOrNull()
}

/** The total number of days of free (zero-price) pricing phases in this offer. */
private val ProductDetails.SubscriptionOfferDetails.freeTrialDays: Int
  get() =
    pricingPhases.pricingPhaseList
      .filter { it.priceAmountMicros == 0L }
      .sumOf { isoPeriodDays(it.billingPeriod) * maxOf(it.billingCycleCount, 1) }

/**
 * The price of the offer's introductory (first) pricing phase, or `null` when the offer has no
 * discounted lead-in phase.
 */
private val ProductDetails.SubscriptionOfferDetails.introductoryPriceMicros: Long?
  get() = pricingPhases.pricingPhaseList.takeIf { it.size > 1 }?.firstOrNull()?.priceAmountMicros

private val ISO_PERIOD_REGEX =
  Regex("""^P(?:(?<days>\d+)D)?(?:(?<weeks>\d+)W)?(?:(?<months>\d+)M)?(?:(?<years>\d+)Y)?$""")

private const val DAYS_PER_WEEK = 7
private const val DAYS_PER_MONTH = 30
private const val DAYS_PER_YEAR = 365

/** The approximate number of days in an ISO 8601 [period] (e.g. `P1M` -> 30). */
private fun isoPeriodDays(period: String): Int {
  val match = ISO_PERIOD_REGEX.matchEntire(period) ?: return 0
  fun component(name: String) = match.groups[name]?.value?.toIntOrNull() ?: 0
  return component("days") +
    component("weeks") * DAYS_PER_WEEK +
    component("months") * DAYS_PER_MONTH +
    component("years") * DAYS_PER_YEAR
}
