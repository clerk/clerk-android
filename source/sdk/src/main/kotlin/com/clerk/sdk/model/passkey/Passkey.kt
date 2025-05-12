package com.clerk.sdk.model.passkey

import com.clerk.sdk.model.verification.Verification
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/** An object that represents a passkey associated with a user. */
@Serializable
data class Passkey(
  /** The unique identifier of the passkey. */
  val id: String,

  /** The passkey's name. */
  val name: String,

  /** The verification details for the passkey. */
  val verification: Verification? = null,

  /** The date when the passkey was created. */
  val createdAt: Instant,

  /** The date when the passkey was last updated. */
  val updatedAt: Instant,

  /** The date when the passkey was last used. */
  val lastUsedAt: Instant? = null,
)
