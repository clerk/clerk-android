package com.clerk.api.billing

import com.clerk.api.network.ClerkApi
import com.clerk.api.network.ClerkPaginatedResponse
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.user.currentSessionId

/**
 * Billing GET APIs for plans, subscriptions, statements, payments, and credits.
 *
 * This is an experimental public-beta API and is subject to change. Pin the SDK version to avoid
 * breaking changes.
 *
 * Apps call these methods through [com.clerk.api.Clerk.billing], for example
 * `Clerk.billing.getPlans(...)`.
 *
 * Write methods from clerk-js (`startCheckout`, `updateCheckout`, `initializePaymentMethod`,
 * `addPaymentMethod`, `cancel`, `remove`, `makeDefault`) are not ported.
 *
 * Public pagination uses clerk-js `initialPage` and `pageSize`. Those values are converted to FAPI
 * `offset` / `limit` inside this namespace: `offset = (initialPage - 1) * pageSize`, `limit =
 * pageSize`. Defaults are `initialPage=1` and `pageSize=10`.
 */
object Billing {

  /**
   * Gets a list of payment attempts for the current user or supplied Organization.
   *
   * @param params Pagination and optional [GetPaymentAttemptsParams.orgId] scope.
   * @return A [ClerkResult] containing a [ClerkPaginatedResponse] of [BillingPayment] objects on
   *   success, or a [ClerkErrorResponse] on failure.
   */
  suspend fun getPaymentAttempts(
    params: GetPaymentAttemptsParams = GetPaymentAttemptsParams()
  ): ClerkResult<ClerkPaginatedResponse<BillingPayment>, ClerkErrorResponse> {
    val pagination = billingOffsetLimit(params.initialPage, params.pageSize)
    val sessionId = currentSessionId()
    return if (params.orgId != null) {
      ClerkApi.billing.getOrganizationPaymentAttempts(
        organizationId = params.orgId,
        offset = pagination.offset,
        limit = pagination.limit,
        sessionId = sessionId,
      )
    } else {
      ClerkApi.billing.getUserPaymentAttempts(
        offset = pagination.offset,
        limit = pagination.limit,
        sessionId = sessionId,
      )
    }
  }

  /**
   * Gets details of a specific payment attempt for the current user or supplied Organization.
   *
   * @param params The payment attempt [GetPaymentAttemptParams.id] and optional organization scope.
   * @return A [ClerkResult] containing a [BillingPayment] on success, or a [ClerkErrorResponse] on
   *   failure.
   */
  suspend fun getPaymentAttempt(
    params: GetPaymentAttemptParams
  ): ClerkResult<BillingPayment, ClerkErrorResponse> {
    val sessionId = currentSessionId()
    return if (params.orgId != null) {
      ClerkApi.billing.getOrganizationPaymentAttempt(
        organizationId = params.orgId,
        id = params.id,
        sessionId = sessionId,
      )
    } else {
      ClerkApi.billing.getUserPaymentAttempt(id = params.id, sessionId = sessionId)
    }
  }

  /**
   * Gets a list of all publicly visible Billing Plans.
   *
   * @param params Optional payer type, organization, seat filter, and pagination.
   *   [GetPlansParams.forPayer] matches clerk-js `for`.
   * @return A [ClerkResult] containing a [ClerkPaginatedResponse] of [BillingPlan] objects on
   *   success, or a [ClerkErrorResponse] on failure.
   */
  suspend fun getPlans(
    params: GetPlansParams? = null
  ): ClerkResult<ClerkPaginatedResponse<BillingPlan>, ClerkErrorResponse> {
    val resolved = params ?: GetPlansParams()
    val pagination = billingOffsetLimit(resolved.initialPage, resolved.pageSize)
    return ClerkApi.billing.getPlans(
      payerType = resolved.forPayer.toPayerTypeQueryValue(),
      orgId = resolved.orgId,
      minSeats = resolved.minSeats,
      offset = pagination.offset,
      limit = pagination.limit,
      sessionId = currentSessionId(),
    )
  }

  /**
   * Gets a given Billing Plan.
   *
   * @param params The Plan [GetPlanParams.id] to fetch.
   * @return A [ClerkResult] containing a [BillingPlan] on success, or a [ClerkErrorResponse] on
   *   failure.
   */
  suspend fun getPlan(params: GetPlanParams): ClerkResult<BillingPlan, ClerkErrorResponse> {
    return ClerkApi.billing.getPlan(id = params.id, sessionId = currentSessionId())
  }

