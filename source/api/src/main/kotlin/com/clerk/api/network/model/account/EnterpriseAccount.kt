package com.clerk.api.network.model.account

import com.clerk.api.network.model.verification.Verification
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * A model representing an enterprise account.
 *
 * `EnterpriseAccount` encapsulates the details of a user's enterprise account.
 */
@Serializable
data class EnterpriseAccount(
  /** The unique identifier for the enterprise account. */
  val id: String,

  /** The type of object, typically a string identifier indicating the object type. */
  @SerialName("object") val objectType: String,

  /** The authentication protocol used (e.g., SAML, OpenID). */
  val protocol: String,

  /** The name of the provider (e.g., Okta, Google). */
  val provider: String,

  /** A flag indicating whether the enterprise account is active. */
  val active: Boolean,

  /** The email address associated with the enterprise account. */
  val emailAddress: String,

  /** The first name of the account holder, if available. */
  val firstName: String? = null,

  /** The last name of the account holder, if available. */
  val lastName: String? = null,

  /** The unique user identifier assigned by the provider, if available. */
  val providerUserId: String? = null,

  /** Public metadata associated with the enterprise account. */
  val publicMetadata: JsonObject,

  /** Verification information for the enterprise account, if available. */
  val verification: Verification? = null,

  /** Details about the enterprise connection associated with this account. */
  val enterpriseConnection: EnterpriseConnection,
) {
  /**
   * A model representing the connection details for an enterprise account.
   *
   * `EnterpriseConnection` contains the configuration and metadata for the connection between the
   * enterprise account and the identity provider.
   */
  @Serializable
  data class EnterpriseConnection(
    /** The unique identifier for the enterprise connection. */
    val id: String,

    /** The authentication protocol used (e.g., SAML, OpenID). */
    val protocol: String,

    /** The name of the provider (e.g., Okta, Google Workspace). */
    val provider: String,

    /** The display name of the enterprise connection. */
    val name: String,

    /** The public URL of the provider's logo. */
    val logoPublicUrl: String,

    /** The domain associated with the enterprise connection (e.g., example.com). */
    val domain: String,

    /** A flag indicating whether the enterprise connection is active. */
    val active: Boolean,

    /** A flag indicating whether user attributes are synchronized with the provider. */
    val syncUserAttributes: Boolean,

    /**
     * A flag indicating whether additional user identifications are disabled for this connection.
     */
    val disableAdditionalIdentifications: Boolean,

    /** The date and time when the enterprise connection was created. */
    val createdAt: Long,

    /** The date and time when the enterprise connection was last updated. */
    val updatedAt: Long,

    /** A flag indicating whether subdomains are allowed for the enterprise connection. */
    val allowSubdomains: Boolean,

    /** A flag indicating whether IDP-initiated flows are allowed. */
    val allowIdpInitiated: Boolean,
  )
}
