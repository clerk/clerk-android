package com.clerk.network.model.verification

import com.clerk.model.error.Error
import kotlinx.serialization.Serializable

/** The state of the verification process of a sign-in or sign-up attempt. */
@Serializable
data class Verification(
  /** The state of the verification. */
  val status: Status? = null,
  /** The strategy pertaining to the parent sign-up or sign-in attempt. */
  val strategy: String? = null,
  /** The number of attempts related to the verification. */
  val attempts: Int? = null,
  /** The time the verification will expire at. */
  val expireAt: Long? = null,
  /** The last error the verification attempt ran into. */
  val error: Error? = null,
  /** The redirect URL for an external verification. */
  val externalVerificationRedirectUrl: String? = null,
  /** The nonce pertaining to the verification. */
  val nonce: String? = null,
) {
  /** The state of the verification. */
  @Serializable
  enum class Status {
    UNVERIFIED,
    VERIFIED,
    TRANSFERABLE,
    FAILED,
    EXPIRED,
    UNKNOWN,
  }
}
