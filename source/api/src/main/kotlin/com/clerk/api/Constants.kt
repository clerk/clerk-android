package com.clerk.api

import com.clerk.sdk.BuildConfig

/** Consolidated constants used throughout the Clerk SDK */
object Constants {

  /** Authentication and verification strategies */
  object Strategy {
    const val PHONE_CODE = "phone_code"
    const val EMAIL_CODE = "email_code"
    const val TOTP = "totp"
    const val BACKUP_CODE = "backup_code"
    const val PASSWORD = "password"
    const val PASSKEY = "passkey"
    const val RESET_PASSWORD_EMAIL_CODE = "reset_password_email_code"
    const val RESET_PASSWORD_PHONE_CODE = "reset_password_phone_code"
    const val TICKET = "ticket"
    const val TRANSFER = "transfer"
    const val ENTERPRISE_SSO = "enterprise_sso"
  }

  /** HTTP and API related constants */
  object Http {
    const val NO_CONTENT = 204
    const val RESET_CONTENT = 205
    const val CURRENT_API_VERSION = "2025-11-10"
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
    const val MAX_ATTESTATION_RETRIES = 3
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
    const val PREPARATION_TIMEOUT_MS = 30_000L // 30 seconds
    const val ATTESTATION_TIMEOUT_MS = 15_000L // 15 seconds
    const val HASH_CACHE_MAX_SIZE = 100
    const val SHA256_HEX_LENGTH = 64
  }

  /** Passkey related constants */
  object Passkey {
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
