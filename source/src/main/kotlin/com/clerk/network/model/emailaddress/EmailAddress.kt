package com.clerk.network.model.emailaddress

import com.clerk.model.verification.Verification
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** The EmailAddress object represents an email address associated with a user. */
@Serializable
data class EmailAddress(
  /** The unique identifier for the email address. */
  val id: String,

  /** The email address value. */
  @SerialName("email_address") val emailAddress: String,

  /** The verification status of the email address. */
  val verification: Verification? = null,

  /** A list of linked accounts or identifiers associated with this email address. */
  @SerialName("linked_to") val linkedTo: List<JsonElement>? = null,
)
