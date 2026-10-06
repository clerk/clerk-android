package com.clerk.api

import com.clerk.sdk.BuildConfig

/** Consolidated constants used throughout the Clerk SDK */
object Constants {

  /**
   * Authentication and verification strategy strings.
   *
   * Superseded by the typed [com.clerk.api.auth.types.Strategy]; use `Strategy.X.value` where a raw
   * string is required.
   */
  object Strategy {
    @Deprecated(
      "Use the typed com.clerk.api.auth.types.Strategy.PhoneCode instead, or " +
        "Strategy.PhoneCode.value for the raw string. Note that Strategy.PhoneCode.value " +
        "is not a compile-time constant, so it cannot be used in const contexts."
    )
    const val PHONE_CODE = "phone_code"
    @Deprecated(
      "Use the typed com.clerk.api.auth.types.Strategy.EmailCode instead, or " +
        "Strategy.EmailCode.value for the raw string. Note that Strategy.EmailCode.value " +
        "is not a compile-time constant, so it cannot be used in const contexts."
    )
    const val EMAIL_CODE = "email_code"
    @Deprecated(
      "Use the typed com.clerk.api.auth.types.Strategy.EmailLink instead, or " +
        "Strategy.EmailLink.value for the raw string. Note that Strategy.EmailLink.value " +
        "is not a compile-time constant, so it cannot be used in const contexts."
    )
    const val EMAIL_LINK = "email_link"
    @Deprecated(
      "Use the typed com.clerk.api.auth.types.Strategy.Totp instead, or " +
        "Strategy.Totp.value for the raw string. Note that Strategy.Totp.value " +
        "is not a compile-time constant, so it cannot be used in const contexts."
    )
    const val TOTP = "totp"
    @Deprecated(
      "Use the typed com.clerk.api.auth.types.Strategy.BackupCode instead, or " +
        "Strategy.BackupCode.value for the raw string. Note that Strategy.BackupCode.value " +
        "is not a compile-time constant, so it cannot be used in const contexts."
    )
    const val BACKUP_CODE = "backup_code"
    @Deprecated(
      "Use the typed com.clerk.api.auth.types.Strategy.Password instead, or " +
        "Strategy.Password.value for the raw string. Note that Strategy.Password.value " +
        "is not a compile-time constant, so it cannot be used in const contexts."
    )
    const val PASSWORD = "password"
    @Deprecated(
      "Use the typed com.clerk.api.auth.types.Strategy.Passkey instead, or " +
        "Strategy.Passkey.value for the raw string. Note that Strategy.Passkey.value " +
        "is not a compile-time constant, so it cannot be used in const contexts."
    )
    const val PASSKEY = "passkey"
    @Deprecated(
      "Use the typed com.clerk.api.auth.types.Strategy.ResetPasswordEmailCode instead, or " +
        "Strategy.ResetPasswordEmailCode.value for the raw string. Note that " +
        "Strategy.ResetPasswordEmailCode.value " +
        "is not a compile-time constant, so it cannot be used in const contexts."
    )
    const val RESET_PASSWORD_EMAIL_CODE = "reset_password_email_code"
    @Deprecated(
      "Use the typed com.clerk.api.auth.types.Strategy.ResetPasswordPhoneCode instead, or " +
        "Strategy.ResetPasswordPhoneCode.value for the raw string. Note that " +
        "Strategy.ResetPasswordPhoneCode.value " +
        "is not a compile-time constant, so it cannot be used in const contexts."
    )
    const val RESET_PASSWORD_PHONE_CODE = "reset_password_phone_code"
    @Deprecated(
      "Use the typed com.clerk.api.auth.types.Strategy.Ticket instead, or " +
        "Strategy.Ticket.value for the raw string. Note that Strategy.Ticket.value " +
        "is not a compile-time constant, so it cannot be used in const contexts."
    )
    const val TICKET = "ticket"
    @Deprecated(
      "Use the typed com.clerk.api.auth.types.Strategy.Transfer instead, or " +
        "Strategy.Transfer.value for the raw string. Note that Strategy.Transfer.value " +
        "is not a compile-time constant, so it cannot be used in const contexts."
    )
    const val TRANSFER = "transfer"
    @Deprecated(
      "Use the typed com.clerk.api.auth.types.Strategy.EnterpriseSso instead, or " +
        "Strategy.EnterpriseSso.value for the raw string. Note that Strategy.EnterpriseSso.value " +
        "is not a compile-time constant, so it cannot be used in const contexts."
    )
    const val ENTERPRISE_SSO = "enterprise_sso"
    @Deprecated(
      "Use the typed com.clerk.api.auth.types.Strategy.TrustedDevice instead, or " +
        "Strategy.TrustedDevice.value for the raw string. Note that Strategy.TrustedDevice.value " +
        "is not a compile-time constant, so it cannot be used in const contexts."
    )
    const val TRUSTED_DEVICE = "trusted_device"
  }

  /** HTTP and API related constants */
  object Http {
    const val NO_CONTENT = 204
    const val RESET_CONTENT = 205
    const val CURRENT_API_VERSION = "2026-08-20"
    const val CURRENT_SDK_VERSION = BuildConfig.SDK_VERSION
    const val IS_MOBILE_HEADER_VALUE = "1"
    const val AUTHORIZATION_HEADER = "Authorization"
    const val IS_NATIVE_QUERY_PARAM = "_is_native"
  }

  /** Configuration and timing constants */
  object Config {
    const val REFRESH_TOKEN_INTERVAL = 5
    const val API_TIMEOUT_SECONDS = 30L
    const val TIMEOUT_MULTIPLIER = 1000
    const val BACKOFF_BASE_DELAY_SECONDS = 5L
    const val MAX_INITIALIZATION_RETRIES = 3
    const val EXPONENTIAL_BACKOFF_SHIFT = 1
    const val DEFAULT_EXPIRATION_BUFFER = 1000L
    const val COMPRESSION_PERCENTAGE = 75
  }

  /** URL and key prefixes */
  object Prefixes {
    const val URL_SSL_PREFIX = "https://"
    const val TOKEN_PREFIX_LIVE = "pk_live_"
    const val TOKEN_PREFIX_TEST = "pk_test_"
  }

  /** Storage and preference keys */
  object Storage {
    const val CLERK_PREFERENCES_FILE_NAME = "clerk_preferences"
    const val KEY_AUTHORIZATION_STARTED = "authStarted"
  }

  /** Device attestation constants */
  object Attestation {
    const val HASH_CONSTANT = 0xff
    const val PREPARATION_TIMEOUT_MS = 30_000L
    const val ATTESTATION_TIMEOUT_MS = 15_000L
    const val HASH_CACHE_MAX_SIZE = 100
    const val SHA256_HEX_LENGTH = 64
  }

  /** Passkey related constants */
  object Passkey {
    @Deprecated(
      "Use the typed com.clerk.api.auth.types.Strategy.Passkey instead, or " +
        "Strategy.Passkey.value for the raw string. Note that Strategy.Passkey.value " +
        "is not a compile-time constant, so it cannot be used in const contexts."
    )
    const val PASSKEY_STRATEGY_VALUE = "passkey"
  }

  /** Parameter and field names */
  object Fields {
    const val STRATEGY = "strategy"
  }

  /** Test constants (only available in test builds) */
  object Test {
    const val CONCURRENCY_TEST_THREAD_COUNT = 10
    const val EXPECTED_STORAGE_LOAD_CALLS = 1
    const val EXPECTED_STORAGE_SAVE_CALLS = 1
    const val EXPECTED_SAVED_DEVICE_ID_COUNT = 1
  }
}
