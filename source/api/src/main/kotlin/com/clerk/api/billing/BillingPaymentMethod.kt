package com.clerk.api.billing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The status of a payment method.
 *
 * Matches clerk-js `BillingPaymentMethodStatus`.
 */
@Serializable
enum class BillingPaymentMethodStatus {
  @SerialName("active") ACTIVE,
  @SerialName("expired") EXPIRED,
  @SerialName("disconnected") DISCONNECTED,
  @SerialName("unknown") UNKNOWN,
}

/**
 * A stored payment method for a user or Organization.
 *
 * Write methods from clerk-js (`remove`, `makeDefault`) are not ported.
 *
 * @property id The unique identifier for the payment method.
 * @property last4 The last four digits of the payment method.
 * @property paymentType The type of payment method. For example, `card`.
 * @property cardType The brand or type of card. For example, `visa` or `mastercard`.
 * @property isDefault Whether the payment method is set as the default for the account.
 * @property isRemovable Whether the payment method can be removed by the user.
 * @property status The current status of the payment method.
 * @property walletType The type of digital wallet, if applicable. For example, `apple_pay` or
 *   `google_pay`.
 * @property expiryYear The card expiration year, if available.
 * @property expiryMonth The card expiration month, if available.
 * @property createdAt The date the payment method was created, as Unix milliseconds.
 * @property updatedAt The date the payment method was last updated, as Unix milliseconds.
 */
@Serializable
data class BillingPaymentMethod(
  val id: String,
  val last4: String? = null,
  val paymentType: String? = null,
  val cardType: String? = null,
  val isDefault: Boolean? = null,
  val isRemovable: Boolean? = null,
  val status: BillingPaymentMethodStatus = BillingPaymentMethodStatus.UNKNOWN,
  val walletType: String? = null,
  val expiryYear: Int? = null,
  val expiryMonth: Int? = null,
  val createdAt: Long? = null,
  val updatedAt: Long? = null,
)
