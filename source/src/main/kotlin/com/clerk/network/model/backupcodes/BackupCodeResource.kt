package com.clerk.network.model.backupcodes

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a backup code resource for two-factor authentication recovery.
 *
 * Backup codes are single-use recovery codes that can be used to authenticate when the user's
 * primary two-factor authentication method (like TOTP or SMS) is unavailable. These codes
 * should be stored securely by the user and can only be used once each.
 *
 * @property objectType The type of object, typically "backup_code"
 * @property id The unique identifier for this backup code resource
 * @property codes List of backup codes that can be used for recovery authentication
 * @property createdAt Timestamp when the backup codes were created (Unix timestamp in milliseconds)
 * @property updatedAt Timestamp when the backup codes were last updated (Unix timestamp in milliseconds)
 */
@Serializable
data class BackupCodeResource(
  /** The type of object, typically "backup_code" */
  @SerialName("object") val objectType: String,
  
  /** The unique identifier for this backup code resource */
  val id: String,
  
  /** List of backup codes that can be used for recovery authentication */
  val codes: List<String>,
  
  /** Timestamp when the backup codes were created (Unix timestamp in milliseconds) */
  @SerialName("created_at") val createdAt: Long,
  
  /** Timestamp when the backup codes were last updated (Unix timestamp in milliseconds) */
  @SerialName("updated_at") val updatedAt: Long,
)
