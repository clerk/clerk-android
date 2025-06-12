package com.clerk.network.model.account

import com.clerk.network.ClerkApi
import com.clerk.network.model.deleted.DeletedObject
import com.clerk.network.model.error.ClerkErrorResponse
import com.clerk.network.model.verification.Verification
import com.clerk.network.serialization.ClerkResult
import com.clerk.user.User
import kotlinx.serialization.SerialName
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
  @SerialName("identification_id") val identificationId: String,

  /** The provider name e.g. google */
  val provider: String,

  /** The unique ID of the user in the provider. */
  @SerialName("provider_user_id") val providerUserId: String,

  /** The provided email address of the user. */
  @SerialName("email_address") val emailAddress: String,

  /** The scopes that the user has granted access to. */
  @SerialName("approved_scopes") val approvedScopes: String,

  /** The user's first name. */
  @SerialName("first_name") val firstName: String? = null,

  /** The user's last name. */
  @SerialName("last_name") val lastName: String? = null,

  /** The user's image URL. */
  @SerialName("image_url") val imageUrl: String? = null,

  /** The user's username. */
  val username: String? = null,

  /**
   * Metadata that can be read from the Frontend API and Backend API and can be set only from the
   * Backend API.
   */
  @SerialName("public_metadata") val publicMetadata: JsonObject? = null,

  /**
   * A descriptive label to differentiate multiple external accounts of the same user for the same
   * provider.
   */
  val label: String? = null,

  /** An object holding information on the verification of this external account. */
  val verification: Verification? = null,
)

/**
 * Reauthorizes this external account by refreshing its verification status.
 *
 * This method is useful when an external account's verification has expired or become invalid. It
 * initiates a new verification process by redirecting to the external provider's authorization
 * endpoint.
 *
 * @return A [ClerkResult] containing the updated [Verification] object on success, or a
 *   [ClerkErrorResponse] on failure
 * @throws IllegalArgumentException if the external verification redirect URL is null
 */
suspend fun ExternalAccount.reauthorize(): ClerkResult<Verification, ClerkErrorResponse> {
  val redirectUrl =
    requireNotNull(this.verification?.externalVerificationRedirectUrl) {
      "External verification redirect URL is null"
    }
  return ClerkApi.user.reauthorizeExternalAccount(
    externalAccountId = this.id,
    redirectUrl = redirectUrl,
  )
}

/**
 * Deletes this external account from the user's profile.
 *
 * This operation is irreversible. Once deleted, the external account will no longer be associated
 * with the user and cannot be used for authentication. The user will need to reconnect the external
 * provider if they want to use it for sign-in again.
 *
 * @return A [ClerkResult] containing a [DeletedObject] on success, or a [ClerkErrorResponse] on
 *   failure
 */
suspend fun ExternalAccount.delete(): ClerkResult<DeletedObject, ClerkErrorResponse> {
  return ClerkApi.user.deleteExternalAccount(externalAccountId = this.id)
}

/**
 * Revokes the access tokens associated with this external account.
 *
 * This method invalidates the OAuth tokens that Clerk has stored for this external account. This is
 * useful for security purposes when you want to ensure that stored tokens can no longer be used to
 * access the external provider's API on behalf of the user.
 *
 * Note: This does not delete the external account itself, only revokes its tokens. The account
 * remains connected but may need reauthorization for future API access.
 *
 * @return A [ClerkResult] containing the updated [User] object on success, or a
 *   [ClerkErrorResponse] on failure
 */
suspend fun ExternalAccount.revokeTokens(): ClerkResult<User, ClerkErrorResponse> {
  return ClerkApi.user.revokeExternalAccountTokens(externalAccountId = this.id)
}
