package com.clerk.model.passkey

import com.clerk.model.verification.Verification
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
  val createdAt: Long,

  /** The date when the passkey was last updated. */
  val updatedAt: Long,

  /** The date when the passkey was last used. */
  val lastUsedAt: Long? = null,
)
