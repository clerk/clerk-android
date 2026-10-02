package com.clerk.api.billing

import com.clerk.api.network.ClerkApi
import com.clerk.api.network.ClerkPaginatedResponse
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.user.currentSessionId

/**
 * Reads Clerk Billing data: Plans, Subscriptions, statements, payment attempts, and credits.
 *
 * Methods that read a payer's data take an optional `orgId`: omit it for the signed-in user, or
 * pass an Organization ID to read that Organization's data, which requires the
 * `org:sys_billing:read` Permission.
 */
object Billing {

  /**
   * Lists your publicly visible Plans.
   *
   * @param forPayer Whether to list user Plans or Organization Plans.
   * @param orgId The Organization to fetch Plans for. Populates each Plan's available prices for
   *   that Organization.
   * @param minSeats The minimum number of seats the returned Plans need to support.
   * @param limit The maximum number of Plans to return.
   * @param offset The number of Plans to skip.
   */
  suspend fun getPlans(
    forPayer: ForPayerType = ForPayerType.USER,
    orgId: String? = null,
    minSeats: Int? = null,
    limit: Int = DEFAULT_BILLING_LIMIT,
    offset: Int = 0,
  ): ClerkResult<ClerkPaginatedResponse<BillingPlan>, ClerkErrorResponse> {
    return ClerkApi.billing.getPlans(
      payerType = forPayer.toPayerTypeQueryValue(),
      orgId = orgId,
      minSeats = minSeats,
      offset = offset,
      limit = limit,
      sessionId = currentSessionId(),
    )
  }

  /** Gets a Plan by ID. */
  suspend fun getPlan(id: String): ClerkResult<BillingPlan, ClerkErrorResponse> {
    return ClerkApi.billing.getPlan(id = id, sessionId = currentSessionId())
  }

  /** Gets the Subscription of the signed-in user, or of the Organization with [orgId]. */
  suspend fun getSubscription(
    orgId: String? = null
  ): ClerkResult<BillingSubscription, ClerkErrorResponse> {
    val sessionId = currentSessionId()
    return if (orgId != null) {
      ClerkApi.billing.getOrganizationSubscription(organizationId = orgId, sessionId = sessionId)
    } else {
      ClerkApi.billing.getUserSubscription(sessionId = sessionId)
    }
  }

  /**
   * Lists the statements of the signed-in user, or of the Organization with [orgId].
   *
   * @param limit The maximum number of statements to return.
   * @param offset The number of statements to skip.
   */
  suspend fun getStatements(
    orgId: String? = null,
    limit: Int = DEFAULT_BILLING_LIMIT,
    offset: Int = 0,
  ): ClerkResult<ClerkPaginatedResponse<BillingStatement>, ClerkErrorResponse> {
    val sessionId = currentSessionId()
    return if (orgId != null) {
      ClerkApi.billing.getOrganizationStatements(
        organizationId = orgId,
        offset = offset,
        limit = limit,
        sessionId = sessionId,
      )
    } else {
      ClerkApi.billing.getUserStatements(offset = offset, limit = limit, sessionId = sessionId)
    }
  }

  /** Gets a statement by ID. */
  suspend fun getStatement(
    id: String,
    orgId: String? = null,
  ): ClerkResult<BillingStatement, ClerkErrorResponse> {
    val sessionId = currentSessionId()
    return if (orgId != null) {
      ClerkApi.billing.getOrganizationStatement(
        organizationId = orgId,
        id = id,
        sessionId = sessionId,
      )
    } else {
      ClerkApi.billing.getUserStatement(id = id, sessionId = sessionId)
    }
  }

  /**
   * Lists the payment attempts of the signed-in user, or of the Organization with [orgId].
   *
   * @param limit The maximum number of payment attempts to return.
   * @param offset The number of payment attempts to skip.
   */
  suspend fun getPaymentAttempts(
    orgId: String? = null,
    limit: Int = DEFAULT_BILLING_LIMIT,
    offset: Int = 0,
  ): ClerkResult<ClerkPaginatedResponse<BillingPayment>, ClerkErrorResponse> {
    val sessionId = currentSessionId()
    return if (orgId != null) {
      ClerkApi.billing.getOrganizationPaymentAttempts(
        organizationId = orgId,
        offset = offset,
        limit = limit,
        sessionId = sessionId,
      )
    } else {
      ClerkApi.billing.getUserPaymentAttempts(
        offset = offset,
        limit = limit,
        sessionId = sessionId,
      )
    }
  }

  /** Gets a payment attempt by ID. */
  suspend fun getPaymentAttempt(
    id: String,
    orgId: String? = null,
  ): ClerkResult<BillingPayment, ClerkErrorResponse> {
    val sessionId = currentSessionId()
    return if (orgId != null) {
      ClerkApi.billing.getOrganizationPaymentAttempt(
        organizationId = orgId,
        id = id,
        sessionId = sessionId,
      )
    } else {
      ClerkApi.billing.getUserPaymentAttempt(id = id, sessionId = sessionId)
    }
  }

  /** Gets the credit balance of the signed-in user, or of the Organization with [orgId]. */
  suspend fun getCreditBalance(
    orgId: String? = null
  ): ClerkResult<BillingCreditBalance, ClerkErrorResponse> {
    val sessionId = currentSessionId()
    return if (orgId != null) {
      ClerkApi.billing.getOrganizationCreditBalance(organizationId = orgId, sessionId = sessionId)
    } else {
      ClerkApi.billing.getUserCreditBalance(sessionId = sessionId)
    }
  }

  /**
   * Lists the credit ledger entries of the signed-in user, or of the Organization with [orgId].
   *
   * @param limit The maximum number of entries to return.
   * @param offset The number of entries to skip.
   */
  suspend fun getCreditHistory(
    orgId: String? = null,
    limit: Int = DEFAULT_BILLING_LIMIT,
    offset: Int = 0,
  ): ClerkResult<ClerkPaginatedResponse<BillingCreditLedger>, ClerkErrorResponse> {
    val sessionId = currentSessionId()
    return if (orgId != null) {
      ClerkApi.billing.getOrganizationCreditHistory(
        organizationId = orgId,
        limit = limit,
        offset = offset,
        sessionId = sessionId,
      )
    } else {
      ClerkApi.billing.getUserCreditHistory(limit = limit, offset = offset, sessionId = sessionId)
    }
  }
}
