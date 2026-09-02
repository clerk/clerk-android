@file:Suppress("TooManyFunctions")

package com.clerk.api.network.api

import com.clerk.api.Clerk
import com.clerk.api.billing.BillingCreditBalance
import com.clerk.api.billing.BillingCreditLedger
import com.clerk.api.billing.BillingPayment
import com.clerk.api.billing.BillingPaymentMethod
import com.clerk.api.billing.BillingPlan
import com.clerk.api.billing.BillingStatement
import com.clerk.api.billing.BillingSubscription
import com.clerk.api.network.ApiParams
import com.clerk.api.network.ApiPaths
import com.clerk.api.network.ClerkPaginatedResponse
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Internal Retrofit API for Clerk Billing GET endpoints.
 *
 * Paths are relative to `/v1/`. [ClerkApiResultConverterFactory] unwraps FAPI `{ response: T,
 * client }` envelopes except for the raw-body GETs (`getPlans`, `getPlan`, `getPaymentAttempts`,
 * `getPaymentAttempt`), which match clerk-js.
 */
internal interface BillingApi {

  @GET(ApiPaths.Billing.PLANS)
  suspend fun getPlans(
    @Query(ApiParams.PAYER_TYPE) payerType: String? = null,
    @Query(ApiParams.ORG_ID) orgId: String? = null,
    @Query(ApiParams.MIN_SEATS) minSeats: Int? = null,
    @Query(ApiParams.LIMIT) limit: Int? = null,
    @Query(ApiParams.OFFSET) offset: Int? = null,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<ClerkPaginatedResponse<BillingPlan>, ClerkErrorResponse>

  @GET(ApiPaths.Billing.PLAN)
  suspend fun getPlan(
    @Path(ApiParams.ID) id: String,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<BillingPlan, ClerkErrorResponse>

  @GET(ApiPaths.Billing.User.SUBSCRIPTION)
  suspend fun getUserSubscription(
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id
  ): ClerkResult<BillingSubscription, ClerkErrorResponse>

  @GET(ApiPaths.Billing.Organization.SUBSCRIPTION)
  suspend fun getOrganizationSubscription(
    @Path(ApiParams.ORGANIZATION_ID) organizationId: String,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<BillingSubscription, ClerkErrorResponse>

  @GET(ApiPaths.Billing.User.STATEMENTS)
  suspend fun getUserStatements(
    @Query(ApiParams.LIMIT) limit: Int? = null,
    @Query(ApiParams.OFFSET) offset: Int? = null,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<ClerkPaginatedResponse<BillingStatement>, ClerkErrorResponse>

  @GET(ApiPaths.Billing.Organization.STATEMENTS)
  suspend fun getOrganizationStatements(
    @Path(ApiParams.ORGANIZATION_ID) organizationId: String,
    @Query(ApiParams.LIMIT) limit: Int? = null,
    @Query(ApiParams.OFFSET) offset: Int? = null,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<ClerkPaginatedResponse<BillingStatement>, ClerkErrorResponse>

  @GET(ApiPaths.Billing.User.STATEMENT)
  suspend fun getUserStatement(
    @Path(ApiParams.ID) id: String,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<BillingStatement, ClerkErrorResponse>

  @GET(ApiPaths.Billing.Organization.STATEMENT)
  suspend fun getOrganizationStatement(
    @Path(ApiParams.ORGANIZATION_ID) organizationId: String,
    @Path(ApiParams.ID) id: String,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<BillingStatement, ClerkErrorResponse>

  @GET(ApiPaths.Billing.User.PAYMENT_ATTEMPTS)
  suspend fun getUserPaymentAttempts(
    @Query(ApiParams.LIMIT) limit: Int? = null,
    @Query(ApiParams.OFFSET) offset: Int? = null,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<ClerkPaginatedResponse<BillingPayment>, ClerkErrorResponse>

  @GET(ApiPaths.Billing.Organization.PAYMENT_ATTEMPTS)
  suspend fun getOrganizationPaymentAttempts(
    @Path(ApiParams.ORGANIZATION_ID) organizationId: String,
    @Query(ApiParams.LIMIT) limit: Int? = null,
    @Query(ApiParams.OFFSET) offset: Int? = null,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<ClerkPaginatedResponse<BillingPayment>, ClerkErrorResponse>

  @GET(ApiPaths.Billing.User.PAYMENT_ATTEMPT)
  suspend fun getUserPaymentAttempt(
    @Path(ApiParams.ID) id: String,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<BillingPayment, ClerkErrorResponse>

  @GET(ApiPaths.Billing.Organization.PAYMENT_ATTEMPT)
  suspend fun getOrganizationPaymentAttempt(
    @Path(ApiParams.ORGANIZATION_ID) organizationId: String,
    @Path(ApiParams.ID) id: String,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<BillingPayment, ClerkErrorResponse>

  @GET(ApiPaths.Billing.User.CREDITS)
  suspend fun getUserCreditBalance(
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id
  ): ClerkResult<BillingCreditBalance, ClerkErrorResponse>

  @GET(ApiPaths.Billing.Organization.CREDITS)
  suspend fun getOrganizationCreditBalance(
    @Path(ApiParams.ORGANIZATION_ID) organizationId: String,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<BillingCreditBalance, ClerkErrorResponse>

  @GET(ApiPaths.Billing.User.CREDIT_HISTORY)
  suspend fun getUserCreditHistory(
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id
  ): ClerkResult<ClerkPaginatedResponse<BillingCreditLedger>, ClerkErrorResponse>

  @GET(ApiPaths.Billing.Organization.CREDIT_HISTORY)
  suspend fun getOrganizationCreditHistory(
    @Path(ApiParams.ORGANIZATION_ID) organizationId: String,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<ClerkPaginatedResponse<BillingCreditLedger>, ClerkErrorResponse>

  @GET(ApiPaths.Billing.User.PAYMENT_METHODS)
  suspend fun getUserPaymentMethods(
    @Query(ApiParams.LIMIT) limit: Int? = null,
    @Query(ApiParams.OFFSET) offset: Int? = null,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<ClerkPaginatedResponse<BillingPaymentMethod>, ClerkErrorResponse>

  @GET(ApiPaths.Billing.Organization.PAYMENT_METHODS)
  suspend fun getOrganizationPaymentMethods(
    @Path(ApiParams.ORGANIZATION_ID) organizationId: String,
    @Query(ApiParams.LIMIT) limit: Int? = null,
    @Query(ApiParams.OFFSET) offset: Int? = null,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<ClerkPaginatedResponse<BillingPaymentMethod>, ClerkErrorResponse>
}
