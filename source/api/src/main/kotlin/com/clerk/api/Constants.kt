package com.clerk.api

import com.clerk.sdk.BuildConfig

/** Consolidated constants used throughout the Clerk SDK */
public object Constants {

  /** Authentication and verification strategies */
  public object Strategy {
    public const val PHONE_CODE: String = "phone_code"
    public const val EMAIL_CODE: String = "email_code"
    public const val EMAIL_LINK: String = "email_link"
    public const val TOTP: String = "totp"
    public const val BACKUP_CODE: String = "backup_code"
    public const val PASSWORD: String = "password"
    public const val PASSKEY: String = "passkey"
    public const val RESET_PASSWORD_EMAIL_CODE: String = "reset_password_email_code"
    public const val RESET_PASSWORD_PHONE_CODE: String = "reset_password_phone_code"
    public const val TICKET: String = "ticket"
    public const val TRANSFER: String = "transfer"
    public const val ENTERPRISE_SSO: String = "enterprise_sso"
    public const val TRUSTED_DEVICE: String = "trusted_device"
  }

  /** HTTP and API related constants */
  public object Http {
    public const val NO_CONTENT: Int = 204
    public const val RESET_CONTENT: Int = 205
    public const val CURRENT_API_VERSION: String = "2026-08-20"
    public const val CURRENT_SDK_VERSION: String = BuildConfig.SDK_VERSION
    public const val IS_MOBILE_HEADER_VALUE: String = "1"
    public const val AUTHORIZATION_HEADER: String = "Authorization"
    public const val IS_NATIVE_QUERY_PARAM: String = "_is_native"
  }

  /** Configuration and timing constants */
  public object Config {
    public const val REFRESH_TOKEN_INTERVAL: Int = 5
    public const val API_TIMEOUT_SECONDS: Long = 30L
    public const val TIMEOUT_MULTIPLIER: Int = 1000
    public const val BACKOFF_BASE_DELAY_SECONDS: Long = 5L
    public const val MAX_INITIALIZATION_RETRIES: Int = 3
    public const val EXPONENTIAL_BACKOFF_SHIFT: Int = 1
    public const val DEFAULT_EXPIRATION_BUFFER: Long = 1000L
    public const val COMPRESSION_PERCENTAGE: Int = 75
  }

  /** URL and key prefixes */
  public object Prefixes {
    public const val URL_SSL_PREFIX: String = "https://"
    public const val TOKEN_PREFIX_LIVE: String = "pk_live_"
    public const val TOKEN_PREFIX_TEST: String = "pk_test_"
  }

  /** Storage and preference keys */
  public object Storage {
    public const val CLERK_PREFERENCES_FILE_NAME: String = "clerk_preferences"
    public const val KEY_AUTHORIZATION_STARTED: String = "authStarted"
  }

  /** Device attestation constants */
  public object Attestation {
    public const val HASH_CONSTANT: Int = 0xff
    public const val PREPARATION_TIMEOUT_MS: Long = 30_000L
    public const val ATTESTATION_TIMEOUT_MS: Long = 15_000L
    public const val HASH_CACHE_MAX_SIZE: Int = 100
    public const val SHA256_HEX_LENGTH: Int = 64
  }

  /** Passkey related constants */
  public object Passkey {
    public const val PASSKEY_STRATEGY_VALUE: String = "passkey"
  }

  /** Parameter and field names */
  public object Fields {
    public const val STRATEGY: String = "strategy"
  }

  /** Test constants (only available in test builds) */
  public object Test {
    public const val CONCURRENCY_TEST_THREAD_COUNT: Int = 10
    public const val EXPECTED_STORAGE_LOAD_CALLS: Int = 1
    public const val EXPECTED_STORAGE_SAVE_CALLS: Int = 1
    public const val EXPECTED_SAVED_DEVICE_ID_COUNT: Int = 1
  }
}
