package com.clerk.api.auth.types

/**
 * MFA (Multi-Factor Authentication) type enum.
 *
 * Represents all supported second factor verification methods.
 */
enum class MfaType {
  /** SMS code sent to the user's phone. */
  PHONE_CODE,

  /** Email code sent to the user's email address. */
  EMAIL_CODE,

  /** Time-based One-Time Password from an authenticator app. */
  TOTP,

  /** Single-use backup code. */
  BACKUP_CODE,
}
