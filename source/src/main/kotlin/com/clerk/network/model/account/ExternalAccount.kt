package com.clerk.network.model.account

import com.clerk.network.model.verification.Verification
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * The `ExternalAccount` object is a model around an identification obtained by an external provider
 * (e.g. a social provider such as Google).
 *
 * External account must be verified, so that you can make sure they can be assigned to their
 * rightful owners. The `ExternalAccount` object holds all necessary state around the verification
 * process.
 */
@Serializable
data class ExternalAccount(
  /** The unique identifier for this external account. */
  val id: String,

  /** The identification with which this external account is associated. */
  val identificationId: String,

  /** The provider name e.g. google */
  val provider: String,

  /** The unique ID of the user in the provider. */
  val providerUserId: String,

  /** The provided email address of the user. */
  val emailAddress: String,

  /** The scopes that the user has granted access to. */
  val approvedScopes: String,

  /** The user's first name. */
  val firstName: String? = null,

  /** The user's last name. */
  val lastName: String? = null,

  /** The user's image URL. */
  val imageUrl: String? = null,

  /** The user's username. */
  val username: String? = null,

  /**
   * Metadata that can be read from the Frontend API and Backend API and can be set only from the
   * Backend API.
   */
  val publicMetadata: JsonObject,

  /**
   * A descriptive label to differentiate multiple external accounts of the same user for the same
   * provider.
   */
  val label: String? = null,

  /** An object holding information on the verification of this external account. */
  val verification: Verification? = null,
)