  /**
   * Gets the main Billing Subscription for the current user or supplied Organization.
   *
   * @param params Optional [GetSubscriptionParams.orgId]. Omit to use the current user.
   * @return A [ClerkResult] containing a [BillingSubscription] on success, or a
   *   [ClerkErrorResponse] on failure.
   */
  suspend fun getSubscription(
    params: GetSubscriptionParams = GetSubscriptionParams()
  ): ClerkResult<BillingSubscription, ClerkErrorResponse> {
    val sessionId = currentSessionId()
    return if (params.orgId != null) {
      ClerkApi.billing.getOrganizationSubscription(
        organizationId = params.orgId,
        sessionId = sessionId,
      )
    } else {
      ClerkApi.billing.getUserSubscription(sessionId = sessionId)
    }
  }

  /**
   * Gets a list of Billing Statements for the current user or supplied Organization.
   *
   * @param params Pagination and optional [GetStatementsParams.orgId] scope.
   * @return A [ClerkResult] containing a [ClerkPaginatedResponse] of [BillingStatement] objects on
   *   success, or a [ClerkErrorResponse] on failure.
   */
  suspend fun getStatements(
    params: GetStatementsParams = GetStatementsParams()
  ): ClerkResult<ClerkPaginatedResponse<BillingStatement>, ClerkErrorResponse> {
    val pagination = billingOffsetLimit(params.initialPage, params.pageSize)
    val sessionId = currentSessionId()
    return if (params.orgId != null) {
      ClerkApi.billing.getOrganizationStatements(
        organizationId = params.orgId,
        offset = pagination.offset,
        limit = pagination.limit,
        sessionId = sessionId,
      )
    } else {
      ClerkApi.billing.getUserStatements(
        offset = pagination.offset,
        limit = pagination.limit,
        sessionId = sessionId,
      )
    }
  }

  /**
   * Gets a given Billing Statement.
   *
   * @param params The statement [GetStatementParams.id] and optional organization scope.
   * @return A [ClerkResult] containing a [BillingStatement] on success, or a [ClerkErrorResponse]
   *   on failure.
   */
  suspend fun getStatement(
    params: GetStatementParams
  ): ClerkResult<BillingStatement, ClerkErrorResponse> {
    val sessionId = currentSessionId()
    return if (params.orgId != null) {
      ClerkApi.billing.getOrganizationStatement(
        organizationId = params.orgId,
        id = params.id,
        sessionId = sessionId,
      )
    } else {
      ClerkApi.billing.getUserStatement(id = params.id, sessionId = sessionId)
    }
  }

  /**
   * Gets the credit balance for the current payer.
   *
   * @param params Optional [GetCreditBalanceParams.orgId]. Omit to use the current user.
   * @return A [ClerkResult] containing a [BillingCreditBalance] on success, or a
   *   [ClerkErrorResponse] on failure.
   */
  suspend fun getCreditBalance(
    params: GetCreditBalanceParams = GetCreditBalanceParams()
  ): ClerkResult<BillingCreditBalance, ClerkErrorResponse> {
    val sessionId = currentSessionId()
    return if (params.orgId != null) {
      ClerkApi.billing.getOrganizationCreditBalance(
        organizationId = params.orgId,
        sessionId = sessionId,
      )
    } else {
      ClerkApi.billing.getUserCreditBalance(sessionId = sessionId)
    }
  }

  /**
   * Gets the credit history for the current payer.
   *
   * clerk-js does not paginate this request. Only [GetCreditHistoryParams.orgId] is forwarded.
   *
   * @param params Optional organization scope. Omit to use the current user.
   * @return A [ClerkResult] containing a [ClerkPaginatedResponse] of [BillingCreditLedger] objects
   *   on success, or a [ClerkErrorResponse] on failure.
   */
  suspend fun getCreditHistory(
    params: GetCreditHistoryParams = GetCreditHistoryParams()
  ): ClerkResult<ClerkPaginatedResponse<BillingCreditLedger>, ClerkErrorResponse> {
    val sessionId = currentSessionId()
    return if (params.orgId != null) {
      ClerkApi.billing.getOrganizationCreditHistory(
        organizationId = params.orgId,
        sessionId = sessionId,
      )
    } else {
      ClerkApi.billing.getUserCreditHistory(sessionId = sessionId)
    }
  }
}
