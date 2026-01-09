package com.clerk.api.auth.types

/**
 * Verification type enum for SignUp.
 *
 * Since multiple verifications can be active during sign-up (e.g., both email and phone), this enum
 * is used to specify which verification type to operate on.
 */
enum class VerificationType {
  /** Email address verification. */
  EMAIL,

  /** Phone number verification. */
  PHONE,
}
