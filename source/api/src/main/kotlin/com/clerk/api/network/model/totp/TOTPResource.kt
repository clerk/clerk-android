package com.clerk.api.network.model.totp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a TOTP (Time-based One-Time Password) resource for two-factor authentication.
 *
 * TOTP is commonly used for two-factor authentication with authenticator apps like Google
 * Authenticator, Authy, or 1Password. This resource contains the secret key and configuration
 * needed to set up TOTP authentication.
 *
 * @property id The unique identifier for this TOTP resource
 * @property secret The base32-encoded secret key used for generating TOTP codes
 * @property uri The otpauth:// URI that can be used to generate QR codes for authenticator apps
 * @property verified Whether the TOTP setup has been verified by the user
 * @property createdAt Timestamp when the TOTP resource was created (Unix timestamp in milliseconds)
 * @property updatedAt Timestamp when the TOTP resource was last updated (Unix timestamp in
 *   milliseconds)
 */
@Serializable
data class TOTPResource(
  /** The unique identifier for this TOTP resource */
  val id: String,

  /** The base32-encoded secret key used for generating TOTP codes */
  val secret: String? = null,

  /** The otpauth:// URI that can be used to generate QR codes for authenticator apps */
  val uri: String? = null,

  /** Whether the TOTP setup has been verified by the user */
  val verified: Boolean,
  @SerialName("backup_codes") val backupCodes: List<String>? = null,

  /** Timestamp when the TOTP resource was created (Unix timestamp in milliseconds) */
  @SerialName("created_at") val createdAt: Long,

  /** Timestamp when the TOTP resource was last updated (Unix timestamp in milliseconds) */
  @SerialName("updated_at") val updatedAt: Long,
)
