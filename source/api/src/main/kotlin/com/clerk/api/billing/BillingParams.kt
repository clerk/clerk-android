package com.clerk.api.billing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal const val DEFAULT_BILLING_INITIAL_PAGE = 1
internal const val DEFAULT_BILLING_PAGE_SIZE = 10

/**
 * The payer type used when listing Billing Plans.
 *
 * Matches clerk-js `for` on `GetPlansParams`. Kotlin cannot use `for` as a parameter name without
 * backticks, so public APIs use [forPayer].
 */
@Serializable
enum class ForPayerType {
  @SerialName("organization") ORGANIZATION,
  @SerialName("user") USER,
}

/**
 * Parameters for [Billing.getPlans].
 *
 * @property forPayer The type of payer for the Plans. Matches clerk-js `for`. When
 *   [ForPayerType.ORGANIZATION], FAPI receives `payer_type=org`; otherwise `payer_type=user`.
 * @property orgId The organization ID to fetch plans for. Providing this parameter populates
 *   `availablePrices` with prices available to the authenticated organization.
 * @property minSeats The minimum number of seats that the returned plans need to support.
 * @property initialPage Which page to fetch. Defaults to `1` when omitted.
 * @property pageSize Maximum number of results per page. Defaults to `10` when omitted.
 */
data class GetPlansParams(
  val forPayer: ForPayerType? = null,
  val orgId: String? = null,
  val minSeats: Int? = null,
  val initialPage: Int? = null,
  val pageSize: Int? = null,
)

/**
 * Parameters for [Billing.getPlan].
 *
 * @property id The ID of the Billing Plan to get.
 */
data class GetPlanParams(val id: String)

/**
 * Parameters for [Billing.getSubscription].
 *
 * @property orgId The Organization ID to get the subscription for. Omit to use the current user.
 */
data class GetSubscriptionParams(val orgId: String? = null)

/**
 * Parameters for [Billing.getStatements].
 *
 * @property orgId The Organization ID to list statements for. Omit to use the current user.
 * @property initialPage Which page to fetch. Defaults to `1` when omitted.
 * @property pageSize Maximum number of results per page. Defaults to `10` when omitted.
 */
data class GetStatementsParams(
  val orgId: String? = null,
  val initialPage: Int? = null,
  val pageSize: Int? = null,
)

/**
 * Parameters for [Billing.getStatement].
 *
 * @property id The ID of the statement to get.
 * @property orgId The Organization ID to get the statement for. Omit to use the current user.
 */
data class GetStatementParams(val id: String, val orgId: String? = null)

/**
 * Parameters for [Billing.getPaymentAttempts].
 *
 * @property orgId The Organization ID to list payment attempts for. Omit to use the current user.
 * @property initialPage Which page to fetch. Defaults to `1` when omitted.
 * @property pageSize Maximum number of results per page. Defaults to `10` when omitted.
 */
data class GetPaymentAttemptsParams(
  val orgId: String? = null,
  val initialPage: Int? = null,
  val pageSize: Int? = null,
)

/**
 * Parameters for [Billing.getPaymentAttempt].
 *
 * @property id The unique identifier for the payment attempt to get.
 * @property orgId The Organization ID to get the payment attempt for. Omit to use the current user.
 */
data class GetPaymentAttemptParams(val id: String, val orgId: String? = null)

/**
 * Parameters for [Billing.getCreditBalance].
 *
 * @property orgId The Organization ID to get the credit balance for. Omit to use the current user.
 */
data class GetCreditBalanceParams(val orgId: String? = null)

/**
 * Parameters for [Billing.getCreditHistory].
 *
 * Matches clerk-js `GetCreditHistoryParams`: organization scope only, no pagination.
 *
 * @property orgId The Organization ID to get the credit history for. Omit to use the current user.
 */
data class GetCreditHistoryParams(val orgId: String? = null)

/**
 * Parameters for [com.clerk.api.user.User.getPaymentMethods] and
 * [com.clerk.api.organizations.Organization.getPaymentMethods].
 *
 * @property initialPage Which page to fetch. Defaults to `1` when omitted.
 * @property pageSize Maximum number of results per page. Defaults to `10` when omitted.
 */
data class GetPaymentMethodsParams(val initialPage: Int? = null, val pageSize: Int? = null)

/** FAPI `offset` / `limit` values derived from clerk-js `initialPage` / `pageSize`. */
internal data class BillingOffsetLimit(val offset: Int, val limit: Int)

/**
 * Converts clerk-js pagination (`initialPage`, `pageSize`) to FAPI `offset` / `limit`.
 *
 * `offset = (initialPage - 1) * pageSize`, `limit = pageSize`. Defaults match clerk-js:
 * `initialPage=1`, `pageSize=10`.
 */
internal fun billingOffsetLimit(
  initialPage: Int? = null,
  pageSize: Int? = null,
): BillingOffsetLimit {
  val resolvedPageSize = pageSize ?: DEFAULT_BILLING_PAGE_SIZE
  val resolvedInitialPage = initialPage ?: DEFAULT_BILLING_INITIAL_PAGE
  return BillingOffsetLimit(
    offset = (resolvedInitialPage - 1) * resolvedPageSize,
    limit = resolvedPageSize,
  )
}

/**
 * Maps clerk-js `for` / [ForPayerType] to the FAPI `payer_type` query value.
 *
 * Organization → `org`. User or omitted → `user`.
 */
internal fun ForPayerType?.toPayerTypeQueryValue(): String {
  return if (this == ForPayerType.ORGANIZATION) "org" else "user"
}
