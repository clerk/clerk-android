package com.clerk.api.network.model.billing

import kotlinx.serialization.Serializable

/**
 * Response of the store purchase preflight check (`POST /me/billing/store_purchases/preflight`).
 *
 * The endpoint returns `allowed: true` when every Clerk-owned precondition passes (the store
 * connection is configured, the product is mapped to a plan, and the payer has no conflicting
 * subscription managed by another processor) and rejects with an API error otherwise, so a
 * successful response always carries `allowed: true`.
 */
@Serializable internal data class StorePurchasePreflight(val allowed: Boolean = false)
