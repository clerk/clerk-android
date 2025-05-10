package com.clerk.factor

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
  val emailAddressId: String? = null,

  /** The ID of the phone number that a code will be sent to. */
  val phoneNumberId: String? = null,

  /** The ID of the Web3 wallet that will be used to sign a message. */
  val web3WalletId: String? = null,

  /** The safe identifier of the factor. */
  val safeIdentifier: String? = null,

  /** Whether the factor is the primary factor. */
  val primary: Boolean? = null,
)
