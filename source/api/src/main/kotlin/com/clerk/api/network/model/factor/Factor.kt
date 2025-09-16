package com.clerk.api.network.model.factor

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The Factor type represents the factor verification strategy that can be used in the sign-in
 * process.
 */
@Serializable
data class Factor(
  /** The strategy of the factor. */
  val strategy: String,

  /** The ID of the email address that a code or link will be sent to. */
  @SerialName("email_address_id") val emailAddressId: String? = null,

  /** The ID of the phone number that a code will be sent to. */
  @SerialName("phone_number_id") val phoneNumberId: String? = null,

  /** The ID of the Web3 wallet that will be used to sign a message. */
  val web3WalletId: String? = null,

  /** The safe identifier of the factor. */
  @SerialName("safe_identifier") val safeIdentifier: String? = null,

  /** Whether the factor is the primary factor. */
  val primary: Boolean? = null,
)

fun Factor.isResetFactor() =
  (strategy == "reset_password_email_code" || strategy == "reset_password_phone_code")
