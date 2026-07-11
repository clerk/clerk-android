package com.clerk.api.network.api

import com.clerk.api.network.ApiParams
import com.clerk.api.network.ApiPaths
import com.clerk.api.network.ClerkPaginatedResponse
import com.clerk.api.network.model.billing.BillingPlan
import com.clerk.api.network.model.billing.BillingSubscriptionItem
import com.clerk.api.network.model.billing.StorePurchasePreflight
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Internal API interface for Clerk Billing (commerce) operations.
 *
 * This interface defines the REST API endpoints for fetching the instance's plan catalog, the
 * current user's subscription items, and for registering app-store purchases with Clerk.
 *
 * This is an internal API interface used by the Clerk SDK and should not be used directly by
 * application code.
 */
internal interface CommerceApi {
  /**
   * Retrieves the plans available on the instance.
   *
   * Plans include the store product identifiers mapped to them in the Clerk Dashboard so that the
   * SDK knows which products to request from Google Play Billing.
   *
   * @param payerType Optional payer type filter (`user` or `org`)
   * @param limit Optional maximum number of plans to return
   * @param offset Optional pagination offset
   * @return A [ClerkResult] containing the paginated plans on success, or a [ClerkErrorResponse] on
   *   failure
   */
  @GET(ApiPaths.Commerce.PLANS)
  suspend fun plans(
    @Query("payer_type") payerType: String? = null,
    @Query(ApiParams.LIMIT) limit: Int? = null,
    @Query(ApiParams.OFFSET) offset: Int? = null,
  ): ClerkResult<ClerkPaginatedResponse<BillingPlan>, ClerkErrorResponse>

  /**
   * Retrieves the current user's subscription items.
   *
   * @param limit Optional maximum number of items to return
   * @param offset Optional pagination offset
   * @return A [ClerkResult] containing the paginated subscription items on success, or a
   *   [ClerkErrorResponse] on failure
   */
  @GET(ApiPaths.Commerce.User.SUBSCRIPTION_ITEMS)
  suspend fun subscriptionItems(
    @Query(ApiParams.LIMIT) limit: Int? = null,
    @Query(ApiParams.OFFSET) offset: Int? = null,
  ): ClerkResult<ClerkPaginatedResponse<BillingSubscriptionItem>, ClerkErrorResponse>

  /**
   * Asks Clerk whether a store purchase may proceed, immediately before the payment sheet opens.
   *
   * Checks every Clerk-owned precondition that would reject the purchase at registration time: the
   * instance's store connection, the product-to-plan mapping, and payer conflicts (an active paid
   * subscription managed by a different processor). An active subscription managed by the same
   * store is not a conflict — the purchase proceeds as an in-app plan change. Registration remains
   * the authoritative check; the preflight only prevents charging the user for a purchase that
   * registration is certain to reject.
   *
   * @param store The store the purchase will be made through (e.g. `google`)
   * @param productId The store product identifier about to be purchased
   * @param purchaseOptionId The purchase option (Google Play base plan) about to be purchased
   * @return A [ClerkResult] containing the preflight verdict (always `allowed` on success), or a
   *   [ClerkErrorResponse] describing why the purchase would be rejected
   */
  @FormUrlEncoded
  @POST(ApiPaths.Commerce.User.STORE_PURCHASE_PREFLIGHT)
  suspend fun preflightStorePurchase(
    @Field("store") store: String,
    @Field("product_id") productId: String,
    @Field("purchase_option_id") purchaseOptionId: String? = null,
  ): ClerkResult<StorePurchasePreflight, ClerkErrorResponse>

  /**
   * Registers an app-store purchase with Clerk.
   *
   * Clerk verifies the payload against the store's server API, binds the transaction to the current
   * user, resolves the plan via the Dashboard's store product mapping, activates the subscription
   * item, and acknowledges the purchase server-side. The endpoint is idempotent by the store
   * transaction lineage: replays (e.g. restore purchases) return the current item.
   *
   * @param store The store the purchase was made through (e.g. `google`)
   * @param payload The store-specific purchase payload (the purchase token for Google Play)
   * @return A [ClerkResult] containing the created or existing [BillingSubscriptionItem] on
   *   success, or a [ClerkErrorResponse] on failure
   */
  @FormUrlEncoded
  @POST(ApiPaths.Commerce.User.STORE_PURCHASES)
  suspend fun createStorePurchase(
    @Field("store") store: String,
    @Field("payload") payload: String,
  ): ClerkResult<BillingSubscriptionItem, ClerkErrorResponse>
}
