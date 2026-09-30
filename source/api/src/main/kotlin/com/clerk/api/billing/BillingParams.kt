package com.clerk.api.billing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal const val DEFAULT_BILLING_LIMIT = 20

/** The payer type of a Billing Plan. */
@Serializable
enum class ForPayerType {
  @SerialName("organization") ORGANIZATION,
  @SerialName("user") USER,
}

internal fun ForPayerType.toPayerTypeQueryValue(): String {
  return if (this == ForPayerType.ORGANIZATION) "org" else "user"
}
